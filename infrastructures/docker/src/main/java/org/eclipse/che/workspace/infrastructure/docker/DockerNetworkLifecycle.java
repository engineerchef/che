package org.eclipse.che.workspace.infrastructure.docker;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.exception.NetworkNotFoundException;
import org.eclipse.che.plugin.docker.client.json.network.NewNetwork;
import org.eclipse.che.plugin.docker.client.params.RemoveNetworkParams;
import org.eclipse.che.plugin.docker.client.params.network.CreateNetworkParams;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

/**
 * @author Alexander Garagatyi
 */
public class DockerNetworkLifecycle {

    private final DockerConnector docker;
    private final String          networkDriver;

    @Inject
    public DockerNetworkLifecycle(DockerConnector docker,
                                  @Nullable @Named("che.docker.network_driver") String networkDriver) {
        this.docker = docker;
        this.networkDriver = networkDriver;
    }

    public void createNetwork(String networkName) throws ServerException {
        try {
            docker.createNetwork(CreateNetworkParams.create(new NewNetwork().withName(networkName)
                                                                            .withDriver(networkDriver)
                                                                            .withCheckDuplicate(true)));
        } catch (IOException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    public void destroyNetwork(String networkName) throws ServerException {
        try {
            docker.removeNetwork(RemoveNetworkParams.create(networkName));
        } catch (NetworkNotFoundException ignore) {
        } catch (IOException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }
}