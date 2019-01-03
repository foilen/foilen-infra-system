/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.junits;

import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.JsonTools;
import com.google.common.collect.ComparisonChain;

public class ResourcesDumpResource extends AbstractBasics implements Comparable<ResourcesDumpResource> {

    private String resourceType;
    private String resourceName;
    private String resourceJson;

    public ResourcesDumpResource() {
    }

    public ResourcesDumpResource(String resourceType, IPResource resource) {
        this.resourceType = resourceType;
        this.resourceName = resource.getResourceName();
        this.resourceJson = JsonTools.compactPrint(resource);
    }

    @Override
    public int compareTo(ResourcesDumpResource o) {
        return ComparisonChain.start() //
                .compare(resourceType, o.resourceType) //
                .compare(resourceName, o.resourceName) //
                .result();
    }

    public String getResourceJson() {
        return resourceJson;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceJson(String resourceJson) {
        this.resourceJson = resourceJson;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

}
