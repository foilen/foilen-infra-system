/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.example.form;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.service.TranslationService;
import com.foilen.infra.plugin.v1.core.visual.PageDefinition;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonFieldHelper;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonFormatting;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonPageItem;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonResourceLink;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonValidation;
import com.foilen.infra.plugin.v1.core.visual.pageItem.LabelPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.ListPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.InputTextFieldPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.ListInputTextFieldPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.MultilineInputTextFieldPageItem;
import com.foilen.infra.plugin.v1.example.resource.EmployeeResource;
import com.foilen.smalltools.tools.DateTools;
import com.foilen.smalltools.tuple.Tuple2;

public class EmployeeResourceSimpleForm implements ResourceEditor<EmployeeResource> {

    private static final String FIELD_NAME_FULL_NAME = "fullName";
    private static final String FIELD_NAME_MANAGER_ID = "managerId";

    @Override
    public void fillResource(CommonServicesContext servicesCtx, ChangesContext changesContext, Map<String, String> validFormValues, EmployeeResource resource) {

        String fullName = validFormValues.get(FIELD_NAME_FULL_NAME);
        String[] nameParts = fullName.split(" ");

        resource.setFirstName(nameParts[0]);
        resource.setLastName(nameParts[1]);
        resource.setNotes(validFormValues.get(EmployeeResource.PROPERTY_NOTES));
        resource.setBirthday(DateTools.parseDateOnly(validFormValues.get(EmployeeResource.PROPERTY_BIRTHDAY)));
        resource.setFoodPreferences(CommonFieldHelper.fromFormListToSet(validFormValues, EmployeeResource.PROPERTY_FOOD_PREFERENCES));

        CommonResourceLink.fillResourceLink(servicesCtx, resource, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class, FIELD_NAME_MANAGER_ID, validFormValues, changesContext);
    }

    @Override
    public void formatForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues) {

        CommonFormatting.trimSpacesAround(rawFormValues);

        String birthday = rawFormValues.get(EmployeeResource.PROPERTY_BIRTHDAY);
        if (birthday != null) {
            birthday = birthday.replaceAll("/", "-");
            rawFormValues.put(EmployeeResource.PROPERTY_BIRTHDAY, birthday);
        }

        CommonFormatting.firstLetterOfEachWordCapital(rawFormValues, FIELD_NAME_FULL_NAME);
    }

    @Override
    public Class<EmployeeResource> getForResourceType() {
        return EmployeeResource.class;
    }

    @Override
    public PageDefinition providePageDefinition(CommonServicesContext servicesCtx, EmployeeResource editedResource) {

        TranslationService translationService = servicesCtx.getTranslationService();

        PageDefinition pageDefinition = new PageDefinition(translationService.translate("EmployeeResourceSimpleForm.title"));

        pageDefinition.addPageItem(new LabelPageItem().setText(translationService.translate("EmployeeResourceSimpleForm.information")));

        ListPageItem instructionsPageItem = new ListPageItem().setTitle(translationService.translate("EmployeeResourceSimpleForm.instructions"));
        instructionsPageItem.getItems().add(translationService.translate("EmployeeResourceSimpleForm.instructions.1"));
        instructionsPageItem.getItems().add(translationService.translate("EmployeeResourceSimpleForm.instructions.2"));
        pageDefinition.addPageItem(instructionsPageItem);

        InputTextFieldPageItem fullNamePageItem = new InputTextFieldPageItem().setLabel(translationService.translate("EmployeeResourceSimpleForm.fullName"));
        fullNamePageItem.setFieldName(FIELD_NAME_FULL_NAME);
        pageDefinition.addPageItem(fullNamePageItem);

        InputTextFieldPageItem birthdayPageItem = CommonPageItem.createInputTextField(servicesCtx, pageDefinition, "EmployeeResourceSimpleForm.birthday", EmployeeResource.PROPERTY_BIRTHDAY);

        MultilineInputTextFieldPageItem notesPageItem = CommonPageItem.createMultilineInputTextField(servicesCtx, pageDefinition, "EmployeeResourceSimpleForm.notes", EmployeeResource.PROPERTY_NOTES);
        notesPageItem.setRows(10);

        ListInputTextFieldPageItem foodPreferencesPageItem = CommonPageItem.createListInputTextFieldPageItem(servicesCtx, pageDefinition, "EmployeeResourceSimpleForm.foodPreferences",
                EmployeeResource.PROPERTY_FOOD_PREFERENCES);

        CommonResourceLink.addResourcePageItem(servicesCtx, pageDefinition, editedResource, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class,
                translationService.translate("EmployeeResourceSimpleForm.manager"), FIELD_NAME_MANAGER_ID);

        if (editedResource != null) {
            fullNamePageItem.setFieldValue(editedResource.getFirstName() + " " + editedResource.getLastName());
            notesPageItem.setFieldValue(editedResource.getNotes());

            foodPreferencesPageItem.setFieldValues(CommonFieldHelper.fromSetToList(editedResource.getFoodPreferences()));

            Date birthday = editedResource.getBirthday();
            if (birthday != null) {
                birthdayPageItem.setFieldValue(DateTools.formatDateOnly(birthday));
            }
        }

        return pageDefinition;
    }

    @Override
    public List<Tuple2<String, String>> validateForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues) {

        TranslationService translationService = servicesCtx.getTranslationService();

        List<Tuple2<String, String>> errors = new ArrayList<>();

        errors.addAll(CommonValidation.validateNotNullOrEmpty(rawFormValues, //
                FIELD_NAME_FULL_NAME, //
                EmployeeResource.PROPERTY_BIRTHDAY //
        ));

        String fullName = rawFormValues.get(FIELD_NAME_FULL_NAME);
        if (fullName != null) {
            String[] nameParts = fullName.split(" ");
            if (nameParts.length != 2) {
                errors.add(new Tuple2<>(FIELD_NAME_FULL_NAME, translationService.translate("error.employee.needFirstAndLastName")));
            }
        }

        String birthday = rawFormValues.get(EmployeeResource.PROPERTY_BIRTHDAY);
        if (birthday != null) {
            try {
                DateTools.parseDateOnly(birthday);
            } catch (Exception e) {
                errors.add(new Tuple2<>(EmployeeResource.PROPERTY_BIRTHDAY, translationService.translate("error.dayFormat")));
            }
        }

        return errors;
    }
}
