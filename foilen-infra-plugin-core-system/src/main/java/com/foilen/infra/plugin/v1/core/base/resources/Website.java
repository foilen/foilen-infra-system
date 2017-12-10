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
import com.google.common.collect.ComparisonChain;

/**
 * This is a website that points to the application that is serving it. <br/>
 * Links to:
 * <ul>
 * <li>{@link Application}: (required / many) POINTS_TO - The applications that are serving that web site. (are the end points ; not where the web site is installed)</li>
 * <li>{@link WebsiteCertificate}: (optional / 1) USES - When using HTTPS needs one certificate.</li>
 * <li>{@link Machine}: (optional / many) INSTALLED_ON - The machines where to install that web site</li>
 * <li>{@link Machine}: (optional / many) INSTALLED_ON_NO_DNS - The machines where to install that application, but won't have a {@link DnsPointer} on them</li>
 * </ul>
 *
 * Manages:
 * <ul>
 * <li>{@link DnsPointer}: (optional / many) POINTS_TO - Some domain names that will automatically point to the {@link Machine}s on which it is INSTALLED_ON</li>
 * </ul>
 */
public class Website extends AbstractIPResource implements Comparable<Website> {

    public static final String LINK_TYPE_INSTALLED_ON_NO_DNS = "INSTALLED_ON_NO_DNS";

    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_DOMAIN_NAMES = "domainNames";
    public static final String PROPERTY_IS_HTTPS = "isHttps";
    public static final String PROPERTY_APPLICATION_ENDPOINT = "applicationEndpoint";

    // Network
    private String name;
    private Set<String> domainNames = new HashSet<>();

    private boolean isHttps;
    private String applicationEndpoint = DockerContainerEndpoints.HTTP_TCP; // Default: HTTP_TCP

    public Website() {
    }

    public Website(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(Website o) {
        return ComparisonChain.start() //
                .compare(this.name, o.name) //
                .result();
    }

    public String getApplicationEndpoint() {
        return applicationEndpoint;
    }

    public Set<String> getDomainNames() {
        return domainNames;
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
        return Joiner.on(", ").join(domainNames);
    }

    @Override
    public String getResourceName() {
        return getName();
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

    public void setName(String name) {
        this.name = name;
    }

}
