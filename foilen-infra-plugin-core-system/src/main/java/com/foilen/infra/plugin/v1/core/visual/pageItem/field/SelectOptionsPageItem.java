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
 * A select/options list.
 */
public class SelectOptionsPageItem extends AbstractFieldPageItem {

    private String label;
    private List<String> options = new ArrayList<>();

    public String getLabel() {
        return label;
    }

    public List<String> getOptions() {
        return options;
    }

    public SelectOptionsPageItem setLabel(String label) {
        this.label = label;
        return this;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this) //
                .add("label", label) //
                .add("fieldName", getFieldName()) //
                .add("fieldValue", getFieldValue()) //
                .toString();
    }

}
