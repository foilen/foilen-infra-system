/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.editor.simpleresourceditor;

import com.foilen.infra.plugin.v1.core.visual.pageItem.field.AbstractFieldPageItem;
import com.foilen.infra.plugin.v1.model.resource.IPResource;

public class PopulatePageItemCtx {

    private AbstractFieldPageItem pageItem;
    IPResource editedResource;
    private Object propertyValue;

    public PopulatePageItemCtx(AbstractFieldPageItem pageItem, IPResource editedResource, Object propertyValue) {
        this.pageItem = pageItem;
        this.editedResource = editedResource;
        this.propertyValue = propertyValue;
    }

    public AbstractFieldPageItem getPageItem() {
        return pageItem;
    }

    public Object getPropertyValue() {
        return propertyValue;
    }

}