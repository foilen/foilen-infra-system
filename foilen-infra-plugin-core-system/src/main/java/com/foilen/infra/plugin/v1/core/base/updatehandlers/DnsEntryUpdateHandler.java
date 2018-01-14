/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.updatehandlers;

import com.foilen.infra.plugin.v1.core.base.resources.DnsEntry;
import com.foilen.infra.plugin.v1.core.base.resources.Domain;
import com.foilen.infra.plugin.v1.core.common.DomainHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractCommonMethodUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.eventhandler.CommonMethodUpdateEventHandlerContext;

public class DnsEntryUpdateHandler extends AbstractCommonMethodUpdateEventHandler<DnsEntry> {

    @Override
    protected void commonHandlerExecute(CommonServicesContext services, ChangesContext changes, CommonMethodUpdateEventHandlerContext<DnsEntry> context) {

        DnsEntry resource = context.getResource();

        context.getManagedResourceTypes().add(Domain.class);

        context.getManagedResources().add(new Domain(resource.getName(), DomainHelper.reverseDomainName(resource.getName())));

    }

    @Override
    public Class<DnsEntry> supportedClass() {
        return DnsEntry.class;
    }

}
