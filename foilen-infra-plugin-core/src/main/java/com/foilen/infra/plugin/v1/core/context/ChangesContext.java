/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.context;

import java.util.ArrayList;
import java.util.List;

import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tuple.Tuple2;
import com.foilen.smalltools.tuple.Tuple3;

/**
 * Every add/update/remove on the system.
 */
public class ChangesContext {

    private List<IPResource> resourcesToAdd = new ArrayList<>();
    private List<Tuple2<Long, IPResource>> resourcesToUpdate = new ArrayList<>();
    private List<Long> resourcesToDelete = new ArrayList<>();

    private List<Tuple2<IPResource, String>> tagsToAdd = new ArrayList<>();
    private List<Tuple2<IPResource, String>> tagsToDelete = new ArrayList<>();

    private List<Tuple3<IPResource, String, IPResource>> linksToAdd = new ArrayList<>();
    private List<Tuple3<IPResource, String, IPResource>> linksToDelete = new ArrayList<>();

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
        return linksToAdd;
    }

    /**
     * Get the links to delete in the format: from -> linkType -> to.
     *
     * @return the list of links to delete
     */
    public List<Tuple3<IPResource, String, IPResource>> getLinksToDelete() {
        return linksToDelete;
    }

    public List<IPResource> getResourcesToAdd() {
        return resourcesToAdd;
    }

    public List<Long> getResourcesToDelete() {
        return resourcesToDelete;
    }

    /**
     * Get the resources to update in the format: currentResourceInternalId -> updatedResource
     *
     * @return the list of resources to update
     */
    public List<Tuple2<Long, IPResource>> getResourcesToUpdate() {
        return resourcesToUpdate;
    }

    public List<Tuple2<IPResource, String>> getTagsToAdd() {
        return tagsToAdd;
    }

    public List<Tuple2<IPResource, String>> getTagsToDelete() {
        return tagsToDelete;
    }

    public boolean hasChanges() {
        return !resourcesToAdd.isEmpty() || !resourcesToDelete.isEmpty() || !resourcesToUpdate.isEmpty() || //
                !tagsToAdd.isEmpty() || !tagsToDelete.isEmpty() || //
                !linksToAdd.isEmpty() || !linksToDelete.isEmpty();
    }

}
