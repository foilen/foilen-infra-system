/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.repositories.documents;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Lnks between plugins.
 */
@Document
public class PluginResourceLink {

    @Id
    private String id;
    @Version
    private long version;

    private String fromResourceId;
    private String fromResourceType;

    private String linkType;

    private String toResourceId;
    private String toResourceType;

    public PluginResourceLink() {
    }

    public PluginResourceLink(String fromResourceId, String fromResourceType, String linkType, String toResourceId, String toResourceType) {
        this.fromResourceId = fromResourceId;
        this.fromResourceType = fromResourceType;
        this.linkType = linkType;
        this.toResourceId = toResourceId;
        this.toResourceType = toResourceType;
    }

    public String getFromResourceId() {
        return fromResourceId;
    }

    public String getFromResourceType() {
        return fromResourceType;
    }

    public String getId() {
        return id;
    }

    public String getLinkType() {
        return linkType;
    }

    public String getToResourceId() {
        return toResourceId;
    }

    public String getToResourceType() {
        return toResourceType;
    }

    public void setFromResourceId(String fromResourceId) {
        this.fromResourceId = fromResourceId;
    }

    public void setFromResourceType(String fromResourceType) {
        this.fromResourceType = fromResourceType;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public void setToResourceId(String toResourceId) {
        this.toResourceId = toResourceId;
    }

    public void setToResourceType(String toResourceType) {
        this.toResourceType = toResourceType;
    }

}
