/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.exception;

public class ResourcePrimaryKeyCannotBeNullException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourcePrimaryKeyCannotBeNullException(String nullPropertyName) {
        super("A resource cannot have a null part for a primary key. Null property name: " + nullPropertyName);
    }

}
