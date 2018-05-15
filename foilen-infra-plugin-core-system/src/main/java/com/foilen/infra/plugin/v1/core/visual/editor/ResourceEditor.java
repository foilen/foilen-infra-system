/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.editor;

import java.util.List;
import java.util.Map;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.visual.PageDefinition;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tuple.Tuple2;

public interface ResourceEditor<R extends IPResource> {

    /**
     * Given the valid form values, update the resource.
     *
     * @param servicesCtx
     *            the services
     * @param changesContext
     *            to add any changes there
     * @param validFormValues
     *            the values on the form
     * @param editedResource
     *            the resource to fill (will be an empty one or a copy of the current one)
     */
    void fillResource(CommonServicesContext servicesCtx, ChangesContext changesContext, Map<String, String> validFormValues, R editedResource);

    /**
     * Given the form values, update those that need formatting.
     *
     * @param servicesCtx
     *            the services
     * @param rawFormValues
     *            the values on the form
     */
    void formatForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues);

    /**
     * Get the type of the resource edited by this editor;
     *
     * @return the resource type
     */
    Class<R> getForResourceType();

    /**
     * Give the page definition for the specified resource.
     *
     * @param servicesCtx
     *            the services
     * @param editedResource
     *            the resource or null if creating a new one or need to fill it
     * @return the page definition
     */
    PageDefinition providePageDefinition(CommonServicesContext servicesCtx, R editedResource);

    /**
     * Given the form values, tell if there are errors.
     *
     * @param servicesCtx
     *            the services
     * @param rawFormValues
     *            the values on the form
     * @return list of errors: fieldName -&gt; errorCode
     */
    List<Tuple2<String, String>> validateForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues);

}
