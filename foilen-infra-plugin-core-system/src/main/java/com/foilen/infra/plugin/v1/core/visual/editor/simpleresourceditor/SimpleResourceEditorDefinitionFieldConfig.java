/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.editor.simpleresourceditor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.foilen.infra.plugin.v1.core.visual.PageDefinition;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.AbstractFieldPageItem;
import com.foilen.smalltools.tuple.Tuple2;

public class SimpleResourceEditorDefinitionFieldConfig {

    private String propertyName;

    private Function<PageDefinition, AbstractFieldPageItem> genAndAddPageItem;
    private Consumer<AbstractFieldPageItem> augmentPageItem = (pageItem) -> {
    };
    private Consumer<PopulatePageItemCtx> populatePageItem = (ctx) -> ctx.getPageItem().setFieldValue(this.getConvertToString().apply(ctx.getPropertyValue()));
    private Consumer<PopulateResourceCtx> populateResource = (ctx) -> ctx.getEditedResourceBeanWrapper().setPropertyValue(ctx.getPropertyName(), this.getConvertFromString().apply(ctx.getTextValue()));

    private Function<?, String> convertToString = (value) -> value.toString();
    private Function<String, Object> convertFromString = (value) -> String.valueOf(value);

    private List<BiFunction<String, String, List<Tuple2<String, String>>>> validators = new ArrayList<>();
    private List<Function<String, String>> formators = new ArrayList<>();

    @SafeVarargs
    public final void addFormator(Function<String, String>... formators) {
        for (Function<String, String> formator : formators) {
            this.formators.add(formator);
        }
    }

    public Consumer<PopulateResourceCtx> getPopulateResource() {
        return populateResource;
    }

    public void setPopulateResource(Consumer<PopulateResourceCtx> populateResource) {
        this.populateResource = populateResource;
    }

    @SafeVarargs
    public final void addValidator(BiFunction<String, String, List<Tuple2<String, String>>>... validators) {
        for (BiFunction<String, String, List<Tuple2<String, String>>> validator : validators) {
            this.validators.add(validator);
        }
    }

    public Consumer<AbstractFieldPageItem> getAugmentPageItem() {
        return augmentPageItem;
    }

    public Function<String, Object> getConvertFromString() {
        return convertFromString;
    }

    @SuppressWarnings("unchecked")
    public Function<Object, String> getConvertToString() {
        return (Function<Object, String>) convertToString;
    }

    public List<Function<String, String>> getFormators() {
        return formators;
    }

    public Function<PageDefinition, AbstractFieldPageItem> getGenAndAddPageItem() {
        return genAndAddPageItem;
    }

    public Consumer<PopulatePageItemCtx> getPopulatePageItem() {
        return populatePageItem;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public List<BiFunction<String, String, List<Tuple2<String, String>>>> getValidators() {
        return validators;
    }

    public SimpleResourceEditorDefinitionFieldConfig setAugmentPageItem(Consumer<AbstractFieldPageItem> augmentPageItem) {
        this.augmentPageItem = augmentPageItem;
        return this;
    }

    public SimpleResourceEditorDefinitionFieldConfig setConvertFromString(Function<String, Object> convertFromString) {
        this.convertFromString = convertFromString;
        return this;
    }

    public <T> SimpleResourceEditorDefinitionFieldConfig setConvertToString(Function<T, String> convertToString) {
        this.convertToString = convertToString;
        return this;
    }

    public SimpleResourceEditorDefinitionFieldConfig setGenAndAddPageItem(Function<PageDefinition, AbstractFieldPageItem> genPageItem) {
        this.genAndAddPageItem = genPageItem;
        return this;
    }

    public SimpleResourceEditorDefinitionFieldConfig setPopulatePageItem(Consumer<PopulatePageItemCtx> populatePageItem) {
        this.populatePageItem = populatePageItem;
        return this;
    }

    public SimpleResourceEditorDefinitionFieldConfig setPropertyName(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public SimpleResourceEditorDefinitionFieldConfig setValidators(List<BiFunction<String, String, List<Tuple2<String, String>>>> validators) {
        this.validators = validators;
        return this;
    }

}
