/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.updatehandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.foilen.infra.plugin.v1.core.base.resources.Application;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.UnixUser;
import com.foilen.infra.plugin.v1.core.base.resources.Website;
import com.foilen.infra.plugin.v1.core.base.resources.WebsiteCertificate;
import com.foilen.infra.plugin.v1.core.base.resources.helper.UnixUserAvailableIdHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractCommonMethodUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.eventhandler.CommonMethodUpdateEventHandlerContext;
import com.foilen.infra.plugin.v1.core.exception.IllegalUpdateException;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionAssetsBundle;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionPortRedirect;
import com.foilen.infra.plugin.v1.model.haproxy.HaProxyConfig;
import com.foilen.infra.plugin.v1.model.haproxy.HaProxyConfigPortHttp;
import com.foilen.infra.plugin.v1.model.haproxy.HaProxyConfigPortHttps;
import com.foilen.infra.plugin.v1.model.outputter.haproxy.HaProxyConfigOutput;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.foilen.smalltools.tuple.Tuple2;

public class MachineHaProxyUpdateHandler extends AbstractCommonMethodUpdateEventHandler<Machine> {

    public static final String UNIX_USER_HA_PROXY_NAME = "infra_web";

    @SuppressWarnings("unchecked")
    protected void addWebsiteConfig(Machine machine, String machineName, IPApplicationDefinition applicationDefinition, AtomicInteger nextLocalPort, Website website,
            List<Tuple2<Application, List<Machine>>> pointsToApplicationOnMachines, HaProxyConfigPortHttp configHttp) {

        // If is installed locally, just point to that one ; else, redirect to all other places
        List<String> installedLocallyApplicationNames = pointsToApplicationOnMachines.stream() //
                .filter(it -> it.getB().contains(machine)) //
                .map(it -> it.getA().getName()) //
                .sorted() //
                .distinct() //
                .collect(Collectors.toList());
        if (installedLocallyApplicationNames.isEmpty()) {
            logger.debug("[{}] {} is not installed locally. Will point to the remote endpoints", machineName, website);

            List<Tuple2<String, Integer>> endpointHostPorts = new ArrayList<>();
            for (Tuple2<Application, List<Machine>> applicationMachines : pointsToApplicationOnMachines) {
                String remoteApplicationName = applicationMachines.getA().getName();
                for (Machine remoteMachine : applicationMachines.getB()) {
                    int localPort = nextLocalPort.getAndIncrement();
                    endpointHostPorts.add(new Tuple2<>("127.0.0.1", localPort));
                    applicationDefinition.addPortRedirect(localPort, remoteMachine.getName(), remoteApplicationName, website.getApplicationEndpoint());
                }
            }
            configHttp.addService(website.getDomainNames(), (Tuple2<String, Integer>[]) endpointHostPorts.toArray());
        } else {
            logger.debug("[{}] {} is installed locally. Will point locally only on applications {}", machineName, website, installedLocallyApplicationNames);

            List<Tuple2<String, Integer>> endpointHostPorts = new ArrayList<>();
            for (String installedLocallyApplicationName : installedLocallyApplicationNames) {
                int localPort = nextLocalPort.getAndIncrement();
                endpointHostPorts.add(new Tuple2<>("127.0.0.1", localPort));
                applicationDefinition.addPortRedirect(localPort, IPApplicationDefinitionPortRedirect.LOCAL_MACHINE, installedLocallyApplicationName, website.getApplicationEndpoint());
            }
            configHttp.addService(website.getDomainNames(), endpointHostPorts.toArray(new Tuple2[endpointHostPorts.size()]));
        }
    }

    @Override
    protected void commonHandlerExecute(CommonServicesContext services, ChangesContext changes, CommonMethodUpdateEventHandlerContext<Machine> context) {

        IPResourceService resourceService = services.getResourceService();

        Machine machine = context.getResource();

        // Get all the websites installed on this machine
        List<Website> websites = resourceService.linkFindAllByFromResourceClassAndLinkTypeAndToResource(Website.class, LinkTypeConstants.INSTALLED_ON, machine);
        websites.addAll(resourceService.linkFindAllByFromResourceClassAndLinkTypeAndToResource(Website.class, Website.LINK_TYPE_INSTALLED_ON_NO_DNS, machine));
        websites = websites.stream() //
                .sorted() //
                .distinct() //
                .collect(Collectors.toList());

        // HA Proxy
        String machineName = machine.getName();
        Application haProxyApplication = new Application();
        haProxyApplication.setDescription("Web HA Proxy for " + machineName);
        haProxyApplication.setName("infra_web-" + machineName.replaceAll("\\.", "_"));

        Optional<Application> existingHaProxyApplicationOptional = resourceService.resourceFindByPk(haProxyApplication);
        if (websites.isEmpty()) {
            logger.info("[{}] No websites to install", machineName);
            if (existingHaProxyApplicationOptional.isPresent()) {
                logger.info("[{}] Deleting existing HA Proxy", machineName);
                changes.resourceDelete(existingHaProxyApplicationOptional.get());
            }
        } else {
            logger.info("[{}] There are {} websites", machineName, websites.size());

            UnixUser unixUser = null;
            Application existingHaProxyApplication = null;
            if (existingHaProxyApplicationOptional.isPresent()) {
                logger.debug("[{}] HA Proxy exists. Will see if needs an update", machineName);
                existingHaProxyApplication = existingHaProxyApplicationOptional.get();
                List<UnixUser> unixUsers = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(existingHaProxyApplication, LinkTypeConstants.RUN_AS, UnixUser.class);
                if (unixUsers.size() > 1) {
                    logger.error("[{}] The HA Proxy application has too many unix users: {}", machineName, unixUsers.size());
                    throw new IllegalUpdateException("The HA Proxy application has too many unix users: " + unixUsers.size());
                } else if (unixUsers.size() == 1) {
                    unixUser = unixUsers.get(0);
                }
            }
            if (unixUser == null) {
                Optional<UnixUser> optional = resourceService.resourceFind(resourceService.createResourceQuery(UnixUser.class).propertyEquals(UnixUser.PROPERTY_NAME, UNIX_USER_HA_PROXY_NAME));
                if (optional.isPresent()) {
                    unixUser = optional.get();
                } else {
                    logger.info("[{}] Could not find the unix user {}. Will create it", machineName, UNIX_USER_HA_PROXY_NAME);
                    unixUser = new UnixUser(UnixUserAvailableIdHelper.getNextAvailableId(), UNIX_USER_HA_PROXY_NAME, "/home/" + UNIX_USER_HA_PROXY_NAME, null, null);
                    changes.resourceAdd(unixUser);
                }
            }

            IPApplicationDefinition applicationDefinition = new IPApplicationDefinition();
            haProxyApplication.setApplicationDefinition(applicationDefinition);

            IPApplicationDefinitionAssetsBundle assetsBundle = applicationDefinition.addAssetsBundle();
            HaProxyConfig haProxyConfig = new HaProxyConfig();
            AtomicInteger nextLocalPort = new AtomicInteger(10000);
            for (Website website : websites) {
                logger.info("[{}] Processing {}", machineName, website);

                // Get the endpoints
                List<Application> pointsToApplications = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(website, LinkTypeConstants.POINTS_TO, Application.class);
                List<Tuple2<Application, List<Machine>>> pointsToApplicationOnMachines = pointsToApplications.stream() //
                        .map(app -> new Tuple2<>(app, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(app, LinkTypeConstants.INSTALLED_ON, Machine.class))) //
                        .collect(Collectors.toList());
                if (pointsToApplications.isEmpty()) {
                    logger.debug("[{}] Cannot configure {} since it goes to no application", machineName, website);
                    continue;
                } else {
                    logger.debug("[{}] {} has {} applications that is points to", machineName, website, pointsToApplications.size());
                }
                boolean hasOneEndpoint = false;
                for (Tuple2<Application, List<Machine>> appMachine : pointsToApplicationOnMachines) {
                    hasOneEndpoint |= !appMachine.getB().isEmpty();
                }
                if (!hasOneEndpoint) {
                    logger.debug("[{}] {} has applications that is points to, but none is installed on a machine", machineName, website);
                    continue;
                }

                // Create configuration for http or https
                if (website.isHttps()) {

                    // HTTPS
                    List<WebsiteCertificate> websiteCertificates = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(website, LinkTypeConstants.USES, WebsiteCertificate.class);
                    if (websiteCertificates.size() != 1) {
                        logger.debug("[{}] Cannot configure {} since we are expecting 1 website certificate and got {}", machineName, website, websiteCertificates.size());
                        continue;
                    } else {
                        WebsiteCertificate websiteCertificate = websiteCertificates.get(0);
                        HaProxyConfigPortHttps configHttps = haProxyConfig.addPortHttps(4433, "/certs");

                        // Add certificate
                        StringBuilder certContent = new StringBuilder();
                        if (websiteCertificate.getCertificate() != null) {
                            certContent.append(websiteCertificate.getCertificate());
                        }
                        if (websiteCertificate.getCaCertificate() != null) {
                            certContent.append(websiteCertificate.getCaCertificate());
                        }
                        if (websiteCertificate.getPrivateKey() != null) {
                            certContent.append(websiteCertificate.getPrivateKey());
                        }
                        for (String domainName : website.getDomainNames()) {
                            logger.debug("[{}] Installing certificate for {}", machineName, domainName);
                            assetsBundle.addAssetContent("/certs/" + domainName + ".pem", certContent.toString());
                        }

                        addWebsiteConfig(machine, machineName, applicationDefinition, nextLocalPort, website, pointsToApplicationOnMachines, configHttps);
                    }
                } else {

                    // HTTP
                    HaProxyConfigPortHttp configHttp = haProxyConfig.addPortHttp(8080);
                    addWebsiteConfig(machine, machineName, applicationDefinition, nextLocalPort, website, pointsToApplicationOnMachines, configHttp);

                }
            }

            applicationDefinition.setFrom("foilen/fcloud-docker-haproxy:1.6.3-002");

            applicationDefinition.addPortExposed(80, 8080);
            applicationDefinition.addPortExposed(443, 4433);

            assetsBundle.addAssetContent("/haproxy.cfg", HaProxyConfigOutput.toConfigFile(haProxyConfig));

            applicationDefinition.addService("haproxy", HaProxyConfigOutput.toRun(haProxyConfig, "/haproxy.cfg"));

            applicationDefinition.setRunAs(unixUser.getId());
            applicationDefinition.addContainerUserToChangeId("haproxy", unixUser.getId());

            // Create or update
            if (existingHaProxyApplicationOptional.isPresent()) {
                if (updateResourceIfDifferent(haProxyApplication, existingHaProxyApplication)) {
                    changes.resourceUpdate(existingHaProxyApplication, existingHaProxyApplication);
                }
                haProxyApplication = existingHaProxyApplication;
            } else {
                changes.resourceAdd(haProxyApplication);
            }

            // Apply links
            changes.linkAdd(haProxyApplication, LinkTypeConstants.RUN_AS, unixUser);
            changes.linkAdd(haProxyApplication, LinkTypeConstants.INSTALLED_ON, machine);
        }

    }

    @Override
    public Class<Machine> supportedClass() {
        return Machine.class;
    }

}
