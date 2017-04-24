/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.languageserver.ide;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.languageserver.shared.model.LanguageDescription;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.component.WsAgentComponent;
import org.eclipse.che.ide.api.editor.EditorRegistry;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.editor.orion.client.OrionContentTypeRegistrant;
import org.eclipse.che.ide.editor.orion.client.OrionHoverRegistrant;
import org.eclipse.che.ide.editor.orion.client.OrionOccurrencesRegistrant;
import org.eclipse.che.ide.editor.orion.client.jso.OrionContentTypeOverlay;
import org.eclipse.che.ide.editor.orion.client.jso.OrionHighlightingConfigurationOverlay;
import org.eclipse.che.plugin.languageserver.ide.editor.LanguageServerEditorProvider;
import org.eclipse.che.plugin.languageserver.ide.highlighting.OccurrencesProvider;
import org.eclipse.che.plugin.languageserver.ide.hover.HoverProvider;
import org.eclipse.che.plugin.languageserver.ide.service.LanguageServerRegistryServiceClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class LanguageServerFileTypeRegister implements WsAgentComponent {


    private final LanguageServerRegistryServiceClient serverLanguageRegistry;
    private final FileTypeRegistry                    fileTypeRegistry;
    private final LanguageServerResources             resources;
    private final EditorRegistry                      editorRegistry;
    private final OrionContentTypeRegistrant          contentTypeRegistrant;
    private final OrionHoverRegistrant                orionHoverRegistrant;
    private final OrionOccurrencesRegistrant          orionOccurrencesRegistrant;
    private final LanguageServerEditorProvider        editorProvider;
    private final HoverProvider                       hoverProvider;
    private final OccurrencesProvider                 occurrencesProvider;

    private final Map<String, String> ext2langId = new HashMap<>();

    @Inject
    public LanguageServerFileTypeRegister(LanguageServerRegistryServiceClient serverLanguageRegistry,
                                          FileTypeRegistry fileTypeRegistry,
                                          LanguageServerResources resources,
                                          EditorRegistry editorRegistry,
                                          OrionContentTypeRegistrant contentTypeRegistrant,
                                          OrionHoverRegistrant orionHoverRegistrant,
                                          OrionOccurrencesRegistrant orionOccurrencesRegistrant,
                                          LanguageServerEditorProvider editorProvider,
                                          HoverProvider hoverProvider,
                                          OccurrencesProvider occurrencesProvider) {
        this.serverLanguageRegistry = serverLanguageRegistry;
        this.fileTypeRegistry = fileTypeRegistry;
        this.resources = resources;
        this.editorRegistry = editorRegistry;
        this.contentTypeRegistrant = contentTypeRegistrant;
        this.orionHoverRegistrant = orionHoverRegistrant;
        this.orionOccurrencesRegistrant = orionOccurrencesRegistrant;
        this.editorProvider = editorProvider;
        this.hoverProvider = hoverProvider;
        this.occurrencesProvider = occurrencesProvider;
    }

    @Override
    public void start(final Callback<WsAgentComponent, Exception> callback) {
        Promise<List<LanguageDescription>> registeredLanguages = serverLanguageRegistry.getSupportedLanguages();
        registeredLanguages.then(new Operation<List<LanguageDescription>>() {
            @Override
            public void apply(List<LanguageDescription> langs) throws OperationException {
                if (!langs.isEmpty()) {
                    JsArrayString contentTypes = JsArrayString.createArray().cast();
                    for (LanguageDescription lang : langs) {
                        String primaryExtension = lang.getFileExtensions().get(0);
                        for (String ext : lang.getFileExtensions()) {
                            final FileType fileType = new FileType(resources.file(), ext);
                            fileTypeRegistry.registerFileType(fileType);
                            editorRegistry.registerDefaultEditor(fileType, editorProvider);
                            ext2langId.put(ext, lang.getLanguageId());
                        }
                        List<String> mimeTypes = lang.getMimeTypes();
                        if (mimeTypes.isEmpty()) {
                            mimeTypes = newArrayList("text/x-" + lang.getLanguageId());
                        }
                        for (String contentTypeId : mimeTypes) {
                            contentTypes.push(contentTypeId);
                            OrionContentTypeOverlay contentType = OrionContentTypeOverlay.create();
                            contentType.setId(contentTypeId);
                            contentType.setName(lang.getLanguageId());
                            contentType.setExtension(primaryExtension);
                            contentType.setExtends("text/plain");

                            // highlighting
                            OrionHighlightingConfigurationOverlay config = OrionHighlightingConfigurationOverlay
                                    .create();
                            config.setId(lang.getLanguageId() + ".highlighting");
                            config.setContentTypes(contentTypeId);
                            config.setPatterns(lang.getHighlightingConfiguration());
                            Logger logger = Logger.getLogger(LanguageServerFileTypeRegister.class.getName());
                            contentTypeRegistrant.registerFileType(contentType, config);
                        }
                    }
                    orionHoverRegistrant.registerHover(contentTypes, hoverProvider);
                    orionOccurrencesRegistrant.registerOccurrencesHandler(contentTypes, occurrencesProvider);
                }
                callback.onSuccess(LanguageServerFileTypeRegister.this);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                callback.onFailure(new Exception(arg.getMessage(), arg.getCause()));
            }
        });
    }

    boolean hasLSForExtension(String ext) {
        return ext2langId.containsKey(ext);
    }

    String findLangId(String ext) {
        return ext2langId.get(ext);
    }
}
