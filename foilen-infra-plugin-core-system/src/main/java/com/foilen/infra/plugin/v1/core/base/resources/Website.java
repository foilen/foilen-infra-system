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

import com.foilen.infra.plugin.v1.model.docker.DockerContainerEndpoints;
import com.foilen.infra.plugin.v1.model.resource.AbstractIPResource;
import com.foilen.infra.plugin.v1.model.resource.InfraPluginResourceCategory;
import com.google.common.base.Joiner;

/**
 * This is a website that points to the application that is serving it. <br/>
 * Links to:
 * <ul>
 * <li>{@link Application}: (required / many) POINTS_TO - The applications that are serving that website.</li>
 * <li>{@link WebsiteCertificate}: (optional / 1) USES - When using HTTPS needs one certificate.</li>
 * </ul>
 *
 * Manages:
 * <ul>
 * <li>{@link DnsPointer}: (optional / many) POINTS_TO - Some domain names that will automatically point to the {@link Machine}s on which it is INSTALLED_ON</li>
 * </ul>
 */
public class Website extends AbstractIPResource {
    // TODO Website - Add an update handler for the Haproxy Application on each machine (port 80 and 443)
    public static final String PROPERTY_DOMAIN_NAMES = "domainNames";
    public static final String PROPERTY_IS_HTTPS = "isHttps";
    public static final String PROPERTY_APPLICATION_ENDPOINT = "applicationEndpoint";

    // Network
    private Set<String> domainNames = new HashSet<>();

    private boolean isHttps;
    private String applicationEndpoint = DockerContainerEndpoints.HTTP_TCP; // Default: HTTP_TCP

    public Website() {
    }

    public String getApplicationEndpoint() {
        return applicationEndpoint;
    }

    public Set<String> getDomainNames() {
        return domainNames;
    }

    @Override
    public InfraPluginResourceCategory getResourceCategory() {
        return InfraPluginResourceCategory.NET;
    }

    @Override
    public String getResourceDescription() {
        return getResourceName();
    }

    @Override
    public String getResourceName() {
        return Joiner.on(", ").join(domainNames);
    }

    public boolean isHttps() {
        return isHttps;
    }

    public void setApplicationEndpoint(String applicationEndpoint) {
        this.applicationEndpoint = applicationEndpoint;
    }

    public void setDomainNames(Set<String> domainNames) {
        this.domainNames = domainNames;
    }

    public void setHttps(boolean isHttps) {
        this.isHttps = isHttps;
    }

}
