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
 * An hidden input text.
 */
public class HiddenFieldPageItem extends AbstractFieldPageItem {

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this) //
                .add("fieldName", getFieldName()) //
                .add("fieldValue", getFieldValue()) //
                .toString();
    }

}
