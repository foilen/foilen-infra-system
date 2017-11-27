/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.updatehandlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.foilen.infra.plugin.v1.core.base.resources.DnsEntry;
import com.foilen.infra.plugin.v1.core.base.resources.DnsPointer;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.model.DnsEntryType;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.foilen.smalltools.tuple.Tuple3;
import com.google.common.base.Strings;

public class DnsPointerUpdateHandler extends AbstractUpdateEventHandler<DnsPointer> {

    @Override
    public void addHandler(CommonServicesContext services, ChangesContext changes, DnsPointer resource) {
        commonHandler(services, changes, resource);
    }

    @Override
    public void checkAndFix(CommonServicesContext services, ChangesContext changes, DnsPointer resource) {
        commonHandler(services, changes, resource);
    }

    private void commonHandler(CommonServicesContext services, ChangesContext changes, DnsPointer resource) {
        List<IPResource> neededManagedResources = new ArrayList<>();

        IPResourceService resourceService = services.getResourceService();

        // Use a DnsEntry per machine
        List<Machine> machines = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, LinkTypeConstants.POINTS_TO, Machine.class);

        for (Machine machine : machines) {
            if (!Strings.isNullOrEmpty(machine.getPublicIp())) {
                neededManagedResources.add(new DnsEntry(resource.getName(), DnsEntryType.A, machine.getPublicIp()));
            }
        }

        manageNeededResourcesNoUpdates(services, changes, resource, neededManagedResources, Arrays.asList(DnsEntry.class));
    }

    @Override
    public void deleteHandler(CommonServicesContext services, ChangesContext changes, DnsPointer resource, List<Tuple3<IPResource, String, IPResource>> previousLinks) {
        detachManagedResources(services, changes, resource, previousLinks);
    }

    @Override
    public Class<DnsPointer> supportedClass() {
        return DnsPointer.class;
    }

    @Override
    public void updateHandler(CommonServicesContext services, ChangesContext changes, DnsPointer previousResource, DnsPointer newResource) {
        commonHandler(services, changes, newResource);
    }

}
