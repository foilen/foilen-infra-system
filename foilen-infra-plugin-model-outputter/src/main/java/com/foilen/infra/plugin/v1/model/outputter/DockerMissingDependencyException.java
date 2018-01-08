/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.outputter;

public class DockerMissingDependencyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DockerMissingDependencyException(String message) {
        super(message);
    }

}
