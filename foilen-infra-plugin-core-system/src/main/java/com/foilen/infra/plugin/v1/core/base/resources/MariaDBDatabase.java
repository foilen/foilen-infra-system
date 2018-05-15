/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.resources;

import com.foilen.infra.plugin.v1.model.resource.AbstractIPResource;
import com.foilen.infra.plugin.v1.model.resource.InfraPluginResourceCategory;
import com.google.common.collect.ComparisonChain;

/**
 * This is for a MariaDB Database. <br>
 * Links to:
 * <ul>
 * <li>{@link MariaDBServer}: (optional / many) INSTALLED_ON - On which server to install it.</li>
 * </ul>
 */
public class MariaDBDatabase extends AbstractIPResource implements Comparable<MariaDBDatabase> {

    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_DESCRIPTION = "description";

    // Basics
    private String name;
    private String description;

    public MariaDBDatabase() {
    }

    public MariaDBDatabase(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public int compareTo(MariaDBDatabase o) {
        return ComparisonChain.start() //
                .compare(this.name, o.name) //
                .result();
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    @Override
    public InfraPluginResourceCategory getResourceCategory() {
        return InfraPluginResourceCategory.DATABASE;
    }

    @Override
    public String getResourceDescription() {
        return description;
    }

    @Override
    public String getResourceName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

}
