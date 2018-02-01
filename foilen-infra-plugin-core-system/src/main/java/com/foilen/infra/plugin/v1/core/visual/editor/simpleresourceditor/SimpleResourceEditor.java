/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.editor.simpleresourceditor;

import java.util.List;
import java.util.Map;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.visual.PageDefinition;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tuple.Tuple2;

/**
 * To create editors that manages everything per field instead of all fields per step.
 */
public abstract class SimpleResourceEditor<R extends IPResource> extends AbstractBasics implements ResourceEditor<R> {

    @Override
    public final void fillResource(CommonServicesContext servicesCtx, ChangesContext changesContext, Map<String, String> validFormValues, R editedResource) {
        SimpleResourceEditorDefinition simpleResourceEditorDefinition = new SimpleResourceEditorDefinition(this, servicesCtx, editedResource);
        getDefinition(simpleResourceEditorDefinition);
        simpleResourceEditorDefinition.fill(changesContext, validFormValues);
    }

    @Override
    public final void formatForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues) {
        SimpleResourceEditorDefinition simpleResourceEditorDefinition = new SimpleResourceEditorDefinition(this, servicesCtx, null);
        getDefinition(simpleResourceEditorDefinition);
        simpleResourceEditorDefinition.format(rawFormValues);
    }

    protected abstract void getDefinition(SimpleResourceEditorDefinition simpleResourceEditorDefinition);

    @Override
    public final PageDefinition providePageDefinition(CommonServicesContext servicesCtx, R editedResource) {
        SimpleResourceEditorDefinition simpleResourceEditorDefinition = new SimpleResourceEditorDefinition(this, servicesCtx, editedResource);
        getDefinition(simpleResourceEditorDefinition);
        return simpleResourceEditorDefinition.getPageDefinition();
    }

    @Override
    public final List<Tuple2<String, String>> validateForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues) {

        SimpleResourceEditorDefinition simpleResourceEditorDefinition = new SimpleResourceEditorDefinition(this, servicesCtx, null);
        getDefinition(simpleResourceEditorDefinition);

        return simpleResourceEditorDefinition.validate(rawFormValues);
    }

}
