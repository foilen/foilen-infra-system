/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.pageItem.field;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.MoreObjects;

/**
 * A list of input texts.
 */
public class ListInputTextFieldPageItem extends AbstractFieldPageItem {

    private String label;
    private String placeholder;
    private boolean isPassword;
    private List<String> fieldValues = new ArrayList<>();

    public List<String> getFieldValues() {
        return fieldValues;
    }

    public String getLabel() {
        return label;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public boolean isPassword() {
        return isPassword;
    }

    public void setFieldValues(List<String> fieldValues) {
        this.fieldValues = fieldValues;
    }

    public ListInputTextFieldPageItem setLabel(String label) {
        this.label = label;
        return this;
    }

    public ListInputTextFieldPageItem setPassword(boolean isPassword) {
        this.isPassword = isPassword;
        return this;
    }

    public ListInputTextFieldPageItem setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this) //
                .add("label", label) //
                .add("placeholder", placeholder) //
                .add("isPassword", isPassword) //
                .add("fieldName", getFieldName()) //
                .add("fieldValues", getFieldValues()) //
                .toString();
    }

}
