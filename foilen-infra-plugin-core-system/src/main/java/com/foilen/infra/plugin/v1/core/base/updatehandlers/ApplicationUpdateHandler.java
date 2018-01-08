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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.foilen.infra.plugin.v1.core.base.resources.Application;
import com.foilen.infra.plugin.v1.core.base.resources.DnsPointer;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.UnixUser;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.exception.IllegalUpdateException;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.foilen.smalltools.tuple.Tuple3;

public class ApplicationUpdateHandler extends AbstractUpdateEventHandler<Application> {

    @Override
    public void addHandler(CommonServicesContext services, ChangesContext changes, Application resource) {
        commonHandler(services, changes, resource);
    }

    @Override
    public void checkAndFix(CommonServicesContext services, ChangesContext changes, Application resource) {
        commonHandler(services, changes, resource);
    }

    private void commonHandler(CommonServicesContext services, ChangesContext changes, Application resource) {

        IPResourceService resourceService = services.getResourceService();

        List<IPResource> neededManagedResources = new ArrayList<>();

        // Update "runAs" with the link RUN_AS -> UnixUser
        List<UnixUser> unixUsers = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, LinkTypeConstants.RUN_AS, UnixUser.class);
        if (unixUsers.size() > 1) {
            throw new IllegalUpdateException("An application cannot have multiple users to run as (only 0 or 1)");
        }
        Integer neededRunAs = null;
        if (!unixUsers.isEmpty()) {
            neededRunAs = unixUsers.get(0).getId();
        }
        Integer currentRunAs = resource.getApplicationDefinition().getRunAs();
        if ((neededRunAs == null && currentRunAs != null) || (neededRunAs != null && !neededRunAs.equals(currentRunAs))) {
            resource.getApplicationDefinition().setRunAs(neededRunAs);
            changes.resourceUpdate(resource.getInternalId(), resource);
        }

        // Create and manage one DnsPointer per "domainNames" ; POINTS_TO Machines that this application is installed on
        List<Machine> installOnMachines = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, LinkTypeConstants.INSTALLED_ON, Machine.class);
        for (String domainName : resource.getDomainNames()) {
            DnsPointer dnsPointer = new DnsPointer(domainName);
            dnsPointer = retrieveOrCreateResource(resourceService, changes, dnsPointer, DnsPointer.class);
            updateLinksOnResource(services, changes, dnsPointer, LinkTypeConstants.POINTS_TO, Machine.class, installOnMachines);

            neededManagedResources.add(dnsPointer);
        }

        // Check that all Applications on each Machine that it is installed on has unique ports exposed per machine
        for (Machine installedOnMachine : installOnMachines) {
            List<Application> applicationsOnMachine = resourceService.linkFindAllByFromResourceClassAndLinkTypeAndToResource(Application.class, LinkTypeConstants.INSTALLED_ON, installedOnMachine);
            Set<Integer> endpointPorts = new HashSet<>();
            for (Application application : applicationsOnMachine) {
                for (Integer port : application.getApplicationDefinition().getPortsExposed().keySet()) {
                    if (!endpointPorts.add(port)) {
                        throw new IllegalUpdateException("The port " + port + " is exposed by many applications installed on " + installedOnMachine.getName());
                    }
                }
            }
        }

        manageNeededResourcesNoUpdates(services, changes, resource, neededManagedResources, Arrays.asList(DnsPointer.class));
    }

    @Override
    public void deleteHandler(CommonServicesContext services, ChangesContext changes, Application resource, List<Tuple3<IPResource, String, IPResource>> previousLinks) {
        detachManagedResources(services, changes, resource, previousLinks);
    }

    @Override
    public Class<Application> supportedClass() {
        return Application.class;
    }

    @Override
    public void updateHandler(CommonServicesContext services, ChangesContext changes, Application previousResource, Application newResource) {
        commonHandler(services, changes, newResource);
    }

}
