/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.junits;

import com.foilen.smalltools.tools.AbstractBasics;
import com.google.common.collect.ComparisonChain;

public class ResourcesDumpTag extends AbstractBasics implements Comparable<ResourcesDumpTag> {

    private String resourceTypeAndName;
    private String tag;

    public ResourcesDumpTag() {
    }

    public ResourcesDumpTag(String resourceTypeAndName, String tag) {
        this.resourceTypeAndName = resourceTypeAndName;
        this.tag = tag;
    }

    @Override
    public int compareTo(ResourcesDumpTag o) {
        return ComparisonChain.start() //
                .compare(resourceTypeAndName, o.resourceTypeAndName) //
                .compare(tag, o.tag) //
                .result();
    }

    public String getResourceTypeAndName() {
        return resourceTypeAndName;
    }

    public String getTag() {
        return tag;
    }

    public void setResourceTypeAndName(String resourceTypeAndName) {
        this.resourceTypeAndName = resourceTypeAndName;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

}
