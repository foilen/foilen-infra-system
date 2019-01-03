/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.mvc;

public class UiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private Object[] params = {};

    public UiException(String messageCode) {
        super(messageCode);
    }

    public UiException(String messageCode, Object... params) {
        super(messageCode);
        this.params = params;
    }

    public Object[] getParams() {
        return params;
    }

}
