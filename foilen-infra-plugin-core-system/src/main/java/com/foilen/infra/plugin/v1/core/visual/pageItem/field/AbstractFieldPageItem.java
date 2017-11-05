/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.pageItem.field;

import com.foilen.infra.plugin.v1.core.visual.FieldPageItem;

public abstract class AbstractFieldPageItem implements FieldPageItem {

    private String fieldName;
    private String fieldValue;

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getFieldValue() {
        return fieldValue;
    }

    @Override
    public void setFieldName(String formName) {
        this.fieldName = formName;
    }

    @Override
    public void setFieldValue(String formValue) {
        this.fieldValue = formValue;
    }

}
