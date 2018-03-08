/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.editors;

import com.foilen.infra.plugin.v1.core.base.resources.Application;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.Website;
import com.foilen.infra.plugin.v1.core.base.resources.WebsiteCertificate;
import com.foilen.infra.plugin.v1.core.visual.editor.simpleresourceditor.SimpleResourceEditor;
import com.foilen.infra.plugin.v1.core.visual.editor.simpleresourceditor.SimpleResourceEditorDefinition;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonFormatting;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonValidation;
import com.foilen.infra.plugin.v1.model.docker.DockerContainerEndpoints;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;

public class WebsiteEditor extends SimpleResourceEditor<Website> {

    public static final String EDITOR_NAME = "Website";

    @Override
    protected void getDefinition(SimpleResourceEditorDefinition simpleResourceEditorDefinition) {
        simpleResourceEditorDefinition.addInputText(Website.PROPERTY_NAME, fieldConfigConsumer -> {
            fieldConfigConsumer.addFormator(CommonFormatting::toLowerCase);
            fieldConfigConsumer.addFormator(CommonFormatting::trimSpacesAround);
            fieldConfigConsumer.addValidator(CommonValidation::validateNotNullOrEmpty);
            fieldConfigConsumer.addValidator(CommonValidation::validateAlphaNumLower);
        });

        simpleResourceEditorDefinition.addListInputText(Website.PROPERTY_DOMAIN_NAMES, fieldConfigConsumer -> {
            fieldConfigConsumer.addFormator(CommonFormatting::toLowerCase);
            fieldConfigConsumer.addFormator(CommonFormatting::trimSpacesAround);
            fieldConfigConsumer.addValidator(CommonValidation::validateNotNullOrEmpty);
            fieldConfigConsumer.addValidator(CommonValidation::validateDomainName);
        });

        simpleResourceEditorDefinition.addInputText(Website.PROPERTY_IS_HTTPS, fieldConfigConsumer -> {
            fieldConfigConsumer.addFormator(CommonFormatting::toLowerCase);
            fieldConfigConsumer.addFormator(CommonFormatting::trimSpacesAround);
            fieldConfigConsumer.addFormator(value -> value == null ? "false" : value);
        });

        simpleResourceEditorDefinition.addSelectOptionsField(Website.PROPERTY_APPLICATION_ENDPOINT, DockerContainerEndpoints.allValues, fieldConfigConsumer -> {
        });

        simpleResourceEditorDefinition.addResources("applications", LinkTypeConstants.POINTS_TO, Application.class);
        simpleResourceEditorDefinition.addResource("websiteCertificate", LinkTypeConstants.USES, WebsiteCertificate.class);
        simpleResourceEditorDefinition.addResources("machines", LinkTypeConstants.INSTALLED_ON, Machine.class);
        simpleResourceEditorDefinition.addResources("machinesOnNoDns", Website.LINK_TYPE_INSTALLED_ON_NO_DNS, Machine.class);
    }

    @Override
    public Class<Website> getForResourceType() {
        return Website.class;
    }

}
