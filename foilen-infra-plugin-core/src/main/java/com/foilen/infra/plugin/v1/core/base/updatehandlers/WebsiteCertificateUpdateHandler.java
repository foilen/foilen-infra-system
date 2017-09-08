/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.updatehandlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.foilen.infra.plugin.v1.core.base.resources.Domain;
import com.foilen.infra.plugin.v1.core.base.resources.WebsiteCertificate;
import com.foilen.infra.plugin.v1.core.common.DomainHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractUpdateEventHandler;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tuple.Tuple3;

public class WebsiteCertificateUpdateHandler extends AbstractUpdateEventHandler<WebsiteCertificate> {

    @Override
    public void addHandler(CommonServicesContext services, ChangesContext changes, WebsiteCertificate resource) {
        commonHandler(services, changes, resource);
    }

    @Override
    public void checkAndFix(CommonServicesContext services, ChangesContext changes, WebsiteCertificate resource) {
        commonHandler(services, changes, resource);
    }

    private void commonHandler(CommonServicesContext services, ChangesContext changes, WebsiteCertificate resource) {
        List<IPResource> neededManagedResources = new ArrayList<>();

        for (String domainName : resource.getDomainNames()) {
            neededManagedResources.add(new Domain(domainName, DomainHelper.reverseDomainName(domainName)));
        }

        manageNeededResources(services, changes, resource, neededManagedResources, Arrays.asList(Domain.class));
    }

    @Override
    public void deleteHandler(CommonServicesContext services, ChangesContext changes, WebsiteCertificate resource, List<Tuple3<IPResource, String, IPResource>> previousLinks) {
        detachManagedResources(services, changes, resource, previousLinks);
    }

    @Override
    public Class<WebsiteCertificate> supportedClass() {
        return WebsiteCertificate.class;
    }

    @Override
    public void updateHandler(CommonServicesContext services, ChangesContext changes, WebsiteCertificate previousResource, WebsiteCertificate newResource) {
        commonHandler(services, changes, newResource);
    }

}
