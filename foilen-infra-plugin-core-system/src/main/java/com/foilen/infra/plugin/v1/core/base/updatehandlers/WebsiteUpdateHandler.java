/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.updatehandlers;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.foilen.infra.plugin.v1.core.base.resources.DnsPointer;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.Website;
import com.foilen.infra.plugin.v1.core.base.resources.WebsiteCertificate;
import com.foilen.infra.plugin.v1.core.common.CertificateHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractCommonMethodUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.eventhandler.CommonMethodUpdateEventHandlerContext;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.foilen.smalltools.crypt.spongycastle.asymmetric.AsymmetricKeys;
import com.foilen.smalltools.crypt.spongycastle.asymmetric.RSACrypt;
import com.foilen.smalltools.crypt.spongycastle.cert.CertificateDetails;
import com.foilen.smalltools.crypt.spongycastle.cert.RSACertificate;
import com.foilen.smalltools.tools.DateTools;

public class WebsiteUpdateHandler extends AbstractCommonMethodUpdateEventHandler<Website> {

    @Override
    protected void commonHandlerExecute(CommonServicesContext services, ChangesContext changes, CommonMethodUpdateEventHandlerContext<Website> context) {

        context.getManagedResourceTypes().add(DnsPointer.class);
        context.getManagedResourceTypes().add(WebsiteCertificate.class);

        IPResourceService resourceService = services.getResourceService();

        Website resource = context.getResource();

        // Create and manage : DnsPointer (attach Machines from the Application)
        List<Machine> installOnMachines = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, LinkTypeConstants.INSTALLED_ON, Machine.class);
        for (String domainName : resource.getDomainNames()) {
            DnsPointer dnsPointer = new DnsPointer(domainName);
            dnsPointer = retrieveOrCreateResource(resourceService, changes, dnsPointer, DnsPointer.class);
            updateLinksOnResource(services, changes, dnsPointer, LinkTypeConstants.POINTS_TO, Machine.class, installOnMachines.stream().collect(Collectors.toList()));

            context.getManagedResources().add(dnsPointer);
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
                            .propertyContains(WebsiteCertificate.PROPERTY_DOMAIN_NAMES, Arrays.asList(domainName)));
                    if (websiteCertificates.isEmpty()) {
                        // Create one
                        websiteCertificate = new WebsiteCertificate();

                        AsymmetricKeys keys = RSACrypt.RSA_CRYPT.generateKeyPair(4096);
                        RSACertificate rsaCertificate = new RSACertificate(keys).selfSign( //
                                new CertificateDetails().setCommonName(domainName) //
                                        .addSanDns(domainName) //
                                        .setEndDate(DateTools.addDate(Calendar.MONTH, 1)));
                        CertificateHelper.toWebsiteCertificate(null, rsaCertificate, websiteCertificate);

                        changes.resourceAdd(websiteCertificate);
                    } else {
                        websiteCertificate = websiteCertificates.get(0);
                    }
                }

                context.getManagedResources().add(websiteCertificate);
            }
        }
    }

    @Override
    public Class<Website> supportedClass() {
        return Website.class;
    }

}
