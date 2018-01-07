/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.helper;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.visual.PageDefinition;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.InputTextFieldPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.ListInputTextFieldPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.MultilineInputTextFieldPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.SelectOptionsPageItem;
import com.foilen.infra.plugin.v1.model.resource.IPResource;

/**
 * Some helpers for {@link ResourceEditor#providePageDefinition(CommonServicesContext, IPResource)}.
 */
public class CommonPageItem {

    /**
     * Create and add an {@link InputTextFieldPageItem} to the page.
     *
     * @param servicesCtx
     *            the services context to get the translation service
     * @param pageDefinition
     *            the page definition on which to add the field
     * @param labelCode
     *            the message code for the label (will be translated)
     * @param fieldName
     *            the name of the field
     * @return the created field on which you can set the value if needed
     */
    public static InputTextFieldPageItem createInputTextField(CommonServicesContext servicesCtx, PageDefinition pageDefinition, String labelCode, String fieldName) {
        InputTextFieldPageItem pageItem = new InputTextFieldPageItem().setLabel(servicesCtx.getTranslationService().translate(labelCode));
        pageItem.setFieldName(fieldName);
        pageDefinition.addPageItem(pageItem);
        return pageItem;
    }

    /**
     * Create and add a {@link ListInputTextFieldPageItem} to the page.
     *
     * @param servicesCtx
     *            the services context to get the translation service
     * @param pageDefinition
     *            the page definition on which to add the field
     * @param labelCode
     *            the message code for the label (will be translated)
     * @param fieldName
     *            the name of the field
     * @return the created field on which you can set the value if needed
     */
    public static ListInputTextFieldPageItem createListInputTextFieldPageItem(CommonServicesContext servicesCtx, PageDefinition pageDefinition, String labelCode, String fieldName) {
        ListInputTextFieldPageItem pageItem = new ListInputTextFieldPageItem().setLabel(servicesCtx.getTranslationService().translate(labelCode));
        pageItem.setFieldName(fieldName);
        pageDefinition.addPageItem(pageItem);
        return pageItem;
    }

    /**
     * Create and add an {@link MultilineInputTextFieldPageItem} to the page.
     *
     * @param servicesCtx
     *            the services context to get the translation service
     * @param pageDefinition
     *            the page definition on which to add the field
     * @param labelCode
     *            the message code for the label (will be translated)
     * @param fieldName
     *            the name of the field
     * @return the created field on which you can set the value if needed
     */
    public static MultilineInputTextFieldPageItem createMultilineInputTextField(CommonServicesContext servicesCtx, PageDefinition pageDefinition, String labelCode, String fieldName) {
        MultilineInputTextFieldPageItem pageItem = new MultilineInputTextFieldPageItem().setLabel(servicesCtx.getTranslationService().translate(labelCode));
        pageItem.setFieldName(fieldName);
        pageDefinition.addPageItem(pageItem);
        return pageItem;
    }

    /**
     * Create and add a {@link SelectOptionsPageItem} to the page.
     *
     * @param servicesCtx
     *            the services context to get the translation service
     * @param pageDefinition
     *            the page definition on which to add the field
     * @param labelCode
     *            the message code for the label (will be translated)
     * @param fieldName
     *            the name of the field
     * @param options
     *            the list of options
     * @return the created field on which you can set the value if needed
     */
    public static SelectOptionsPageItem createSelectOptionsField(CommonServicesContext servicesCtx, PageDefinition pageDefinition, String labelCode, String fieldName, Enum<?>... options) {
        SelectOptionsPageItem pageItem = new SelectOptionsPageItem().setLabel(servicesCtx.getTranslationService().translate(labelCode));
        pageItem.setFieldName(fieldName);
        pageItem.setOptions(Arrays.asList(options).stream().map(it -> it.name()).collect(Collectors.toList()));
        pageDefinition.addPageItem(pageItem);
        return pageItem;
    }

    private CommonPageItem() {
    }
}
