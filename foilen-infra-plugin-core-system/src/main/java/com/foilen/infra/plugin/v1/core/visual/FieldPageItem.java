/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual;

/**
 * A page item that is a form's field.
 */
public interface FieldPageItem extends PageItem {

    String getFieldName();

    String getFieldValue();

    void setFieldName(String formName);

    void setFieldValue(String formValue);

}
