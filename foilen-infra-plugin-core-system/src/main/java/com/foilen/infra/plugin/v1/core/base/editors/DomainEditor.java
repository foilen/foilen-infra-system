/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.editors;

import java.util.List;
import java.util.Map;

import com.foilen.infra.plugin.v1.core.base.resources.Domain;
import com.foilen.infra.plugin.v1.core.common.DomainHelper;
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

public class DomainEditor implements ResourceEditor<Domain> {

    public static final String EDITOR_NAME = "Domain";

    private static final String FIELD_NAME_NAME = "name";

    @Override
    public void fillResource(CommonServicesContext servicesCtx, ChangesContext changesContext, Map<String, String> validFormValues, Domain resource) {
        String name = validFormValues.get(FIELD_NAME_NAME);
        if (resource.getName() == null) {
            resource.setName(name);
            resource.setReverseName(DomainHelper.reverseDomainName(name));
        }
    }

    @Override
    public void formatForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues) {
        CommonFormatting.trimSpacesAround(rawFormValues);
        CommonFormatting.toLowerCase(rawFormValues);
    }

    @Override
    public Class<Domain> getForResourceType() {
        return Domain.class;
    }

    @Override
    public PageDefinition providePageDefinition(CommonServicesContext servicesCtx, Domain resource) {

        TranslationService translationService = servicesCtx.getTranslationService();

        PageDefinition pageDefinition = new PageDefinition(translationService.translate("DomainEditor.title"));

        if (resource == null) {
            CommonPageItem.createInputTextField(servicesCtx, pageDefinition, "DomainEditor.name", FIELD_NAME_NAME);
        } else {

            pageDefinition.addPageItem(new LabelPageItem().setText( //
                    translationService.translate("DomainEditor.nameArg", resource.getName()) //
            ));
            pageDefinition.addPageItem(new LabelPageItem().setText( //
                    translationService.translate("DomainEditor.reverseName", resource.getReverseName()) //
            ));

        }

        return pageDefinition;

    }

    @Override
    public List<Tuple2<String, String>> validateForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues) {
        List<Tuple2<String, String>> errors = CommonValidation.validateNotNullOrEmpty(rawFormValues, FIELD_NAME_NAME);
        errors.addAll(CommonValidation.validateDomainName(rawFormValues, FIELD_NAME_NAME));
        return errors;

    }

}
