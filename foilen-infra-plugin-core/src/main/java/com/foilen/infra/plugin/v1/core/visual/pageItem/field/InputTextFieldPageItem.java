/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.pageItem.field;

import com.google.common.base.MoreObjects;

/**
 * An input text.
 */
public class InputTextFieldPageItem extends AbstractFieldPageItem {

    private String label;
    private String placeholder;
    private boolean isPassword;

    public String getLabel() {
        return label;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public boolean isPassword() {
        return isPassword;
    }

    public InputTextFieldPageItem setLabel(String label) {
        this.label = label;
        return this;
    }

    public InputTextFieldPageItem setPassword(boolean isPassword) {
        this.isPassword = isPassword;
        return this;
    }

    public InputTextFieldPageItem setPlaceholder(String placeholder) {
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
                .add("fieldValue", getFieldValue()) //
                .toString();
    }

}
