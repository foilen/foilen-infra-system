/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base;

import java.util.Arrays;
import java.util.Calendar;

import com.foilen.infra.plugin.v1.core.base.editors.DomainEditor;
import com.foilen.infra.plugin.v1.core.base.editors.MachineEditor;
import com.foilen.infra.plugin.v1.core.base.editors.ManualDnsEntryEditor;
import com.foilen.infra.plugin.v1.core.base.editors.ManualWebsiteCertificateEditor;
import com.foilen.infra.plugin.v1.core.base.editors.SelfSignedWebsiteCertificateEditor;
import com.foilen.infra.plugin.v1.core.base.editors.UnixUserEditor;
import com.foilen.infra.plugin.v1.core.base.resources.Application;
import com.foilen.infra.plugin.v1.core.base.resources.DnsEntry;
import com.foilen.infra.plugin.v1.core.base.resources.DnsPointer;
import com.foilen.infra.plugin.v1.core.base.resources.Domain;
import com.foilen.infra.plugin.v1.core.base.resources.InfraConfig;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.MariaDBDatabase;
import com.foilen.infra.plugin.v1.core.base.resources.MariaDBServer;
import com.foilen.infra.plugin.v1.core.base.resources.MariaDBUser;
import com.foilen.infra.plugin.v1.core.base.resources.UnixUser;
import com.foilen.infra.plugin.v1.core.base.resources.UrlRedirection;
import com.foilen.infra.plugin.v1.core.base.resources.Website;
import com.foilen.infra.plugin.v1.core.base.resources.WebsiteCertificate;
import com.foilen.infra.plugin.v1.core.base.resources.helper.UnixUserAvailableIdHelper;
import com.foilen.infra.plugin.v1.core.base.timers.SelfSignedWebsiteCertificateRefreshTimer;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.ApplicationUpdateHandler;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.DnsEntryUpdateHandler;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.DnsPointerUpdateHandler;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.DomainUpdateHandler;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.InfraConfigUpdateHandler;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.MachineUpdateHandler;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.MariaDBServerUpdateHandler;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.UnixUserUpdateHandler;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.UrlRedirectionUpdateHandler;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.WebsiteCertificateUpdateHandler;
import com.foilen.infra.plugin.v1.core.base.updatehandlers.WebsiteUpdateHandler;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionProvider;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionV1;

public class IPCorePluginDefinitionProvider implements IPPluginDefinitionProvider {

    @Override
    public IPPluginDefinitionV1 getIPPluginDefinition() {
        IPPluginDefinitionV1 pluginDefinitionV1 = new IPPluginDefinitionV1("Main", "Core", "Main components that are always available", "1.0.0");

        // Resources
        pluginDefinitionV1.addCustomResource(Application.class, "Application", //
                Arrays.asList(Application.PROPERTY_NAME), //
                Arrays.asList( //
                        Application.PROPERTY_NAME, //
                        Application.PROPERTY_DOMAIN_NAMES //
                ));

        pluginDefinitionV1.addCustomResource(DnsEntry.class, "Dns Entry", //
                Arrays.asList( //
                        DnsEntry.PROPERTY_NAME, //
                        DnsEntry.PROPERTY_TYPE, //
                        DnsEntry.PROPERTY_DETAILS //
                ), //
                Arrays.asList( //
                        DnsEntry.PROPERTY_NAME, //
                        DnsEntry.PROPERTY_TYPE, //
                        DnsEntry.PROPERTY_DETAILS //
                ));

        pluginDefinitionV1.addCustomResource(DnsPointer.class, "Dns Pointer", //
                Arrays.asList( //
                        DnsPointer.PROPERTY_NAME //
                ), //
                Arrays.asList( //
                        DnsPointer.PROPERTY_NAME //
                ));

        pluginDefinitionV1.addCustomResource(Domain.class, "Domain", //
                Arrays.asList(Domain.PROPERTY_NAME), //
                Arrays.asList( //
                        Domain.PROPERTY_NAME, //
                        Domain.PROPERTY_REVERSE_NAME //
                ));

        pluginDefinitionV1.addCustomResource(Machine.class, "Machine", //
                Arrays.asList(Machine.PROPERTY_NAME), //
                Arrays.asList( //
                        Machine.PROPERTY_NAME, //
                        Machine.PROPERTY_PUBLIC_IP //
                ));

        pluginDefinitionV1.addCustomResource(UnixUser.class, "Unix User", //
                Arrays.asList(UnixUser.PROPERTY_ID), //
                Arrays.asList( //
                        UnixUser.PROPERTY_NAME, //
                        UnixUser.PROPERTY_HOME_FOLDER, //
                        UnixUser.PROPERTY_SHELL //
                ));

        pluginDefinitionV1.addCustomResource(UrlRedirection.class, "Url Redirection", //
                Arrays.asList( //
                        UrlRedirection.PROPERTY_DOMAIN_NAME //
                ), //
                Arrays.asList( //
                        UrlRedirection.PROPERTY_DOMAIN_NAME, //
                        UrlRedirection.PROPERTY_HTTP_REDIRECT_TO_URL, //
                        UrlRedirection.PROPERTY_HTTPS_REDIRECT_TO_URL //
                ));

        pluginDefinitionV1.addCustomResource(Website.class, "Website", //
                Arrays.asList( //
                        Website.PROPERTY_DOMAIN_NAMES //
                ), //
                Arrays.asList( //
                        Website.PROPERTY_DOMAIN_NAMES //
                ));

        pluginDefinitionV1.addCustomResource(WebsiteCertificate.class, "Website Certificate", //
                Arrays.asList(WebsiteCertificate.PROPERTY_THUMBPRINT), //
                Arrays.asList( //
                        WebsiteCertificate.PROPERTY_THUMBPRINT, //
                        WebsiteCertificate.PROPERTY_DOMAIN_NAMES, //
                        WebsiteCertificate.PROPERTY_CA_CERTIFICATE, //
                        WebsiteCertificate.PROPERTY_START, //
                        WebsiteCertificate.PROPERTY_END //
                ));

        pluginDefinitionV1.addCustomResource(MariaDBServer.class, "MariaDB Server", //
                Arrays.asList(MariaDBServer.PROPERTY_NAME), //
                Arrays.asList( //
                        MariaDBServer.PROPERTY_NAME, //
                        MariaDBServer.PROPERTY_DESCRIPTION //
                ));

        pluginDefinitionV1.addCustomResource(MariaDBDatabase.class, "MariaDB Database", //
                Arrays.asList(MariaDBDatabase.PROPERTY_NAME), //
                Arrays.asList( //
                        MariaDBDatabase.PROPERTY_NAME, //
                        MariaDBDatabase.PROPERTY_DESCRIPTION //
                ));

        pluginDefinitionV1.addCustomResource(MariaDBUser.class, "MariaDB User", //
                Arrays.asList(MariaDBUser.PROPERTY_NAME), //
                Arrays.asList( //
                        MariaDBUser.PROPERTY_NAME, //
                        MariaDBUser.PROPERTY_DESCRIPTION //
                ));

        pluginDefinitionV1.addCustomResource(InfraConfig.class, "Infrastructure Configuration", //
                Arrays.asList(), //
                Arrays.asList());

        // Resource editors
        pluginDefinitionV1.addTranslations("/com/foilen/infra/plugin/v1/core/base/messages");
        pluginDefinitionV1.addResourceEditor(new DomainEditor(), DomainEditor.EDITOR_NAME);
        pluginDefinitionV1.addResourceEditor(new MachineEditor(), MachineEditor.EDITOR_NAME);
        pluginDefinitionV1.addResourceEditor(new ManualDnsEntryEditor(), ManualDnsEntryEditor.EDITOR_NAME);
        pluginDefinitionV1.addResourceEditor(new ManualWebsiteCertificateEditor(), ManualWebsiteCertificateEditor.EDITOR_NAME);
        pluginDefinitionV1.addResourceEditor(new SelfSignedWebsiteCertificateEditor(), SelfSignedWebsiteCertificateEditor.EDITOR_NAME);
        pluginDefinitionV1.addResourceEditor(new UnixUserEditor(), UnixUserEditor.EDITOR_NAME);

        // Update events
        pluginDefinitionV1.addUpdateHandler(new ApplicationUpdateHandler());
        pluginDefinitionV1.addUpdateHandler(new DnsEntryUpdateHandler());
        pluginDefinitionV1.addUpdateHandler(new DnsPointerUpdateHandler());
        pluginDefinitionV1.addUpdateHandler(new DomainUpdateHandler());
        pluginDefinitionV1.addUpdateHandler(new MachineUpdateHandler());
        pluginDefinitionV1.addUpdateHandler(new UnixUserUpdateHandler());
        pluginDefinitionV1.addUpdateHandler(new UrlRedirectionUpdateHandler());
        pluginDefinitionV1.addUpdateHandler(new WebsiteUpdateHandler());
        pluginDefinitionV1.addUpdateHandler(new WebsiteCertificateUpdateHandler());
        pluginDefinitionV1.addUpdateHandler(new MariaDBServerUpdateHandler());
        pluginDefinitionV1.addUpdateHandler(new InfraConfigUpdateHandler());

        // Timers
        pluginDefinitionV1.addTimer(new SelfSignedWebsiteCertificateRefreshTimer(), //
                SelfSignedWebsiteCertificateRefreshTimer.TIMER_NAME, //
                Calendar.DAY_OF_YEAR, 1, //
                false, true);

        return pluginDefinitionV1;
    }

    @Override
    public void initialize(CommonServicesContext commonServicesContext) {
        UnixUserAvailableIdHelper.init(commonServicesContext.getResourceService());
    }

}
