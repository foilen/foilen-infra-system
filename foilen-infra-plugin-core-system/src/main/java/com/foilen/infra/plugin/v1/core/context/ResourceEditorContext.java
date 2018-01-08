/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.context;

import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.google.common.base.MoreObjects;

public class ResourceEditorContext {

    private ResourceEditor<?> editor;
    private String editorName;

    public <T extends IPResource> ResourceEditorContext(ResourceEditor<T> provider, String providerName) {
        this.editor = provider;
        this.editorName = providerName;
    }

    public ResourceEditor<?> getEditor() {
        return editor;
    }

    public String getEditorName() {
        return editorName;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this) //
                .add("editorName", editorName) //
                .toString();
    }
}
