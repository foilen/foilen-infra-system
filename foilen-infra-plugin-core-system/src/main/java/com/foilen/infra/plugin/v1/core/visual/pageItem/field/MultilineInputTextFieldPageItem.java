/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.pageItem.field;

import com.google.common.base.MoreObjects;

/**
 * An input text that is on multiple lines.
 */
public class MultilineInputTextFieldPageItem extends AbstractFieldPageItem {

    private String label;
    private int rows = 10;

    public String getLabel() {
        return label;
    }

    public int getRows() {
        return rows;
    }

    public MultilineInputTextFieldPageItem setLabel(String label) {
        this.label = label;
        return this;
    }

    public MultilineInputTextFieldPageItem setRows(int rows) {
        this.rows = rows;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this) //
                .add("label", label) //
                .add("rows", rows) //
                .add("fieldName", getFieldName()) //
                .add("fieldValue", getFieldValue()) //
                .toString();
    }

}
