/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.updatehandlers;

import java.util.List;

import com.foilen.infra.plugin.v1.core.base.resources.DnsEntry;
import com.foilen.infra.plugin.v1.core.base.resources.DnsPointer;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.model.DnsEntryType;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractCommonMethodUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.eventhandler.CommonMethodUpdateEventHandlerContext;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.google.common.base.Strings;

public class DnsPointerUpdateHandler extends AbstractCommonMethodUpdateEventHandler<DnsPointer> {

    @Override
    protected void commonHandlerExecute(CommonServicesContext services, ChangesContext changes, CommonMethodUpdateEventHandlerContext<DnsPointer> context) {

        IPResourceService resourceService = services.getResourceService();

        DnsPointer resource = context.getResource();

        context.getManagedResourceTypes().add(DnsEntry.class);

        // Use a DnsEntry per machine
        List<Machine> machines = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, LinkTypeConstants.POINTS_TO, Machine.class);

        for (Machine machine : machines) {
            if (!Strings.isNullOrEmpty(machine.getPublicIp())) {
                context.getManagedResources().add(new DnsEntry(resource.getName(), DnsEntryType.A, machine.getPublicIp()));
            }
        }

    }

    @Override
    public Class<DnsPointer> supportedClass() {
        return DnsPointer.class;
    }

}
