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
 * This is a domain/sub-domain name. Mostly used to make sure the users only use the domains they own.<br>
 * Links to:
 * <ul>
 * </ul>
 *
 * Manages:
 * <ul>
 * <li>{@link Domain}: Creates/uses a parent {@link Domain} to make sure it is owned by the user</li>
 * </ul>
 */
public class Domain extends AbstractIPResource implements Comparable<Domain> {

    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_REVERSE_NAME = "reverseName";

    private String name;
    private String reverseName;

    public Domain() {
    }

    public Domain(String name, String reverseName) {
        this.name = name;
        this.reverseName = reverseName;
    }

    @Override
    public int compareTo(Domain o) {
        ComparisonChain cc = ComparisonChain.start();
        cc = cc.compare(name, o.name);
        return cc.result();
    }

    public String getName() {
        return name;
    }

    @Override
    public InfraPluginResourceCategory getResourceCategory() {
        return InfraPluginResourceCategory.NET;
    }

    @Override
    public String getResourceDescription() {
        return "Domain for " + name;
    }

    @Override
    public String getResourceName() {
        return name;
    }

    public String getReverseName() {
        return reverseName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setReverseName(String reverseName) {
        this.reverseName = reverseName;
    }

}
