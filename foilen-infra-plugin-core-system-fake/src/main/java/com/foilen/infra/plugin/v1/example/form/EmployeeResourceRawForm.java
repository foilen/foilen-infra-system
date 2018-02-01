/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.example.form;

import com.foilen.infra.plugin.v1.core.visual.editor.simpleresourceditor.SimpleResourceEditor;
import com.foilen.infra.plugin.v1.core.visual.editor.simpleresourceditor.SimpleResourceEditorDefinition;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonFormatting;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonValidation;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.MultilineInputTextFieldPageItem;
import com.foilen.infra.plugin.v1.example.resource.EmployeeResource;
import com.foilen.smalltools.tools.DateTools;

public class EmployeeResourceRawForm extends SimpleResourceEditor<EmployeeResource> {

    private static final String FIELD_NAME_COWORKERS = "coworkers";
    private static final String FIELD_NAME_MANAGER = "manager";

    @Override
    protected void getDefinition(SimpleResourceEditorDefinition simpleResourceEditorDefinition) {
        simpleResourceEditorDefinition.addInputText(EmployeeResource.PROPERTY_FIRST_NAME, fieldConfig -> {
            fieldConfig.addFormator(CommonFormatting::trimSpacesAround);
            fieldConfig.addFormator(CommonFormatting::firstLetterOfEachWordCapital);
            fieldConfig.addValidator(CommonValidation::validateNotNullOrEmpty);
        });

        simpleResourceEditorDefinition.addInputText(EmployeeResource.PROPERTY_LAST_NAME, fieldConfig -> {
            fieldConfig.addFormator(CommonFormatting::trimSpacesAround);
            fieldConfig.addFormator(CommonFormatting::firstLetterOfEachWordCapital);
            fieldConfig.addValidator(CommonValidation::validateNotNullOrEmpty);
        });

        simpleResourceEditorDefinition.addInputText(EmployeeResource.PROPERTY_BIRTHDAY, (fieldConfig) -> {
            fieldConfig.setConvertFromString(DateTools::parseDateOnly);
            fieldConfig.setConvertToString(DateTools::formatDateOnly);
            fieldConfig.addFormator(CommonFormatting::trimSpacesAround);
            fieldConfig.addFormator(fieldValue -> {
                if (fieldValue == null) {
                    return null;
                }
                return fieldValue.replaceAll("/", "-");
            });
            fieldConfig.addValidator(CommonValidation::validateNotNullOrEmpty);
            fieldConfig.addValidator(CommonValidation::validateDayFormat);
        });

        simpleResourceEditorDefinition.addMultilineInputText(EmployeeResource.PROPERTY_NOTES, (fieldConfig) -> {
            fieldConfig.setAugmentPageItem((pi) -> ((MultilineInputTextFieldPageItem) pi).setRows(10));
            fieldConfig.addFormator(CommonFormatting::trimSpacesAround);
        });

        simpleResourceEditorDefinition.addListInputText(EmployeeResource.PROPERTY_FOOD_PREFERENCES, (fieldConfig) -> {
            fieldConfig.addFormator(CommonFormatting::trimSpacesAround);
        });

        simpleResourceEditorDefinition.addResource(FIELD_NAME_MANAGER, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class);
        simpleResourceEditorDefinition.addResources(FIELD_NAME_COWORKERS, EmployeeResource.LINK_TYPE_COWORKER, EmployeeResource.class);

    }

    @Override
    public Class<EmployeeResource> getForResourceType() {
        return EmployeeResource.class;
    }

}
