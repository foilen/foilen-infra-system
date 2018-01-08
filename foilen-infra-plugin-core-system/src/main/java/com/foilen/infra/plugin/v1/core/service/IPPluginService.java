/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.service;

import java.util.List;
import java.util.Optional;

import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.UpdateEventContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionProvider;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionV1;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tuple.Tuple3;

/**
 * The plugin loader.
 */
public interface IPPluginService {

    /**
     * Get the list of working plugins.
     *
     * @return the immutable list of working plugins
     */
    List<IPPluginDefinitionV1> getAvailablePlugins();

    /**
     * Get the list of broken plugins (those that couldn't be loaded).
     *
     * @return the immutable list of broken plugins with their error message
     */
    List<Tuple3<Class<? extends IPPluginDefinitionProvider>, IPPluginDefinitionV1, String>> getBrokenPlugins();

    /**
     * Get the resource editor.
     *
     * @param editorName
     *            the name of the editor
     * @return the editor
     */
    Optional<ResourceEditor<?>> getResourceEditorByName(String editorName);

    /**
     * Get all the possible editors for the resource type.
     *
     * @param resourceType
     *            the resource type
     * @return the list of editors names
     */
    List<String> getResourceEditorNamesByResourceType(Class<? extends IPResource> resourceType);

    /**
     * Get all the update event handlers.
     *
     * @return the immutable list of handlers
     */
    List<UpdateEventContext> getUpdateEvents();

    /**
     * Load all the plugins.
     *
     * @param commonServicesContext
     *            the common services context
     * @param internalServicesContext
     *            the internal services context
     */
    void loadPlugins(CommonServicesContext commonServicesContext, InternalServicesContext internalServicesContext);

}
