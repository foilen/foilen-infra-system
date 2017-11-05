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
import com.foilen.infra.plugin.v1.core.base.resources.Domain;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.model.DnsEntryType;
import com.foilen.infra.plugin.v1.core.common.DomainHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.exception.IllegalUpdateException;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tools.StringTools;
import com.foilen.smalltools.tuple.Tuple3;

public class MachineUpdateHandler extends AbstractUpdateEventHandler<Machine> {

    @Override
    public void addHandler(CommonServicesContext services, ChangesContext changes, Machine resource) {
        commonHandler(services, changes, resource);
    }

    @Override
    public void checkAndFix(CommonServicesContext services, ChangesContext changes, Machine resource) {
        commonHandler(services, changes, resource);
    }

    private void commonHandler(CommonServicesContext services, ChangesContext changes, Machine resource) {
        List<IPResource> neededManagedResources = new ArrayList<>();
        if (resource.getPublicIp() == null) {
            // Use a Domain
            neededManagedResources.add(new Domain(resource.getName(), DomainHelper.reverseDomainName(resource.getName())));
        } else {
            // Use a DnsEntry
            neededManagedResources.add(new DnsEntry(resource.getName(), DnsEntryType.A, resource.getPublicIp()));
        }

        manageNeededResources(services, changes, resource, neededManagedResources, Arrays.asList(DnsEntry.class, Domain.class));
    }

    @Override
    public void deleteHandler(CommonServicesContext services, ChangesContext changes, Machine resource, List<Tuple3<IPResource, String, IPResource>> previousLinks) {
        detachManagedResources(services, changes, resource, previousLinks);
    }

    @Override
    public Class<Machine> supportedClass() {
        return Machine.class;
    }

    @Override
    public void updateHandler(CommonServicesContext services, ChangesContext changes, Machine previousResource, Machine newResource) {

        if (!StringTools.safeEquals(previousResource.getName(), newResource.getName())) {
            throw new IllegalUpdateException("You cannot change a Machine's name");
        }

        commonHandler(services, changes, newResource);

    }

}
