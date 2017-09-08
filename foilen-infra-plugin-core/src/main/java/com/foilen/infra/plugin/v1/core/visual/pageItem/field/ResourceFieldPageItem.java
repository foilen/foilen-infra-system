/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.pageItem.field;

import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.google.common.base.MoreObjects;

/**
 * A resource selector or creator.
 */
public class ResourceFieldPageItem<R extends IPResource> extends AbstractFieldPageItem {

    private Class<R> resourceType;
    private R value;

    private String label;

    public ResourceFieldPageItem() {
    }

    public ResourceFieldPageItem(Class<R> resourceType, R value) {
        this.resourceType = resourceType;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public Class<R> getResourceType() {
        return resourceType;
    }

    public R getValue() {
        return value;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ResourceFieldPageItem<R> setResourceType(Class<R> resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public ResourceFieldPageItem<R> setValue(R value) {
        this.value = value;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this) //
                .add("resourceType", resourceType) //
                .add("value", value) //
                .toString();
    }

}
