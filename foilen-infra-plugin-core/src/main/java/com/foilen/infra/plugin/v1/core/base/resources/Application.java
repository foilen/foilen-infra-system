/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.resources;

import java.util.HashSet;
import java.util.Set;

import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.resource.AbstractIPResource;
import com.foilen.infra.plugin.v1.model.resource.InfraPluginResourceCategory;
import com.google.common.collect.ComparisonChain;

/**
 * This is for any application/service that is installed on a machine. <br/>
 * Links to:
 * <ul>
 * <li>{@link UnixUser}: (optional / 1) RUN_AS - The user that executes that application. Will update the "runAs" of the Application itself and the "runAs" of all the services that are "null"</li>
 * <li>{@link Machine}: (optional / many) INSTALLED_ON - The machines where to install that application</li>
 * </ul>
 *
 * Manages:
 * <ul>
 * <li>{@link DnsPointer}: (optional / many) POINTS_TO - Some domain names that will automatically point to the {@link Machine}s on which it is INSTALLED_ON</li>
 * </ul>
 */
public class Application extends AbstractIPResource implements Comparable<Application> {

    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_DESCRIPTION = "description";
    public static final String PROPERTY_APPLICATION_DEFINITION = "applicationDefinition";
    public static final String PROPERTY_DOMAIN_NAMES = "domainNames";

    // Application
    private String name;
    private String description;

    // Details
    private IPApplicationDefinition applicationDefinition = new IPApplicationDefinition();

    // Network
    private Set<String> domainNames = new HashSet<>();

    public Application() {
    }

    @Override
    public int compareTo(Application o) {
        return ComparisonChain.start() //
                .compare(this.name, o.name) //
                .result();
    }

    public IPApplicationDefinition getApplicationDefinition() {
        return applicationDefinition;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getDomainNames() {
        return domainNames;
    }

    public String getName() {
        return name;
    }

    @Override
    public InfraPluginResourceCategory getResourceCategory() {
        return InfraPluginResourceCategory.INFRASTRUCTURE;
    }

    @Override
    public String getResourceDescription() {
        return description;
    }

    @Override
    public String getResourceName() {
        return name;
    }

    public void setApplicationDefinition(IPApplicationDefinition applicationDefinition) {
        this.applicationDefinition = applicationDefinition;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDomainNames(Set<String> domainNames) {
        this.domainNames = domainNames;
    }

    public void setName(String name) {
        this.name = name;
    }

}
