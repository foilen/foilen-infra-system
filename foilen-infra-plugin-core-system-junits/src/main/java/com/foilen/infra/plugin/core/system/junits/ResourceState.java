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

import com.google.common.collect.ComparisonChain;

public class ResourceState {

    private String resource;
    private List<ResourcesStateLink> links = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private String contentInJson;

    public ResourceState(String resource) {
        this.resource = resource;
    }

    public String getContentInJson() {
        return contentInJson;
    }

    public List<ResourcesStateLink> getLinks() {
        return links;
    }

    public String getResource() {
        return resource;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setContentInJson(String contentInJson) {
        this.contentInJson = contentInJson;
    }

    public void setLinks(List<ResourcesStateLink> links) {
        this.links = links;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void sort() {
        Collections.sort(links, (a, b) -> {
            return ComparisonChain.start() //
                    .compare(a.getType(), b.getType()) //
                    .compare(a.getTo(), b.getTo()) //
                    .result();
        });
        Collections.sort(tags);
    }

}
