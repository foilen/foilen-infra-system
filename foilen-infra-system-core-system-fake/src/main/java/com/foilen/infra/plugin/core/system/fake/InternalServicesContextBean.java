/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.foilen.infra.plugin.core.system.fake.service.FakeSystemServicesImpl;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalIPResourceService;

@Component
public class InternalServicesContextBean extends InternalServicesContext {

    @Autowired
    private FakeSystemServicesImpl fakeSystemServicesImpl;

    public InternalServicesContextBean() {
        super(null, null);
    }

    @Override
    public InternalChangeService getInternalChangeService() {
        return fakeSystemServicesImpl;
    }

    @Override
    public InternalIPResourceService getInternalIPResourceService() {
        return fakeSystemServicesImpl;
    }

}
