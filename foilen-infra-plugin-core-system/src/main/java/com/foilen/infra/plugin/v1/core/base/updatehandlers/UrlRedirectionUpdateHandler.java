/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.updatehandlers;

import java.util.List;

import com.foilen.infra.plugin.v1.core.base.resources.DnsPointer;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.UrlRedirection;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractCommonMethodUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.eventhandler.CommonMethodUpdateEventHandlerContext;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;

public class UrlRedirectionUpdateHandler extends AbstractCommonMethodUpdateEventHandler<UrlRedirection> {

    @Override
    protected void commonHandlerExecute(CommonServicesContext services, ChangesContext changes, CommonMethodUpdateEventHandlerContext<UrlRedirection> context) {

        IPResourceService resourceService = services.getResourceService();

        UrlRedirection resource = context.getResource();

        // Create and manage : DnsPointer (attach Machines)
        List<Machine> installOnMachines = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, LinkTypeConstants.INSTALLED_ON, Machine.class);
        String domainName = resource.getDomainName();
        DnsPointer dnsPointer = new DnsPointer(domainName);
        dnsPointer = retrieveOrCreateResource(resourceService, changes, dnsPointer, DnsPointer.class);
        updateLinksOnResource(services, changes, dnsPointer, LinkTypeConstants.POINTS_TO, Machine.class, installOnMachines);

        context.getManagedResources().add(dnsPointer);

    }

    @Override
    public Class<UrlRedirection> supportedClass() {
        return UrlRedirection.class;
    }

}
