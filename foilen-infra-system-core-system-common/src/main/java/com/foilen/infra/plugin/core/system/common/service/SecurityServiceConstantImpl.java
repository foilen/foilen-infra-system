/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.common.service;

import com.foilen.infra.plugin.v1.core.service.SecurityService;

/**
 * Always uses the same value. THIS IS INSECURE AND ONLY FOR TESTS.
 */
public class SecurityServiceConstantImpl implements SecurityService {

    @Override
    public String getCsrfParameterName() {
        return "_csrf";
    }

    @Override
    public String getCsrfValue(Object request) {
        return "UNIQUE";
    }

}
