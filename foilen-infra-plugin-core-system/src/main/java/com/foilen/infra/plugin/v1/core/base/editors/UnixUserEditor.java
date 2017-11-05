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
import java.util.Optional;

import org.apache.commons.codec.digest.Sha2Crypt;

import com.foilen.infra.plugin.v1.core.base.resources.UnixUser;
import com.foilen.infra.plugin.v1.core.base.resources.helper.UnixUserAvailableIdHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.TranslationService;
import com.foilen.infra.plugin.v1.core.visual.PageDefinition;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonFormatting;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonPageItem;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonValidation;
import com.foilen.infra.plugin.v1.core.visual.pageItem.LabelPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.InputTextFieldPageItem;
import com.foilen.smalltools.tools.CharsetTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.google.common.base.Strings;

public class UnixUserEditor implements ResourceEditor<UnixUser> {

    public static final String EDITOR_NAME = "Unix User";

    protected static final String FIELD_PASSWORD_CONF = "passwordConf";
    protected static final String FIELD_PASSWORD = "password";
    protected static final String CLEAR_PASSWORD_CHAR = "*";

    @Override
    public void fillResource(CommonServicesContext servicesCtx, ChangesContext changesContext, Map<String, String> validFormValues, UnixUser resource) {
        if (resource.getId() == null) {
            // Choose id
            resource.setId(UnixUserAvailableIdHelper.getNextAvailableId());

            // Other initial properties
            String username = validFormValues.get(UnixUser.PROPERTY_NAME);
            resource.setHomeFolder("/home/" + username);
        }

        // Other common properties
        resource.setName(validFormValues.get(UnixUser.PROPERTY_NAME));

        // Update password
        String password = validFormValues.get(FIELD_PASSWORD);
        String passwordConf = validFormValues.get(FIELD_PASSWORD_CONF);
        if (Strings.isNullOrEmpty(passwordConf) && CLEAR_PASSWORD_CHAR.equals(password)) {
            // Clear the password
            resource.setHashedPassword(null);
        } else if (!Strings.isNullOrEmpty(password)) {
            resource.setHashedPassword(Sha2Crypt.sha512Crypt(password.getBytes(CharsetTools.UTF_8)));
        }

    }

    @Override
    public void formatForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues) {
        CommonFormatting.trimSpacesAround(rawFormValues, UnixUser.PROPERTY_NAME);
        CommonFormatting.toLowerCase(rawFormValues, UnixUser.PROPERTY_NAME);
    }

    @Override
    public Class<UnixUser> getForResourceType() {
        return UnixUser.class;
    }

    @Override
    public PageDefinition providePageDefinition(CommonServicesContext servicesCtx, UnixUser resource) {

        TranslationService translationService = servicesCtx.getTranslationService();

        PageDefinition pageDefinition = new PageDefinition(translationService.translate("UnixUserEditor.title"));

        if (resource != null) {
            pageDefinition.addPageItem(new LabelPageItem().setText(translationService.translate("UnixUserEditor.id", resource.getId())));
        }
        InputTextFieldPageItem namePageItem = CommonPageItem.createInputTextField(servicesCtx, pageDefinition, "UnixUserEditor.name", UnixUser.PROPERTY_NAME);
        pageDefinition.addPageItem(new LabelPageItem().setText(translationService.translate("UnixUserEditor.clearPasswordInstructions")));
        CommonPageItem.createInputTextField(servicesCtx, pageDefinition, "UnixUserEditor.password", FIELD_PASSWORD).setPassword(true);
        CommonPageItem.createInputTextField(servicesCtx, pageDefinition, "UnixUserEditor.passwordConf", FIELD_PASSWORD_CONF).setPassword(true);

        if (resource != null) {
            namePageItem.setFieldValue(resource.getName());
        }

        return pageDefinition;

    }

    @Override
    public List<Tuple2<String, String>> validateForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues) {

        List<Tuple2<String, String>> errors = CommonValidation.validateNotNullOrEmpty(rawFormValues, UnixUser.PROPERTY_NAME);
        // If new name or changing name, make sure no collision
        if (errors.isEmpty()) {
            IPResourceService resourceService = servicesCtx.getResourceService();
            String username = rawFormValues.get(UnixUser.PROPERTY_NAME);
            Optional<UnixUser> unixUser = resourceService.resourceFind(resourceService.createResourceQuery(UnixUser.class) //
                    .propertyEquals(UnixUser.PROPERTY_NAME, username));
            if (unixUser.isPresent()) {
                Long expectedInternalId = null;
                try {
                    String idText = rawFormValues.get("_resourceId");
                    if (!Strings.isNullOrEmpty(idText)) {
                        expectedInternalId = Long.valueOf(idText);
                    }
                } catch (Exception e) {
                }

                if (!unixUser.get().getInternalId().equals(expectedInternalId)) {
                    errors.add(new Tuple2<>(UnixUser.PROPERTY_NAME, "error.nameTaken"));
                }
            }
        }

        // Password are confirmed
        String password = rawFormValues.get(FIELD_PASSWORD);
        String passwordConf = rawFormValues.get(FIELD_PASSWORD_CONF);
        if (Strings.isNullOrEmpty(passwordConf) && CLEAR_PASSWORD_CHAR.equals(password)) {
            // Fine, will clear the password
        } else {
            errors.addAll(CommonValidation.validateSamePassword(rawFormValues, FIELD_PASSWORD, FIELD_PASSWORD_CONF));
        }

        return errors;

    }

}
