/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

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

    private InternalIPResourceService internalFCResourceService;
    private InternalChangeService internalChangeService;

    public InternalServicesContext(InternalIPResourceService internalFCResourceService, InternalChangeService internalChangeService) {
        this.internalFCResourceService = internalFCResourceService;
        this.internalChangeService = internalChangeService;
    }

    public InternalChangeService getInternalChangeService() {
        return internalChangeService;
    }

    public InternalIPResourceService getInternalIPResourceService() {
        return internalFCResourceService;
    }

}
