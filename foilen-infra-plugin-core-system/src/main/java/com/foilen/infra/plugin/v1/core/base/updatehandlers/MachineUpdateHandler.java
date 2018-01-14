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
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.model.DnsEntryType;
import com.foilen.infra.plugin.v1.core.common.DomainHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractCommonMethodUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.eventhandler.CommonMethodUpdateEventHandlerContext;
import com.foilen.infra.plugin.v1.core.exception.IllegalUpdateException;
import com.foilen.smalltools.tools.StringTools;

public class MachineUpdateHandler extends AbstractCommonMethodUpdateEventHandler<Machine> {

    @Override
    protected void commonHandlerExecute(CommonServicesContext services, ChangesContext changes, CommonMethodUpdateEventHandlerContext<Machine> context) {

        Machine resource = context.getResource();

        if (context.getOldResource() != null && !StringTools.safeEquals(context.getOldResource().getName(), resource.getName())) {
            throw new IllegalUpdateException("You cannot change a Machine's name");
        }

        context.getManagedResourceTypes().add(Domain.class);
        context.getManagedResourceTypes().add(DnsEntry.class);

        if (resource.getPublicIp() == null) {
            // Use a Domain
            context.getManagedResources().add(new Domain(resource.getName(), DomainHelper.reverseDomainName(resource.getName())));
        } else {
            // Use a DnsEntry
            context.getManagedResources().add(new DnsEntry(resource.getName(), DnsEntryType.A, resource.getPublicIp()));
        }
    }

    @Override
    public Class<Machine> supportedClass() {
        return Machine.class;
    }

}
