/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.updatehandlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.foilen.infra.plugin.v1.core.base.resources.DnsPointer;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.UrlRedirection;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.foilen.smalltools.tuple.Tuple3;

public class UrlRedirectionUpdateHandler extends AbstractUpdateEventHandler<UrlRedirection> {

    @Override
    public void addHandler(CommonServicesContext services, ChangesContext changes, UrlRedirection resource) {
        commonHandler(services, changes, resource);
    }

    @Override
    public void checkAndFix(CommonServicesContext services, ChangesContext changes, UrlRedirection resource) {
        commonHandler(services, changes, resource);
    }

    private void commonHandler(CommonServicesContext services, ChangesContext changes, UrlRedirection resource) {
        IPResourceService resourceService = services.getResourceService();

        List<IPResource> neededManagedResources = new ArrayList<>();

        // Create and manage : DnsPointer (attach Machines)
        List<Machine> installOnMachines = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, LinkTypeConstants.INSTALLED_ON, Machine.class);
        String domainName = resource.getDomainName();
        DnsPointer dnsPointer = new DnsPointer(domainName);
        dnsPointer = retrieveOrCreateResource(resourceService, changes, dnsPointer, DnsPointer.class);
        updateLinksOnResource(services, changes, dnsPointer, LinkTypeConstants.POINTS_TO, Machine.class, installOnMachines);

        neededManagedResources.add(dnsPointer);

        manageNeededResourcesNoUpdates(services, changes, resource, neededManagedResources, Arrays.asList(DnsPointer.class));
    }

    @Override
    public void deleteHandler(CommonServicesContext services, ChangesContext changes, UrlRedirection resource, List<Tuple3<IPResource, String, IPResource>> previousLinks) {

        IPResourceService resourceService = services.getResourceService();

        List<DnsPointer> managedDnsPointers = linkFindAllByFromResourceAndLinkTypeAndToResourceClass(previousLinks, resource, LinkTypeConstants.MANAGES, DnsPointer.class);
        removeManagedLinkAndDeleteIfNotManagedByAnyoneElse(resourceService, changes, managedDnsPointers, resource);

    }

    @Override
    public Class<UrlRedirection> supportedClass() {
        return UrlRedirection.class;
    }

    @Override
    public void updateHandler(CommonServicesContext services, ChangesContext changes, UrlRedirection previousResource, UrlRedirection newResource) {
        commonHandler(services, changes, newResource);

    }

}
