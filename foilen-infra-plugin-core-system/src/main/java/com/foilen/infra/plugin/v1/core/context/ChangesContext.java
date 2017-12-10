/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.foilen.infra.plugin.v1.core.exception.IllegalUpdateException;
import com.foilen.infra.plugin.v1.core.exception.ResourcePrimaryKeyCollisionException;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tuple.Tuple2;
import com.foilen.smalltools.tuple.Tuple3;

/**
 * Every add/update/remove on the system.
 */
public class ChangesContext {

    private IPResourceService resourceService;

    private List<IPResource> resourcesToAdd = new ArrayList<>();
    private List<Tuple2<Long, IPResource>> resourcesToUpdate = new ArrayList<>();
    private List<Long> resourcesToDelete = new ArrayList<>();

    private List<Tuple2<IPResource, String>> tagsToAdd = new ArrayList<>();
    private List<Tuple2<IPResource, String>> tagsToDelete = new ArrayList<>();

    private List<Tuple3<IPResource, String, IPResource>> linksToAdd = new ArrayList<>();
    private List<Tuple3<IPResource, String, IPResource>> linksToDelete = new ArrayList<>();

    public ChangesContext(IPResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public void clear() {
        resourcesToAdd.clear();
        resourcesToUpdate.clear();
        resourcesToDelete.clear();
        tagsToAdd.clear();
        tagsToDelete.clear();
        linksToAdd.clear();
        linksToDelete.clear();
    }

    /**
     * Get the links to add in the format: from -> linkType -> to.
     *
     * @return the list of links to add
     */
    public List<Tuple3<IPResource, String, IPResource>> getLinksToAdd() {
        return Collections.unmodifiableList(linksToAdd);
    }

    /**
     * Get the links to delete in the format: from -> linkType -> to.
     *
     * @return the list of links to delete
     */
    public List<Tuple3<IPResource, String, IPResource>> getLinksToDelete() {
        return Collections.unmodifiableList(linksToDelete);
    }

    public List<IPResource> getResourcesToAdd() {
        return Collections.unmodifiableList(resourcesToAdd);
    }

    public List<Long> getResourcesToDelete() {
        return Collections.unmodifiableList(resourcesToDelete);
    }

    /**
     * Get the resources to update in the format: currentResourceInternalId -> updatedResource
     *
     * @return the list of resources to update
     */
    public List<Tuple2<Long, IPResource>> getResourcesToUpdate() {
        return Collections.unmodifiableList(resourcesToUpdate);
    }

    public List<Tuple2<IPResource, String>> getTagsToAdd() {
        return Collections.unmodifiableList(tagsToAdd);
    }

    public List<Tuple2<IPResource, String>> getTagsToDelete() {
        return Collections.unmodifiableList(tagsToDelete);
    }

    public boolean hasChanges() {
        return !resourcesToAdd.isEmpty() || !resourcesToDelete.isEmpty() || !resourcesToUpdate.isEmpty() || //
                !tagsToAdd.isEmpty() || !tagsToDelete.isEmpty() || //
                !linksToAdd.isEmpty() || !linksToDelete.isEmpty();
    }

    public void linkAdd(IPResource resourceFrom, String linkType, IPResource resourceTo) {
        Tuple3<IPResource, String, IPResource> newTuple = new Tuple3<>(resourceFrom, linkType, resourceTo);
        linksToAdd.add(newTuple);

        linksClean(newTuple, linksToDelete);
    }

    public void linkDelete(IPResource resourceFrom, String linkType, IPResource resourceTo) {
        Tuple3<IPResource, String, IPResource> newTuple = new Tuple3<>(resourceFrom, linkType, resourceTo);
        linksToDelete.add(newTuple);

        linksClean(newTuple, linksToAdd);
    }

    protected void linksClean(Tuple3<IPResource, String, IPResource> newTuple, List<Tuple3<IPResource, String, IPResource>> linksList) {
        linksList.removeIf(it -> //
        resourceService.resourceEqualsPk(newTuple.getA(), it.getA()) && //
                newTuple.getB().equals(it.getB()) && //
                resourceService.resourceEqualsPk(newTuple.getC(), it.getC()) //
        );
    }

    public void resourceAdd(IPResource resource) {
        resourcesToAdd.add(resource);

        // Check for conflicts
        if (resourceCheckInList(resource, resourcesToUpdate) || resourceCheckInListIds(resource, resourcesToDelete)) {
            throw new ResourcePrimaryKeyCollisionException();
        }
    }

    protected boolean resourceCheckInList(IPResource resource, List<Tuple2<Long, IPResource>> resources) {
        return resources.stream() //
                .filter(it -> resourceService.resourceEqualsPk(resource, it.getB())) //
                .findAny().isPresent();
    }

    protected boolean resourceCheckInListIds(IPResource resource, List<Long> resourceIds) {
        return resourceIds.stream() //
                .map(it -> resourceService.resourceFind(it).get()) //
                .filter(it -> resourceService.resourceEqualsPk(resource, it)) //
                .findAny().isPresent();
    }

    public void resourceDelete(IPResource resource) {
        Long internalId = resource.getInternalId();
        if (internalId == null) {
            Optional<IPResource> found = resourceService.resourceFindByPk(resource);
            if (found.isPresent()) {
                internalId = found.get().getInternalId();
            }
        }
        resourceDelete(internalId);
    }

    public void resourceDelete(Long resourceId) {
        if (resourceId == null) {
            throw new IllegalUpdateException("Cannot delete a resource without id");
        }
        resourcesToDelete.add(resourceId);

        // Remove in add and update
        Optional<IPResource> resourceOptional = resourceService.resourceFind(resourceId);
        if (resourceOptional.isPresent()) {
            IPResource resource = resourceOptional.get();
            resourcesToAdd.removeIf(it -> resourceService.resourceEqualsPk(resource, it));
        }
        resourcesToUpdate.removeIf(it -> it.getA() == resourceId);

    }

    public void resourceUpdate(IPResource resource) {
        resourceUpdate(resource.getInternalId(), resource);
    }

    public void resourceUpdate(IPResource resource, IPResource updatedResource) {
        resourceUpdate(resource.getInternalId(), updatedResource);
    }

    public void resourceUpdate(Long resourceId, IPResource updatedResource) {
        if (resourceId == null) {
            throw new IllegalUpdateException("Cannot modify a resource without id");
        }

        // Remove previous update
        resourcesToUpdate.removeIf(it -> it.getA() == resourceId);
        resourcesToDelete.removeIf(it -> it == resourceId);

        resourcesToUpdate.add(new Tuple2<>(resourceId, updatedResource));

    }

    public void tagAdd(IPResource resource, String tagName) {
        Tuple2<IPResource, String> newTuple = new Tuple2<>(resource, tagName);
        tagsToAdd.add(newTuple);

        tagsClean(newTuple, tagsToDelete);
    }

    public void tagDelete(IPResource resource, String tagName) {
        Tuple2<IPResource, String> newTuple = new Tuple2<>(resource, tagName);
        tagsToDelete.add(newTuple);

        tagsClean(newTuple, tagsToAdd);
    }

    protected void tagsClean(Tuple2<IPResource, String> newTuple, List<Tuple2<IPResource, String>> tagsList) {
        tagsList.removeIf(it -> //
        resourceService.resourceEqualsPk(newTuple.getA(), it.getA()) && //
                newTuple.getB().equals(it.getB()) //
        );
    }

}
