/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.editors;

import java.util.List;
import java.util.Map;

import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.service.TranslationService;
import com.foilen.infra.plugin.v1.core.visual.PageDefinition;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonFormatting;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonPageItem;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonValidation;
import com.foilen.infra.plugin.v1.core.visual.pageItem.LabelPageItem;
import com.foilen.smalltools.tuple.Tuple2;
import com.google.common.base.Strings;

public class MachineEditor implements ResourceEditor<Machine> {

    public static final String EDITOR_NAME = "Machine";

    @Override
    public void fillResource(CommonServicesContext servicesCtx, ChangesContext changesContext, Map<String, String> validFormValues, Machine resource) {
        if (resource.getName() == null) {
            resource.setName(validFormValues.get(Machine.PROPERTY_NAME));
            resource.setPublicIp(validFormValues.get(Machine.PROPERTY_PUBLIC_IP));
        }
    }

    @Override
    public void formatForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues) {
        CommonFormatting.trimSpacesAround(rawFormValues);
        CommonFormatting.toLowerCase(rawFormValues);
    }

    @Override
    public Class<Machine> getForResourceType() {
        return Machine.class;
    }

    @Override
    public PageDefinition providePageDefinition(CommonServicesContext servicesCtx, Machine resource) {

        TranslationService translationService = servicesCtx.getTranslationService();

        PageDefinition pageDefinition = new PageDefinition(translationService.translate("MachineEditor.title"));

        if (resource == null) {
            CommonPageItem.createInputTextField(servicesCtx, pageDefinition, "MachineEditor.name", Machine.PROPERTY_NAME);
            CommonPageItem.createInputTextField(servicesCtx, pageDefinition, "MachineEditor.publicIp", Machine.PROPERTY_PUBLIC_IP);
        } else {
            pageDefinition.addPageItem(new LabelPageItem().setText(translationService.translate("MachineEditor.nameArg", resource.getName())));
            pageDefinition.addPageItem(new LabelPageItem().setText(translationService.translate("MachineEditor.publicIpArg", resource.getPublicIp())));
        }

        return pageDefinition;

    }

    @Override
    public List<Tuple2<String, String>> validateForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues) {
        List<Tuple2<String, String>> errors = CommonValidation.validateNotNullOrEmpty(rawFormValues, Machine.PROPERTY_NAME);
        errors.addAll(CommonValidation.validateDomainName(rawFormValues, Machine.PROPERTY_NAME));
        if (!Strings.isNullOrEmpty(rawFormValues.get(Machine.PROPERTY_PUBLIC_IP))) {
            errors.addAll(CommonValidation.validateIpAddress(rawFormValues, Machine.PROPERTY_PUBLIC_IP));
        }
        return errors;

    }

}
