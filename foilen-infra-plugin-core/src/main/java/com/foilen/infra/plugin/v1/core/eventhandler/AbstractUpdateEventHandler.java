/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.eventhandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.exception.ResourceNotFromRepositoryException;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.foilen.smalltools.listscomparator.ListComparatorHandler;
import com.foilen.smalltools.listscomparator.ListsComparator;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tuple.Tuple3;

public abstract class AbstractUpdateEventHandler<R extends IPResource> extends AbstractBasics implements UpdateEventHandler<R> {

    protected void detachManagedResources(CommonServicesContext services, ChangesContext changes, IPResource resource, List<Tuple3<IPResource, String, IPResource>> previousLinks) {

        IPResourceService resourceService = services.getResourceService();

        // Get the currently managed resources
        List<IPResource> currentlyManagedResources = linkFindAllByFromResourceAndLinkType(previousLinks, resource, LinkTypeConstants.MANAGES);

        // Detach them
        for (IPResource currentlyManagedResource : currentlyManagedResources) {
            removeManagedLinkAndDeleteIfNotManagedByAnyoneElse(resourceService, changes, currentlyManagedResource, resource);
        }
    }

    protected List<IPResource> linkFindAllByFromResourceAndLinkType(List<Tuple3<IPResource, String, IPResource>> previousLinks, IPResource fromResource, String linkType) {
        if (fromResource.getInternalId() == null) {
            throw new ResourceNotFromRepositoryException(fromResource);
        }
        return previousLinks.stream().filter( //
                it -> {
                    return fromResource.getInternalId().equals(it.getA().getInternalId()) && //
                    linkType.equals(it.getB());
                }) //
                .map(it -> it.getC()) //
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected <T extends IPResource> List<T> linkFindAllByFromResourceAndLinkTypeAndToResourceClass(List<Tuple3<IPResource, String, IPResource>> previousLinks, R fromResource, String linkType,
            Class<T> toResourceType) {
        if (fromResource.getInternalId() == null) {
            throw new ResourceNotFromRepositoryException(fromResource);
        }
        return previousLinks.stream().filter( //
                it -> {
                    return fromResource.getInternalId().equals(it.getA().getInternalId()) && //
                    linkType.equals(it.getB()) && //
                    toResourceType.isInstance(it.getC());
                }) //
                .map(it -> (T) it.getC()) //
                .collect(Collectors.toList());
    }

    protected void manageNeededResources(CommonServicesContext services, ChangesContext changes, IPResource resource, List<IPResource> neededManagedResources,
            List<Class<? extends IPResource>> managedResourceTypes) {

        IPResourceService resourceService = services.getResourceService();

        // Get the currently managed resources
        List<IPResource> currentlyManagedResources = new ArrayList<>();
        for (Class<? extends IPResource> managedResourceType : managedResourceTypes) {
            currentlyManagedResources.addAll(resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, LinkTypeConstants.MANAGES, managedResourceType));
        }

        // Find or create the needed resources
        neededManagedResources = neededManagedResources.stream().map(it -> retrieveOrCreateResource(resourceService, changes, it)).collect(Collectors.toList());

        // Create the links
        List<Long> neededManagedResourceIds = new ArrayList<>();
        for (IPResource neededManagedResource : neededManagedResources) {
            changes.getLinksToAdd().add(new Tuple3<>(resource, LinkTypeConstants.MANAGES, neededManagedResource));
            if (neededManagedResource.getInternalId() != null) {
                neededManagedResourceIds.add(neededManagedResource.getInternalId());
            }
        }

        // Remove the previously no more used links
        for (IPResource currentlyManagedResource : currentlyManagedResources) {
            if (!neededManagedResourceIds.contains(currentlyManagedResource.getInternalId())) {
                removeManagedLinkAndDeleteIfNotManagedByAnyoneElse(resourceService, changes, currentlyManagedResource, resource);
            }
        }
    }

    protected void removeManagedLinkAndDeleteIfNotManagedByAnyoneElse(IPResourceService resourceService, ChangesContext changes, Collection<? extends IPResource> managedResources,
            IPResource... removeLinksFrom) {

        for (IPResource managedResource : managedResources) {
            removeManagedLinkAndDeleteIfNotManagedByAnyoneElse(resourceService, changes, managedResource, removeLinksFrom);
        }

    }

    protected void removeManagedLinkAndDeleteIfNotManagedByAnyoneElse(IPResourceService resourceService, ChangesContext changes, IPResource managedResource, IPResource... removeLinksFrom) {

        List<? extends IPResource> fromResources = resourceService.linkFindAllByLinkTypeAndToResource(LinkTypeConstants.MANAGES, managedResource);
        logger.debug("Resource {} is managed by {} other resources", managedResource, fromResources.size());

        // Remove the managed links
        int removedLinks = 0;
        List<Long> removeLinksFromIds = Arrays.asList(removeLinksFrom).stream().map(IPResource::getInternalId).collect(Collectors.toList());
        for (IPResource fromResource : fromResources) {
            if (removeLinksFromIds.contains(fromResource.getInternalId())) {
                ++removedLinks;
                changes.getLinksToDelete().add(new Tuple3<>(fromResource, LinkTypeConstants.MANAGES, managedResource));
            }
        }
        logger.debug("Resource {} is now managed by {} other resources", managedResource, fromResources.size() - removedLinks);

        // All managed links removed and not manually edited-> Delete the resource
        if (fromResources.size() - removedLinks == 0 && managedResource.getResourceEditorName() == null) {
            changes.getResourcesToDelete().add(managedResource.getInternalId());
        }
    }

    protected IPResource retrieveOrCreateResource(IPResourceService resourceService, ChangesContext changes, IPResource resource) {

        // Search in current changes
        Optional<IPResource> foundOptional = changes.getResourcesToAdd().stream() //
                .filter(it -> resourceService.resourceEqualsPk(resource, it)) //
                .findAny();
        if (foundOptional.isPresent()) {
            return foundOptional.get();
        }

        foundOptional = changes.getResourcesToUpdate().stream() //
                .filter(it -> resourceService.resourceEqualsPk(resource, it.getB())) //
                .map(it -> it.getB()) //
                .findAny();
        if (foundOptional.isPresent()) {
            return foundOptional.get();
        }

        // Search in repository
        foundOptional = resourceService.resourceFindByPk(resource);
        if (foundOptional.isPresent()) {
            return foundOptional.get();
        }

        // Create new
        changes.getResourcesToAdd().add(resource);
        return resource;

    }

    @SuppressWarnings("unchecked")
    protected <T extends IPResource> T retrieveOrCreateResource(IPResourceService resourceService, ChangesContext changes, T resource, Class<T> resourceClass) {

        // Search in current changes
        Optional<T> foundOptional = changes.getResourcesToAdd().stream() //
                .filter(it -> resourceService.resourceEqualsPk(resource, it)) //
                .map(it -> (T) it) //
                .findAny();
        if (foundOptional.isPresent()) {
            return foundOptional.get();
        }

        foundOptional = changes.getResourcesToUpdate().stream() //
                .filter(it -> resourceService.resourceEqualsPk(resource, it.getB())) //
                .map(it -> (T) it.getB()) //
                .findAny();
        if (foundOptional.isPresent()) {
            return foundOptional.get();
        }

        // Search in repository
        foundOptional = resourceService.resourceFindByPk(resource);
        if (foundOptional.isPresent()) {
            return foundOptional.get();
        }

        // Create new
        changes.getResourcesToAdd().add(resource);
        return resource;

    }

    protected <T extends Comparable<T> & IPResource> void updateLinksOnResource(CommonServicesContext services, ChangesContext changes, IPResource fromResource, String linkType,
            Class<T> toResourceClass, List<T> finalTos) {

        List<T> currentTos;
        if (fromResource.getInternalId() == null) {
            currentTos = new ArrayList<>();
        } else {
            currentTos = services.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(fromResource, linkType, toResourceClass);
        }

        Collections.sort(finalTos);
        Collections.sort(currentTos);
        IPResource fromDnsPointer = fromResource;
        ListsComparator.compareLists(finalTos, currentTos, new ListComparatorHandler<T, T>() {

            @Override
            public void both(T left, T right) {
                // Good
            }

            @Override
            public void leftOnly(T left) {
                // Create
                changes.getLinksToAdd().add(new Tuple3<>(fromDnsPointer, linkType, left));
            }

            @Override
            public void rightOnly(T right) {
                // Remove
                changes.getLinksToDelete().add(new Tuple3<>(fromDnsPointer, linkType, right));
            }
        });
    }

}
