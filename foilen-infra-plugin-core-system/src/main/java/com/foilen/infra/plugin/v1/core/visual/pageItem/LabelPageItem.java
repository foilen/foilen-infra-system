/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.pageItem;

import com.foilen.infra.plugin.v1.core.visual.PageItem;
import com.google.common.base.MoreObjects;

/**
 * A label with some text to display.
 */
public class LabelPageItem implements PageItem {

    private String text;

    public String getText() {
        return text;
    }

    public LabelPageItem setText(String text) {
        this.text = text;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this) //
                .add("text", text) //
                .toString();
    }

}
