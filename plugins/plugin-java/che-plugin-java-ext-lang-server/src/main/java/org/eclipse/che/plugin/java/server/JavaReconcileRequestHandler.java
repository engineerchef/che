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
package org.eclipse.che.plugin.java.server;

import com.google.inject.Inject;

import org.eclipse.che.api.core.jsonrpc.JsonRpcException;
import org.eclipse.che.api.core.jsonrpc.RequestHandlerConfigurator;
import org.eclipse.che.ide.ext.java.shared.dto.JavaClassInfo;
import org.eclipse.che.ide.ext.java.shared.dto.ReconcileResult;
import org.eclipse.che.jdt.javaeditor.JavaReconciler;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaModelManager;

/**
 * @author Roman Nikitenko
 */
public class JavaReconcileRequestHandler {
    private static final String    INCOMING_METHOD = "request:java-reconcile";
    private static final JavaModel JAVA_MODEL      = JavaModelManager.getJavaModelManager().getJavaModel();

    @Inject
    private JavaReconciler reconciler;

    @Inject
    public void configureHandler(RequestHandlerConfigurator configurator) {
        configurator.newConfiguration()
                    .methodName(INCOMING_METHOD)
                    .paramsAsDto(JavaClassInfo.class)
                    .resultAsDto(ReconcileResult.class)
                    .withFunction(this::getReconcileOperation);
    }

    private ReconcileResult getReconcileOperation(String endpointId, JavaClassInfo javaClassInfo) {
        try {
            IJavaProject javaProject = JAVA_MODEL.getJavaProject(javaClassInfo.getProjectPath());
            return reconciler.reconcile(javaProject, javaClassInfo.getFQN());
        } catch (JavaModelException e) {
            throw new JsonRpcException(500, e.getLocalizedMessage());
        }
    }
}
