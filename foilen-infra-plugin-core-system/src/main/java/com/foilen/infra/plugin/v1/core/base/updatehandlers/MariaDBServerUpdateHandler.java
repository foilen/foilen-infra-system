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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.foilen.infra.plugin.v1.core.base.resources.Application;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.MariaDBDatabase;
import com.foilen.infra.plugin.v1.core.base.resources.MariaDBServer;
import com.foilen.infra.plugin.v1.core.base.resources.MariaDBUser;
import com.foilen.infra.plugin.v1.core.base.resources.UnixUser;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.mariadb.MysqlManagerConfig;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.mariadb.MysqlManagerConfigAdmin;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.mariadb.MysqlManagerConfigDatabaseGrants;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.mariadb.MysqlManagerConfigGrant;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.mariadb.MysqlManagerConfigPermission;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.mariadb.MysqlManagerConfigUser;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.exception.ProblemException;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionAssetsBundle;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionVolume;
import com.foilen.infra.plugin.v1.model.docker.DockerContainerEndpoints;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.SecureRandomTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.foilen.smalltools.tuple.Tuple3;
import com.google.common.base.Strings;

public class MariaDBServerUpdateHandler extends AbstractUpdateEventHandler<MariaDBServer> {

    @Override
    public void addHandler(CommonServicesContext services, ChangesContext changes, MariaDBServer resource) {
        commonHandler(services, changes, resource);
    }

    @Override
    public void checkAndFix(CommonServicesContext services, ChangesContext changes, MariaDBServer resource) {
        commonHandler(services, changes, resource);
    }

    private void commonHandler(CommonServicesContext services, ChangesContext changes, MariaDBServer mariaDBServer) {

        String serverName = mariaDBServer.getName();
        logger.debug("[{}] Processing", serverName);

        // Create a root password if none is set
        if (Strings.isNullOrEmpty(mariaDBServer.getRootPassword())) {
            mariaDBServer.setRootPassword(SecureRandomTools.randomHexString(25));
            changes.getResourcesToUpdate().add(new Tuple2<>(mariaDBServer.getInternalId(), mariaDBServer));
        }

        // Get the user and machines
        List<UnixUser> unixUsers = services.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(mariaDBServer, LinkTypeConstants.RUN_AS, UnixUser.class);
        List<Machine> machines = services.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(mariaDBServer, LinkTypeConstants.INSTALLED_ON, Machine.class);

        logger.debug("[{}] Running as {} on {}", serverName, unixUsers, machines);

        // Prepare the config
        MysqlManagerConfig mysqlManagerConfig = new MysqlManagerConfig();
        mysqlManagerConfig.setAdmin(new MysqlManagerConfigAdmin("root", mariaDBServer.getRootPassword()));
        mysqlManagerConfig.getUsersToIgnore().add(new MysqlManagerConfigUser("root", "%"));

        Map<String, MysqlManagerConfigPermission> userConfigByName = new HashMap<>();

        services.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(MariaDBDatabase.class, LinkTypeConstants.INSTALLED_ON, mariaDBServer).forEach(mariaDBDatabase -> {
            String databaseName = mariaDBDatabase.getName();
            logger.debug("[{}] Has database {}", serverName, databaseName);
            mysqlManagerConfig.getDatabases().add(databaseName);

            // ADMIN
            services.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(MariaDBUser.class, MariaDBUser.LINK_TYPE_ADMIN, mariaDBDatabase).forEach(mariaDBUser -> {
                logger.debug("[{}] Database {} has user {} as ADMIN", serverName, databaseName, mariaDBUser.getName());
                MysqlManagerConfigPermission userConfig = userConfigByName.get(mariaDBUser.getName());
                if (userConfig == null) {
                    userConfig = new MysqlManagerConfigPermission(mariaDBUser.getName(), "%", mariaDBUser.getPassword());
                    userConfigByName.put(mariaDBUser.getName(), userConfig);
                }
                userConfig.getDatabaseGrants().add(new MysqlManagerConfigDatabaseGrants(databaseName, //
                        MysqlManagerConfigGrant.CREATE, //
                        MysqlManagerConfigGrant.ALTER, //
                        MysqlManagerConfigGrant.DROP //
                ));
            });

            // READ
            services.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(MariaDBUser.class, MariaDBUser.LINK_TYPE_READ, mariaDBDatabase).forEach(mariaDBUser -> {
                logger.debug("[{}] Database {} has user {} as READ", serverName, databaseName, mariaDBUser.getName());
                MysqlManagerConfigPermission userConfig = userConfigByName.get(mariaDBUser.getName());
                if (userConfig == null) {
                    userConfig = new MysqlManagerConfigPermission(mariaDBUser.getName(), "%", mariaDBUser.getPassword());
                    userConfigByName.put(mariaDBUser.getName(), userConfig);
                }
                userConfig.getDatabaseGrants().add(new MysqlManagerConfigDatabaseGrants(databaseName, //
                        MysqlManagerConfigGrant.SELECT //
                ));
            });

            // WRITE
            services.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(MariaDBUser.class, MariaDBUser.LINK_TYPE_WRITE, mariaDBDatabase).forEach(mariaDBUser -> {
                logger.debug("[{}] Database {} has user {} as WRITE", serverName, databaseName, mariaDBUser.getName());
                MysqlManagerConfigPermission userConfig = userConfigByName.get(mariaDBUser.getName());
                if (userConfig == null) {
                    userConfig = new MysqlManagerConfigPermission(mariaDBUser.getName(), "%", mariaDBUser.getPassword());
                    userConfigByName.put(mariaDBUser.getName(), userConfig);
                }
                userConfig.getDatabaseGrants().add(new MysqlManagerConfigDatabaseGrants(databaseName, //
                        MysqlManagerConfigGrant.INSERT, //
                        MysqlManagerConfigGrant.UPDATE, //
                        MysqlManagerConfigGrant.DELETE //
                ));
            });

        });

        // Apply users permissions
        userConfigByName.values().forEach(userConfig -> {
            mysqlManagerConfig.getUsersPermissions().add(userConfig);
        });

        List<IPResource> neededManagedResources = new ArrayList<>();
        if (unixUsers.size() > 1) {
            throw new ProblemException("Cannot run as more than 1 unix user");
        }
        if (machines.size() > 1) {
            throw new ProblemException("Cannot be installed on multiple machines");
        }
        if (unixUsers.size() == 1) {

            UnixUser unixUser = unixUsers.get(0);

            // Application
            Application application = new Application();
            application.setName(serverName);
            application.setDescription(mariaDBServer.getDescription());

            IPApplicationDefinition applicationDefinition = application.getApplicationDefinition();

            applicationDefinition.setFrom("mariadb:10.3.2");
            applicationDefinition.addBuildStepCommand("export TERM=dumb ; " + //
                    "echo \"deb https://dl.bintray.com/foilen/debian stable main\" > /etc/apt/sources.list.d/foilen.list && " + //
                    "apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 379CE192D401AB61 && " + //
                    "apt-get update && " + //
                    "apt-get install -y mysql-manager=1.0.1 && " + //
                    "apt-get clean && rm -rf /var/lib/apt/lists/*");

            applicationDefinition.addService("app", "/mariadb-start.sh");
            IPApplicationDefinitionAssetsBundle assetsBundle = applicationDefinition.addAssetsBundle();
            assetsBundle.addAssetResource("/mariadb-start.sh", "/com/foilen/infra/plugin/v1/core/base/resources/mariadb/mariadb-start.sh");
            assetsBundle.addAssetResource("/mariadb-update-manager.sh", "/com/foilen/infra/plugin/v1/core/base/resources/mariadb/mariadb-update-manager.sh");
            applicationDefinition.addBuildStepCommand("chmod 755 /*.sh");
            applicationDefinition.addContainerUserToChangeId("mysql", unixUser.getId());
            assetsBundle.addAssetResource("/etc/mysql/mysql.conf.d/mysqld.cnf", "/com/foilen/infra/plugin/v1/core/base/resources/mariadb/mysqld.cnf");

            applicationDefinition.addPortEndpoint(3306, DockerContainerEndpoints.MYSQL_TCP);

            applicationDefinition.setRunAs(unixUser.getId());

            // Data folder
            String baseFolder = unixUser.getHomeFolder() + "/mysql/" + serverName;
            applicationDefinition.addVolume(new IPApplicationDefinitionVolume(baseFolder + "/data", "/var/lib/mysql", unixUser.getId(), unixUser.getId(), "770"));

            // Run folder
            applicationDefinition.addVolume(new IPApplicationDefinitionVolume(baseFolder + "/run", "/var/run/mysqld/", unixUser.getId(), unixUser.getId(), "770"));

            // Save the root password
            applicationDefinition.addVolume(new IPApplicationDefinitionVolume(baseFolder + "/config", "/volumes/config/", unixUser.getId(), unixUser.getId(), "770"));
            String newPass = mariaDBServer.getRootPassword();
            assetsBundle.addAssetContent("/newPass", newPass);
            assetsBundle.addAssetContent("/newPass.cnf", "[client]\npassword=" + newPass);

            // Save the database config for the manager
            applicationDefinition.addCopyWhenStartedContent("/manager-config.json", JsonTools.prettyPrint(mysqlManagerConfig));
            applicationDefinition.addExecuteWhenStartedCommand("/mariadb-update-manager.sh");

            // Manage the app
            neededManagedResources.add(application);
            manageNeededResources(services, changes, mariaDBServer, neededManagedResources, Arrays.asList(Application.class));

            // add Machine INSTALLED_ON to mysqlApplicationDefinition (only 0 or 1)
            if (machines.size() == 1) {
                Machine machine = machines.get(0);
                changes.getLinksToAdd().add(new Tuple3<>(application, LinkTypeConstants.INSTALLED_ON, machine));
            }

            // add UnixUser RUN_AS to mysqlApplicationDefinition (only 1)
            changes.getLinksToAdd().add(new Tuple3<>(application, LinkTypeConstants.RUN_AS, unixUser));
        } else {
            manageNeededResources(services, changes, mariaDBServer, neededManagedResources, Arrays.asList(Application.class));
        }
    }

    @Override
    public void deleteHandler(CommonServicesContext services, ChangesContext changes, MariaDBServer resource, List<Tuple3<IPResource, String, IPResource>> previousLinks) {
        detachManagedResources(services, changes, resource, previousLinks);
    }

    @Override
    public Class<MariaDBServer> supportedClass() {
        return MariaDBServer.class;
    }

    @Override
    public void updateHandler(CommonServicesContext services, ChangesContext changes, MariaDBServer previousResource, MariaDBServer newResource) {
        commonHandler(services, changes, newResource);
    }

}
