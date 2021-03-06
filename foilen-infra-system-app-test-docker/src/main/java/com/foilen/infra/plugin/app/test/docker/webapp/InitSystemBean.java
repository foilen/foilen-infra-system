/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.app.test.docker.webapp;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.foilen.infra.plugin.v1.core.common.InfraPluginCommonInit;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;

@Component
public class InitSystemBean {

    @Autowired
    private CommonServicesContext commonServicesContext;

    @Autowired
    private InternalServicesContext internalServicesContext;

    @PostConstruct
    public void init() {
        InfraPluginCommonInit.init(commonServicesContext, internalServicesContext);
    }

}
