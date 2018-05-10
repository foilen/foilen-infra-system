/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.callback;

import com.foilen.infra.plugin.system.utils.model.DockerStateIds;

public class NoOpDockerContainerManagementCallback implements DockerContainerManagementCallback {

    @Override
    public boolean proceedWithTransformedContainer(String containerName, DockerStateIds dockerStateIds) {
        return true;
    }

}
