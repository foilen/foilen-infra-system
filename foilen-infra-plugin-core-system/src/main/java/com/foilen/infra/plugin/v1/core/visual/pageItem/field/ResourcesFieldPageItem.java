/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.pageItem.field;

import java.util.ArrayList;
import java.util.List;

import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.google.common.base.MoreObjects;

/**
 * A resource selector or creator.
 */
public class ResourcesFieldPageItem<R extends IPResource> extends AbstractFieldPageItem {

    private Class<R> resourceType;
    private List<R> values = new ArrayList<>();

    private String label;

    public String getLabel() {
        return label;
    }

    public Class<R> getResourceType() {
        return resourceType;
    }

    public List<R> getValues() {
        return values;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ResourcesFieldPageItem<R> setResourceType(Class<R> resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public ResourcesFieldPageItem<R> setValues(List<R> values) {
        this.values = values;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this) //
                .add("resourceType", resourceType) //
                .add("values", values) //
                .toString();
    }

}
