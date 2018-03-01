/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.junits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.foilen.smalltools.tools.AbstractBasics;

public class ResourcesDump extends AbstractBasics {

    private List<ResourcesDumpResource> resources = new ArrayList<>();
    private List<ResourcesDumpTag> tags = new ArrayList<>();
    private List<ResourcesDumpLink> links = new ArrayList<>();

    public List<ResourcesDumpLink> getLinks() {
        return links;
    }

    public List<ResourcesDumpResource> getResources() {
        return resources;
    }

    public List<ResourcesDumpTag> getTags() {
        return tags;
    }

    public void setLinks(List<ResourcesDumpLink> links) {
        this.links = links;
    }

    public void setResources(List<ResourcesDumpResource> resources) {
        this.resources = resources;
    }

    public void setTags(List<ResourcesDumpTag> tags) {
        this.tags = tags;
    }

    public void sort() {
        Collections.sort(resources);
        Collections.sort(tags);
        Collections.sort(links);

    }

}
