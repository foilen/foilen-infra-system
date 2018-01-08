/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.callback;

import com.foilen.infra.plugin.system.utils.model.DockerStateIds;

public interface DockerContainerManagementCallback {

    /**
     * Tell if should proceed.
     *
     * @param containerName
     *            the name of the container
     * @param dockerStateIds
     *            the ids of the container
     * @return true to proceed or false to skip
     */
    boolean proceedWithTransformedContainer(String containerName, DockerStateIds dockerStateIds);

}
