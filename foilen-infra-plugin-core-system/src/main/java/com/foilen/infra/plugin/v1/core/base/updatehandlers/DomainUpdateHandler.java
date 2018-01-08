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

import com.foilen.infra.plugin.v1.core.base.resources.Domain;
import com.foilen.infra.plugin.v1.core.common.DomainHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.exception.IllegalUpdateException;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tools.StringTools;
import com.foilen.smalltools.tuple.Tuple3;

public class DomainUpdateHandler extends AbstractUpdateEventHandler<Domain> {

    @Override
    public void addHandler(CommonServicesContext services, ChangesContext changes, Domain resource) {
        commonHandler(services, changes, resource);

    }

    @Override
    public void checkAndFix(CommonServicesContext services, ChangesContext changes, Domain resource) {
        commonHandler(services, changes, resource);
    }

    private void commonHandler(CommonServicesContext services, ChangesContext changes, Domain resource) {
        List<IPResource> neededManagedResources = new ArrayList<>();

        // Use a Domain for the parent domain
        String parentDomainName = DomainHelper.parentDomainName(resource.getName());
        if (parentDomainName == null) {
            return;
        }
        neededManagedResources.add(new Domain(parentDomainName, DomainHelper.reverseDomainName(parentDomainName)));

        manageNeededResourcesNoUpdates(services, changes, resource, neededManagedResources, Arrays.asList(Domain.class));
    }

    @Override
    public void deleteHandler(CommonServicesContext services, ChangesContext changes, Domain resource, List<Tuple3<IPResource, String, IPResource>> previousLinks) {
        detachManagedResources(services, changes, resource, previousLinks);
    }

    @Override
    public Class<Domain> supportedClass() {
        return Domain.class;
    }

    @Override
    public void updateHandler(CommonServicesContext services, ChangesContext changes, Domain previousResource, Domain newResource) {

        if (!StringTools.safeEquals(previousResource.getName(), newResource.getName())) {
            throw new IllegalUpdateException("You cannot change a Domain's name");
        }

        commonHandler(services, changes, newResource);

    }

}
