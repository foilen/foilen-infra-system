/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.common.context;

import org.springframework.beans.factory.annotation.Autowired;

import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalIPResourceService;

public class InternalServicesContextBean extends InternalServicesContext {

    @Autowired
    private InternalChangeService internalChangeService;
    @Autowired
    private InternalIPResourceService internalIPResourceService;

    public InternalServicesContextBean() {
        super(null, null);
    }

    @Override
    public InternalChangeService getInternalChangeService() {
        return internalChangeService;
    }

    @Override
    public InternalIPResourceService getInternalIPResourceService() {
        return internalIPResourceService;
    }

}
