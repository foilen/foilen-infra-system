/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.model;

public enum DockerStep {

    BUILD_IMAGE, //
    RESTART_CONTAINER, //
    COPY_AND_EXECUTE_IN_RUNNING_CONTAINER, //
    COMPLETED, //

}
