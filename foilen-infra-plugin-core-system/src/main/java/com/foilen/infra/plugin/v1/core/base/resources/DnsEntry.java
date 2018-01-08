/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.resources;

import java.util.Objects;

import com.foilen.infra.plugin.v1.core.base.resources.model.DnsEntryType;
import com.foilen.infra.plugin.v1.model.resource.AbstractIPResource;
import com.foilen.infra.plugin.v1.model.resource.InfraPluginResourceCategory;
import com.google.common.collect.ComparisonChain;

/**
 * This is a DNS entry.<br/>
 *
 * Links to:
 * <ul>
 * </ul>
 *
 * Manages:
 * <ul>
 * <li>{@link Domain}: Creates/uses a {@link Domain} to make sure it is owned by the user</li>
 * </ul>
 */
public class DnsEntry extends AbstractIPResource implements Comparable<DnsEntry> {

    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_TYPE = "type";
    public static final String PROPERTY_DETAILS = "details";

    private String name;
    private DnsEntryType type;
    private String details;

    public DnsEntry() {
    }

    public DnsEntry(String name, DnsEntryType type, String details) {
        this.name = name;
        this.type = type;
        this.details = details;
    }

    @Override
    public int compareTo(DnsEntry o) {
        ComparisonChain cc = ComparisonChain.start();
        cc = cc.compare(name, o.name);
        cc = cc.compare(type, o.type);
        cc = cc.compare(details, o.details);
        return cc.result();
    }

    @Override
    public boolean equals(Object o) {

        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o.getClass() != getClass()) {
            return false;
        }

        DnsEntry se = (DnsEntry) o;
        return Objects.equals(name, se.name) //
                && Objects.equals(type, se.type) //
                && Objects.equals(details, se.details);
    }

    public String getDetails() {
        return details;
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
        return details;
    }

    @Override
    public String getResourceName() {
        return name + " / " + type;
    }

    public DnsEntryType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(details, name, type);
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(DnsEntryType type) {
        this.type = type;
    }

}
