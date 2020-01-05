/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.service;

import org.springframework.stereotype.Component;

import com.foilen.smalltools.tools.AbstractBasics;

@Component
public class EntitlementServiceImpl extends AbstractBasics implements EntitlementService {

    @Override
    public void canDeleteResourcesOrFailUi(String username) {
    }

    @Override
    public void canUpdateResourcesOrFailUi(String username) {
    }

    @Override
    public void isAdminOrFailUi(String username) {
    }

}
