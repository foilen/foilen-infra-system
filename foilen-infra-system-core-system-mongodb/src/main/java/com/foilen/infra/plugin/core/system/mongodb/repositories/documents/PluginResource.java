/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.repositories.documents;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tools.AssertTools;

/**
 * A Plugin resource.
 */
@Document
public class PluginResource {

    @Id
    private String id;
    @Version
    private long version;

    private String editorName;

    private String type;

    private IPResource resource;

    private Set<String> tags = new HashSet<>();

    public PluginResource() {
    }

    public PluginResource(String resourceType, IPResource resource) {
        store(resourceType, resource);
    }

    public PluginResource addTag(String tagName) {
        tags.add(tagName);
        return this;
    }

    public String getEditorName() {
        return editorName;
    }

    public String getId() {
        return id;
    }

    public IPResource getResource() {
        resource.setInternalId(id);
        return resource;
    }

    public Set<String> getTags() {
        return tags;
    }

    public String getType() {
        return type;
    }

    public long getVersion() {
        return version;
    }

    public PluginResource setEditorName(String editorName) {
        this.editorName = editorName;
        return this;
    }

    public PluginResource setId(String id) {
        this.id = id;
        return this;
    }

    public PluginResource setResource(IPResource resource) {
        this.resource = resource;
        return this;
    }

    public PluginResource setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public PluginResource setType(String type) {
        this.type = type;
        return this;
    }

    public PluginResource setVersion(long version) {
        this.version = version;
        return this;
    }

    public void store(String resourceType, IPResource resource) {
        AssertTools.assertNotNull(resourceType, "The resourceType cannot be null");
        AssertTools.assertNotNull(resource, "The resource to store cannot be null");
        type = resourceType;
        this.resource = resource;
        editorName = resource.getResourceEditorName();
    }

}
