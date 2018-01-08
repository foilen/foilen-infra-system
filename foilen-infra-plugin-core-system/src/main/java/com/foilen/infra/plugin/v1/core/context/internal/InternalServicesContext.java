/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.context.internal;

import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalIPResourceService;

/**
 * All the internal services.
 */
public class InternalServicesContext {

    private InternalIPResourceService internalIPResourceService;
    private InternalChangeService internalChangeService;

    public InternalServicesContext(InternalIPResourceService internalIPResourceService, InternalChangeService internalChangeService) {
        this.internalIPResourceService = internalIPResourceService;
        this.internalChangeService = internalChangeService;
    }

    public InternalChangeService getInternalChangeService() {
        return internalChangeService;
    }

    public InternalIPResourceService getInternalIPResourceService() {
        return internalIPResourceService;
    }

}
