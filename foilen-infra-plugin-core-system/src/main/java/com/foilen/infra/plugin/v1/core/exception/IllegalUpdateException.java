/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.exception;

public class IllegalUpdateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IllegalUpdateException(String message) {
        super(message);
    }

}
