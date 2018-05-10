/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.junits;

import com.foilen.smalltools.tools.AbstractBasics;
import com.google.common.collect.ComparisonChain;

public class ResourcesDumpLink extends AbstractBasics implements Comparable<ResourcesDumpLink> {

    private String fromResourceTypeAndName;
    private String linkType;
    private String toResourceTypeAndName;

    public ResourcesDumpLink() {
    }

    public ResourcesDumpLink(String fromResourceTypeAndName, String linkType, String toResourceTypeAndName) {
        this.fromResourceTypeAndName = fromResourceTypeAndName;
        this.linkType = linkType;
        this.toResourceTypeAndName = toResourceTypeAndName;
    }

    @Override
    public int compareTo(ResourcesDumpLink o) {
        return ComparisonChain.start() //
                .compare(fromResourceTypeAndName, o.fromResourceTypeAndName) //
                .compare(linkType, o.linkType) //
                .compare(toResourceTypeAndName, o.toResourceTypeAndName) //
                .result();
    }

    public String getFromResourceTypeAndName() {
        return fromResourceTypeAndName;
    }

    public String getLinkType() {
        return linkType;
    }

    public String getToResourceTypeAndName() {
        return toResourceTypeAndName;
    }

    public void setFromResourceTypeAndName(String fromResourceTypeAndName) {
        this.fromResourceTypeAndName = fromResourceTypeAndName;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public void setToResourceTypeAndName(String toResourceTypeAndName) {
        this.toResourceTypeAndName = toResourceTypeAndName;
    }

}
