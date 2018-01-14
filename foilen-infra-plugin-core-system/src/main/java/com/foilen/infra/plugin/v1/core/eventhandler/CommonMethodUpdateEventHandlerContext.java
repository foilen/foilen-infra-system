/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.eventhandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.foilen.infra.plugin.v1.model.resource.IPResource;

public class CommonMethodUpdateEventHandlerContext<R extends IPResource> {

    private R oldResource;
    private R resource;

    private List<IPResource> managedResources = new ArrayList<>();
    private List<Class<? extends IPResource>> managedResourceTypes = new ArrayList<>();
    private boolean managedResourcesUpdateContentIfExists = false;

    public void addManagedResources(IPResource... resources) {
        managedResources.addAll(Arrays.asList(resources));
    }

    @SafeVarargs
    public final void addManagedResourceTypes(Class<? extends IPResource>... types) {
        managedResourceTypes.addAll(Arrays.asList(types));
    }

    public List<IPResource> getManagedResources() {
        return managedResources;
    }

    public List<Class<? extends IPResource>> getManagedResourceTypes() {
        return managedResourceTypes;
    }

    public R getOldResource() {
        return oldResource;
    }

    public R getResource() {
        return resource;
    }

    public boolean isManagedResourcesUpdateContentIfExists() {
        return managedResourcesUpdateContentIfExists;
    }

    public void setManagedResources(List<IPResource> managedResources) {
        this.managedResources = managedResources;
    }

    public void setManagedResourcesUpdateContentIfExists(boolean managedResourcesUpdateContentIfExists) {
        this.managedResourcesUpdateContentIfExists = managedResourcesUpdateContentIfExists;
    }

    public void setManagedResourceTypes(List<Class<? extends IPResource>> managedResourceTypes) {
        this.managedResourceTypes = managedResourceTypes;
    }

    public void setOldResource(R oldResource) {
        this.oldResource = oldResource;
    }

    public void setResource(R resource) {
        this.resource = resource;
    }

}
