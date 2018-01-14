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

import com.foilen.infra.plugin.v1.core.base.resources.Application;
import com.foilen.infra.plugin.v1.core.base.resources.InfraConfig;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.MariaDBDatabase;
import com.foilen.infra.plugin.v1.core.base.resources.MariaDBServer;
import com.foilen.infra.plugin.v1.core.base.resources.MariaDBUser;
import com.foilen.infra.plugin.v1.core.base.resources.UnixUser;
import com.foilen.infra.plugin.v1.core.base.resources.Website;
import com.foilen.infra.plugin.v1.core.base.resources.WebsiteCertificate;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractCommonMethodUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.eventhandler.CommonMethodUpdateEventHandlerContext;
import com.foilen.infra.plugin.v1.core.exception.IllegalUpdateException;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionAssetsBundle;
import com.foilen.infra.plugin.v1.model.docker.DockerContainerEndpoints;
import com.foilen.infra.plugin.v1.model.infra.InfraLoginConfig;
import com.foilen.infra.plugin.v1.model.infra.InfraLoginConfigDetails;
import com.foilen.infra.plugin.v1.model.infra.InfraUiConfig;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.foilen.smalltools.tools.CollectionsTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.SecureRandomTools;
import com.google.common.base.Strings;

public class InfraConfigUpdateHandler extends AbstractCommonMethodUpdateEventHandler<InfraConfig> {

    @Override
    protected void commonHandlerExecute(CommonServicesContext services, ChangesContext changes, CommonMethodUpdateEventHandlerContext<InfraConfig> context) {

        context.setManagedResourcesUpdateContentIfExists(true);

        context.getManagedResourceTypes().add(Application.class);
        context.getManagedResourceTypes().add(Website.class);

        InfraConfig infraConfig = context.getResource();

        // Check that there is only a single instance of this resource
        IPResourceService resourceService = services.getResourceService();
        List<InfraConfig> infraConfigs = resourceService.resourceFindAll(resourceService.createResourceQuery(InfraConfig.class));
        if (infraConfigs.size() > 1) {
            throw new IllegalUpdateException("It is not possible to have more than 1 InfraConfig resource");
        }
        if (infraConfigs.size() == 1 && (infraConfig.getInternalId() == null || !infraConfigs.get(0).getInternalId().equals(infraConfig.getInternalId()))) {
            logger.info("There is already 1 InfraConfig resource and it is not possible to have more than 1. Current existing id {} and the id of the one to update {}",
                    infraConfigs.get(0).getInternalId(), infraConfig.getInternalId());
            throw new IllegalUpdateException("There is already 1 InfraConfig resource and it is not possible to have more than 1");
        }

        // Create missing values
        boolean infraConfigNeedsUpdate = false;
        if (Strings.isNullOrEmpty(infraConfig.getApplicationId())) {
            infraConfig.setApplicationId(SecureRandomTools.randomHexString(25));
            infraConfigNeedsUpdate = true;
        }
        if (Strings.isNullOrEmpty(infraConfig.getLoginCookieSignatureSalt())) {
            infraConfig.setLoginCookieSignatureSalt(SecureRandomTools.randomHexString(25));
            infraConfigNeedsUpdate = true;
        }
        if (Strings.isNullOrEmpty(infraConfig.getLoginCsrfSalt())) {
            infraConfig.setLoginCsrfSalt(SecureRandomTools.randomHexString(25));
            infraConfigNeedsUpdate = true;
        }
        if (Strings.isNullOrEmpty(infraConfig.getUiCsrfSalt())) {
            infraConfig.setUiCsrfSalt(SecureRandomTools.randomHexString(25));
            infraConfigNeedsUpdate = true;
        }
        if (Strings.isNullOrEmpty(infraConfig.getUiLoginCookieSignatureSalt())) {
            infraConfig.setUiLoginCookieSignatureSalt(SecureRandomTools.randomHexString(25));
            infraConfigNeedsUpdate = true;
        }

        // Get the user and machines
        List<WebsiteCertificate> loginWebsiteCertificates = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(infraConfig, InfraConfig.LINK_TYPE_LOGIN_USES,
                WebsiteCertificate.class);
        List<MariaDBServer> loginMariaDBServers = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(infraConfig, InfraConfig.LINK_TYPE_LOGIN_USES, MariaDBServer.class);
        List<MariaDBDatabase> loginMariaDBDatabases = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(infraConfig, InfraConfig.LINK_TYPE_LOGIN_USES, MariaDBDatabase.class);
        List<MariaDBUser> loginMariaDBUsers = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(infraConfig, InfraConfig.LINK_TYPE_LOGIN_USES, MariaDBUser.class);
        List<UnixUser> loginUnixUsers = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(infraConfig, InfraConfig.LINK_TYPE_LOGIN_USES, UnixUser.class);
        List<Machine> loginMachines = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(infraConfig, InfraConfig.LINK_TYPE_LOGIN_INSTALLED_ON, Machine.class);

        List<WebsiteCertificate> uiWebsiteCertificates = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(infraConfig, InfraConfig.LINK_TYPE_UI_USES, WebsiteCertificate.class);
        List<MariaDBServer> uiMariaDBServers = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(infraConfig, InfraConfig.LINK_TYPE_UI_USES, MariaDBServer.class);
        List<MariaDBDatabase> uiMariaDBDatabases = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(infraConfig, InfraConfig.LINK_TYPE_UI_USES, MariaDBDatabase.class);
        List<MariaDBUser> uiMariaDBUsers = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(infraConfig, InfraConfig.LINK_TYPE_UI_USES, MariaDBUser.class);
        List<UnixUser> uiUnixUsers = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(infraConfig, InfraConfig.LINK_TYPE_UI_USES, UnixUser.class);
        List<Machine> uiMachines = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(infraConfig, InfraConfig.LINK_TYPE_UI_INSTALLED_ON, Machine.class);

        validateResourcesToUse(resourceService, loginWebsiteCertificates, loginMariaDBServers, loginMariaDBDatabases, loginMariaDBUsers, loginUnixUsers, loginMachines);
        validateResourcesToUse(resourceService, uiWebsiteCertificates, uiMariaDBServers, uiMariaDBDatabases, uiMariaDBUsers, uiUnixUsers, uiMachines);

        // Create the Applications and Websites if everything is available
        if (hasAllPropertiesSet(infraConfig, //
                loginWebsiteCertificates, loginMariaDBServers, loginMariaDBDatabases, loginMariaDBUsers, loginUnixUsers, loginMachines, //
                uiWebsiteCertificates, uiMariaDBServers, uiMariaDBDatabases, uiMariaDBUsers, uiUnixUsers, uiMachines)) {

            logger.info("Will create the applications");

            // Prepare the login config
            MariaDBServer loginMariaDBServer = loginMariaDBServers.get(0);
            List<Machine> loginMariaDBServerMachines = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(loginMariaDBServer, LinkTypeConstants.INSTALLED_ON, Machine.class);
            if (loginMariaDBServerMachines.isEmpty()) {
                logger.info("Login Maria DB is not installed on a machine");
            } else {
                String loginMariaDBServerMachine = loginMariaDBServerMachines.get(0).getName();
                MariaDBDatabase loginMariaDBDatabase = loginMariaDBDatabases.get(0);
                MariaDBUser loginMariaDBUser = loginMariaDBUsers.get(0);
                UnixUser loginUnixUser = loginUnixUsers.get(0);

                InfraLoginConfig infraLoginConfig = new InfraLoginConfig();

                infraLoginConfig.setAdministratorEmail(infraConfig.getLoginAdministratorEmail());
                infraLoginConfig.setApplicationId(infraConfig.getApplicationId());

                infraLoginConfig.setCookieDateName("login_date");
                infraLoginConfig.setCookieSignatureName("login_signature");
                infraLoginConfig.setCookieSignatureSalt(infraConfig.getLoginCookieSignatureSalt());
                infraLoginConfig.setCookieUserName("login_username");

                infraLoginConfig.setCsrfSalt(infraConfig.getLoginCsrfSalt());

                infraLoginConfig.setFromEmail(infraConfig.getLoginEmailFrom());

                boolean loginIsHttps = !loginWebsiteCertificates.isEmpty();
                infraLoginConfig.setLoginBaseUrl((loginIsHttps ? "https://" : "http://") + infraConfig.getLoginDomainName());

                infraLoginConfig.setMysqlHostName("127.0.0.1");
                infraLoginConfig.setMysqlPort(3306);
                infraLoginConfig.setMysqlDatabaseName(loginMariaDBDatabase.getName());
                infraLoginConfig.setMysqlDatabaseUserName(loginMariaDBUser.getName());
                infraLoginConfig.setMysqlDatabasePassword(loginMariaDBUser.getPassword());

                // Login Application
                Application loginApplication = new Application();
                loginApplication.setName("infra_login");
                loginApplication.setDescription("Login service");

                IPApplicationDefinition loginApplicationDefinition = loginApplication.getApplicationDefinition();

                loginApplicationDefinition.setFrom("foilen-login:0.2.1");

                loginApplicationDefinition.getEnvironments().put("CONFIG_FILE", "/login_config.json");

                IPApplicationDefinitionAssetsBundle loginAssetsBundle = loginApplicationDefinition.addAssetsBundle();
                loginAssetsBundle.addAssetContent("/login_config.json", JsonTools.prettyPrint(infraLoginConfig));

                loginApplicationDefinition.addPortRedirect(3306, loginMariaDBServerMachine, loginMariaDBServer.getName(), DockerContainerEndpoints.MYSQL_TCP);
                loginApplicationDefinition.addPortEndpoint(14010, DockerContainerEndpoints.HTTP_TCP);

                loginApplicationDefinition.setRunAs(loginUnixUser.getId());
                loginApplicationDefinition.setWorkingDirectory("/app");
                loginApplicationDefinition.setCommand("java -jar foilen-login.jar");

                context.getManagedResources().add(loginApplication);

                for (Machine machine : loginMachines) {
                    changes.linkAdd(loginApplication, LinkTypeConstants.INSTALLED_ON, machine);
                }
                changes.linkAdd(loginApplication, LinkTypeConstants.RUN_AS, loginUnixUser);

                // Login Website
                Website loginWebsite = new Website("infra_login");
                loginWebsite.setApplicationEndpoint(DockerContainerEndpoints.HTTP_TCP);
                loginWebsite.getDomainNames().add(infraConfig.getLoginDomainName());
                loginWebsite.setHttps(loginIsHttps);
                context.getManagedResources().add(loginWebsite);
                changes.linkAdd(loginWebsite, LinkTypeConstants.POINTS_TO, loginApplication);
                if (loginIsHttps) {
                    changes.linkAdd(loginWebsite, LinkTypeConstants.USES, loginWebsiteCertificates.get(0));
                }
                for (Machine loginMachine : loginMachines) {
                    changes.linkAdd(loginWebsite, LinkTypeConstants.INSTALLED_ON, loginMachine);
                }

                // Prepare the UI config
                MariaDBServer uiMariaDBServer = uiMariaDBServers.get(0);
                String uiMariaDBServerMachine = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(uiMariaDBServer, LinkTypeConstants.INSTALLED_ON, Machine.class).get(0).getName();
                MariaDBDatabase uiMariaDBDatabase = uiMariaDBDatabases.get(0);
                MariaDBUser uiMariaDBUser = uiMariaDBUsers.get(0);
                UnixUser uiUnixUser = uiUnixUsers.get(0);

                InfraUiConfig infraUiConfig = new InfraUiConfig();

                boolean uiIsHttps = !uiWebsiteCertificates.isEmpty();
                infraUiConfig.setBaseUrl((uiIsHttps ? "https://" : "http://") + infraConfig.getUiDomainName());

                infraUiConfig.setCsrfSalt(infraConfig.getUiCsrfSalt());
                infraUiConfig.setMysqlHostName("127.0.0.1");
                infraUiConfig.setMysqlPort(3306);
                infraUiConfig.setMysqlDatabaseName(uiMariaDBDatabase.getName());
                infraUiConfig.setMysqlDatabaseUserName(uiMariaDBUser.getName());
                infraUiConfig.setMysqlDatabasePassword(uiMariaDBUser.getPassword());

                infraUiConfig.setMailHost("127.0.0.1");
                infraUiConfig.setMailPort(25);

                infraUiConfig.setMailFrom(infraConfig.getUiEmailFrom());
                infraUiConfig.setMailAlertsTo(infraConfig.getUiAlertsToEmail());

                infraUiConfig.setLoginCookieSignatureSalt(infraConfig.getUiLoginCookieSignatureSalt());

                InfraLoginConfigDetails loginConfigDetails = infraUiConfig.getLoginConfigDetails();
                loginConfigDetails.setAppId(infraConfig.getApplicationId());
                loginConfigDetails.setBaseUrl(infraLoginConfig.getLoginBaseUrl());
                if (loginIsHttps) {
                    loginConfigDetails.setCertText(loginWebsiteCertificates.get(0).getCertificate());
                }

                // UI Application
                Application uiApplication = new Application();
                uiApplication.setName("infra_ui");
                uiApplication.setDescription("UI service");

                IPApplicationDefinition uiApplicationDefinition = uiApplication.getApplicationDefinition();

                uiApplicationDefinition.setFrom("foilen-infra-ui:master-SNAPSHOT");

                uiApplicationDefinition.getEnvironments().put("CONFIG_FILE", "/ui_config.json");

                IPApplicationDefinitionAssetsBundle uiAssetsBundle = uiApplicationDefinition.addAssetsBundle();
                uiAssetsBundle.addAssetContent("/ui_config.json", JsonTools.prettyPrint(infraUiConfig));

                uiApplicationDefinition.addPortRedirect(3306, uiMariaDBServerMachine, uiMariaDBServer.getName(), DockerContainerEndpoints.MYSQL_TCP);
                uiApplicationDefinition.addPortEndpoint(8080, DockerContainerEndpoints.HTTP_TCP);

                uiApplicationDefinition.setRunAs(uiUnixUser.getId());
                uiApplicationDefinition.setEntrypoint(new ArrayList<>());
                uiApplicationDefinition.setCommand("java -jar /app/foilen-infra-ui.jar");

                context.getManagedResources().add(uiApplication);

                for (Machine machine : uiMachines) {
                    changes.linkAdd(uiApplication, LinkTypeConstants.INSTALLED_ON, machine);
                }
                changes.linkAdd(uiApplication, LinkTypeConstants.RUN_AS, uiUnixUser);

                // UI Website
                Website uiWebsite = new Website("infra_ui");
                uiWebsite.setApplicationEndpoint(DockerContainerEndpoints.HTTP_TCP);
                uiWebsite.getDomainNames().add(infraConfig.getUiDomainName());
                uiWebsite.setHttps(uiIsHttps);
                context.getManagedResources().add(uiWebsite);
                changes.linkAdd(uiWebsite, LinkTypeConstants.POINTS_TO, uiApplication);
                if (uiIsHttps) {
                    changes.linkAdd(uiWebsite, LinkTypeConstants.USES, uiWebsiteCertificates.get(0));
                }
                for (Machine uiMachine : uiMachines) {
                    changes.linkAdd(uiWebsite, LinkTypeConstants.INSTALLED_ON, uiMachine);
                }

            }

        } else {
            logger.info("Missing some parameters. Will not create the applications");
        }

        if (infraConfigNeedsUpdate) {
            changes.resourceUpdate(infraConfig.getInternalId(), infraConfig);
        }
    }

    private boolean hasAllPropertiesSet(InfraConfig infraConfig, //
            List<WebsiteCertificate> loginWebsiteCertificates, List<MariaDBServer> loginMariaDBServers, List<MariaDBDatabase> loginMariaDBDatabases, List<MariaDBUser> loginMariaDBUsers,
            List<UnixUser> loginUnixUsers, List<Machine> loginMachines, //
            List<WebsiteCertificate> uiWebsiteCertificates, List<MariaDBServer> uiMariaDBServers, List<MariaDBDatabase> uiMariaDBDatabases, List<MariaDBUser> uiMariaDBUsers,
            List<UnixUser> uiUnixUsers, List<Machine> uiMachines) {

        boolean hasAllPropertiesSet = true;
        hasAllPropertiesSet &= CollectionsTools.isAllItemNotNullOrEmpty( //
                infraConfig.getApplicationId(), //
                infraConfig.getLoginAdministratorEmail(), //
                infraConfig.getLoginCookieSignatureSalt(), //
                infraConfig.getLoginCsrfSalt(), //
                infraConfig.getLoginDomainName(), //
                infraConfig.getLoginEmailFrom(), //
                infraConfig.getUiAlertsToEmail(), //
                infraConfig.getUiCsrfSalt(), //
                infraConfig.getUiDomainName(), //
                infraConfig.getUiEmailFrom());

        hasAllPropertiesSet &= !loginMariaDBServers.isEmpty();
        hasAllPropertiesSet &= !loginMariaDBDatabases.isEmpty();
        hasAllPropertiesSet &= !loginMariaDBUsers.isEmpty();
        hasAllPropertiesSet &= !loginUnixUsers.isEmpty();
        hasAllPropertiesSet &= !loginMachines.isEmpty();

        hasAllPropertiesSet &= !uiMariaDBServers.isEmpty();
        hasAllPropertiesSet &= !uiMariaDBDatabases.isEmpty();
        hasAllPropertiesSet &= !uiMariaDBUsers.isEmpty();
        hasAllPropertiesSet &= !uiUnixUsers.isEmpty();
        hasAllPropertiesSet &= !uiMachines.isEmpty();

        return hasAllPropertiesSet;
    }

    @Override
    public Class<InfraConfig> supportedClass() {
        return InfraConfig.class;
    }

    private void validateResourcesToUse(IPResourceService resourceService, List<WebsiteCertificate> websiteCertificates, List<MariaDBServer> mariaDBServers, List<MariaDBDatabase> mariaDBDatabases,
            List<MariaDBUser> mariaDBUsers, List<UnixUser> unixUsers, List<Machine> machines) {

        // Check the amounts
        if (websiteCertificates.size() > 1) {
            throw new IllegalUpdateException("Can only use a single certificate");
        }
        if (mariaDBServers.size() > 1) {
            throw new IllegalUpdateException("Can only use a single database server");
        }
        if (mariaDBDatabases.size() > 1) {
            throw new IllegalUpdateException("Can only use a single database");
        }
        if (mariaDBUsers.size() > 1) {
            throw new IllegalUpdateException("Can only use a single database user");
        }
        if (unixUsers.size() > 1) {
            throw new IllegalUpdateException("Can only use a single unix user");
        }

        // MariaDB resources are linked together
        if (!mariaDBServers.isEmpty() && !mariaDBDatabases.isEmpty() && !mariaDBUsers.isEmpty()) {
            MariaDBServer mariaDBServer = mariaDBServers.get(0);
            MariaDBDatabase mariaDBDatabase = mariaDBDatabases.get(0);
            MariaDBUser mariaDBUser = mariaDBUsers.get(0);
            if (!resourceService.linkExistsByFromResourceAndLinkTypeAndToResource(mariaDBDatabase, LinkTypeConstants.INSTALLED_ON, mariaDBServer)) {
                throw new IllegalUpdateException("The database is not installed on the database server");
            }
            if (!resourceService.linkExistsByFromResourceAndLinkTypeAndToResource(mariaDBUser, MariaDBUser.LINK_TYPE_ADMIN, mariaDBDatabase)) {
                throw new IllegalUpdateException("The database user is not an ADMIN on the database");
            }
            if (!resourceService.linkExistsByFromResourceAndLinkTypeAndToResource(mariaDBUser, MariaDBUser.LINK_TYPE_READ, mariaDBDatabase)) {
                throw new IllegalUpdateException("The database user is not a READER on the database");
            }
            if (!resourceService.linkExistsByFromResourceAndLinkTypeAndToResource(mariaDBUser, MariaDBUser.LINK_TYPE_WRITE, mariaDBDatabase)) {
                throw new IllegalUpdateException("The database user is not a WRITER on the database");
            }
        }

    }

}
