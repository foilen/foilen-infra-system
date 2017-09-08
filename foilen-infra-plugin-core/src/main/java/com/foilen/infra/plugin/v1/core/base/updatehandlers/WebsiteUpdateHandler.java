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
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.foilen.infra.plugin.v1.core.base.resources.Application;
import com.foilen.infra.plugin.v1.core.base.resources.DnsPointer;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.Website;
import com.foilen.infra.plugin.v1.core.base.resources.WebsiteCertificate;
import com.foilen.infra.plugin.v1.core.common.CertificateHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.foilen.smalltools.crypt.asymmetric.AsymmetricKeys;
import com.foilen.smalltools.crypt.asymmetric.RSACrypt;
import com.foilen.smalltools.crypt.cert.CertificateDetails;
import com.foilen.smalltools.crypt.cert.RSACertificate;
import com.foilen.smalltools.tools.DateTools;
import com.foilen.smalltools.tuple.Tuple3;

public class WebsiteUpdateHandler extends AbstractUpdateEventHandler<Website> {

    @Override
    public void addHandler(CommonServicesContext services, ChangesContext changes, Website resource) {
        commonHandler(services, changes, resource);
    }

    @Override
    public void checkAndFix(CommonServicesContext services, ChangesContext changes, Website resource) {
        commonHandler(services, changes, resource);
    }

    private void commonHandler(CommonServicesContext services, ChangesContext changes, Website resource) {
        IPResourceService resourceService = services.getResourceService();

        List<IPResource> neededManagedResources = new ArrayList<>();

        // Create and manage : DnsPointer (attach Machines from the Application)
        List<Application> applications = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, LinkTypeConstants.POINTS_TO, Application.class);
        Set<Machine> installOnMachines = new HashSet<>();
        applications.forEach(application -> {
            installOnMachines.addAll(resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(application, LinkTypeConstants.INSTALLED_ON, Machine.class));
        });
        for (String domainName : resource.getDomainNames()) {
            DnsPointer dnsPointer = new DnsPointer(domainName);
            dnsPointer = retrieveOrCreateResource(resourceService, changes, dnsPointer, DnsPointer.class);
            updateLinksOnResource(services, changes, dnsPointer, LinkTypeConstants.POINTS_TO, Machine.class, installOnMachines.stream().collect(Collectors.toList()));

            neededManagedResources.add(dnsPointer);
        }

        // Create and manage : WebsiteCertificate
        if (resource.isHttps()) {
            List<WebsiteCertificate> managedWebsiteCertificates = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, LinkTypeConstants.MANAGES, WebsiteCertificate.class);
            for (String domainName : resource.getDomainNames()) {
                // Find the one already managed
                WebsiteCertificate websiteCertificate = null;
                Optional<WebsiteCertificate> websiteCertificateOptional = managedWebsiteCertificates.stream().filter(it -> it.getDomainNames().contains(domainName)).findAny();
                if (websiteCertificateOptional.isPresent()) {
                    websiteCertificate = websiteCertificateOptional.get();
                } else {
                    // Find that already exists
                    List<WebsiteCertificate> websiteCertificates = resourceService.resourceFindAll(resourceService.createResourceQuery(WebsiteCertificate.class) //
                            .propertyEquals(WebsiteCertificate.PROPERTY_DOMAIN_NAMES, domainName));
                    if (websiteCertificates.isEmpty()) {
                        // Create one
                        websiteCertificate = new WebsiteCertificate();

                        AsymmetricKeys keys = RSACrypt.RSA_CRYPT.generateKeyPair(4096);
                        RSACertificate rsaCertificate = new RSACertificate(keys).selfSign( //
                                new CertificateDetails().setCommonName(domainName) //
                                        .addSanDns(domainName) //
                                        .setEndDate(DateTools.addDate(Calendar.MONTH, 1)));
                        CertificateHelper.toWebsiteCertificate(null, rsaCertificate, websiteCertificate);

                        changes.getResourcesToAdd().add(websiteCertificate);
                    } else {
                        websiteCertificate = websiteCertificates.get(0);
                    }
                }

                neededManagedResources.add(websiteCertificate);
            }
        }

        manageNeededResources(services, changes, resource, neededManagedResources, Arrays.asList(DnsPointer.class, WebsiteCertificate.class));
    }

    @Override
    public void deleteHandler(CommonServicesContext services, ChangesContext changes, Website resource, List<Tuple3<IPResource, String, IPResource>> previousLinks) {
        detachManagedResources(services, changes, resource, previousLinks);
    }

    @Override
    public Class<Website> supportedClass() {
        return Website.class;
    }

    @Override
    public void updateHandler(CommonServicesContext services, ChangesContext changes, Website previousResource, Website newResource) {
        commonHandler(services, changes, newResource);
    }

}
