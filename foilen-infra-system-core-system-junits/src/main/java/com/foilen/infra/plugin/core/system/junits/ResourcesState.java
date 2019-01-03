/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.junits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResourcesState {

    private List<ResourceState> resources = new ArrayList<>();

    public List<ResourceState> getResources() {
        return resources;
    }

    public void setResources(List<ResourceState> resources) {
        this.resources = resources;
    }

    public void sort() {

        Collections.sort(resources, (a, b) -> a.getResource().compareTo(b.getResource()));

        for (ResourceState resource : resources) {
            resource.sort();
        }

    }

}
