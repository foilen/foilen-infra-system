/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.withparent;

import com.foilen.infra.plugin.v1.model.resource.AbstractIPResource;
import com.foilen.infra.plugin.v1.model.resource.InfraPluginResourceCategory;

public abstract class AbstractParent extends AbstractIPResource {

    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_ON_PARENT = "onParent";

    private String name;
    private String onParent;

    public AbstractParent() {
    }

    public AbstractParent(String name, String onParent) {
        this.name = name;
        this.onParent = onParent;
    }

    public String getName() {
        return name;
    }

    public String getOnParent() {
        return onParent;
    }

    @Override
    public InfraPluginResourceCategory getResourceCategory() {
        return InfraPluginResourceCategory.INFRASTRUCTURE;
    }

    @Override
    public String getResourceDescription() {
        return "Testing the hierarchy";
    }

    @Override
    public String getResourceName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOnParent(String onParent) {
        this.onParent = onParent;
    }

}
