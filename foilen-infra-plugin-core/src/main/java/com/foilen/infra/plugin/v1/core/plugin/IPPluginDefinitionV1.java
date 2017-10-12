/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.plugin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import com.foilen.infra.plugin.v1.core.context.ResourceEditorContext;
import com.foilen.infra.plugin.v1.core.context.TimerEventContext;
import com.foilen.infra.plugin.v1.core.context.UpdateEventContext;
import com.foilen.infra.plugin.v1.core.eventhandler.TimerEventHandler;
import com.foilen.infra.plugin.v1.core.eventhandler.UpdateEventHandler;
import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.model.resource.IPResource;

public class IPPluginDefinitionV1 {

    // Information
    private String pluginVendor;
    private String pluginName;
    private String pluginDescription;
    private String pluginVersion;

    // Extension points
    private List<TimerEventContext> timers = new ArrayList<>();
    private List<UpdateEventContext> updateHandlers = new ArrayList<>();
    private List<IPResourceDefinition> customResources = new ArrayList<>();
    private List<ResourceEditorContext> resourceEditors = new ArrayList<>();
    private List<String> translations = new ArrayList<>();

    public IPPluginDefinitionV1(String pluginVendor, String pluginName, String pluginDescription, String pluginVersion) {
        this.pluginVendor = pluginVendor;
        this.pluginName = pluginName;
        this.pluginDescription = pluginDescription;
        this.pluginVersion = pluginVersion;
    }

    /**
     * Add a resource type.
     *
     * @param resourceClass
     *            the class that can load that resource type
     * @param resourceType
     *            the type of the resource
     * @param primaryKeyProperties
     *            all the combined properties that can retrieve a unique resource
     * @param searchableProperties
     *            which properties should be searchable (and be indexed)
     */
    public void addCustomResource(Class<? extends IPResource> resourceClass, String resourceType, Collection<String> primaryKeyProperties, Collection<String> searchableProperties) {
        customResources.add(new IPResourceDefinition(resourceClass, resourceType, primaryKeyProperties, searchableProperties));
    }

    /**
     * Add a resource editor.
     *
     * @param editor
     *            the form page definition editor
     * @param editorName
     *            a unique name for this editor
     */
    public void addResourceEditor(ResourceEditor<?> editor, String editorName) {
        resourceEditors.add(new ResourceEditorContext(editor, editorName));
    }

    /**
     * Add a recurrent timer.
     *
     * @param handler
     *            the event handler
     * @param timerName
     *            the name of the timer
     * @param calendarUnit
     *            the unit of the delta that is a constant on {@link Calendar}
     * @param deltaTime
     *            the delta between events
     */
    public void addTimer(TimerEventHandler handler, String timerName, int calendarUnit, int deltaTime) {
        timers.add(new TimerEventContext(handler, timerName, calendarUnit, deltaTime));
    }

    /**
     * Add a timer.
     *
     * @param handler
     *            the event handler
     * @param timerName
     *            the name of the timer
     * @param calendarUnit
     *            the unit of the delta that is a constant on {@link Calendar}
     * @param deltaTime
     *            the delta between events
     * @param oneTime
     *            just runs once
     * @param startWhenFirstCreated
     *            true: start once when created ; false: start only when delta is reached
     */
    public void addTimer(TimerEventHandler handler, String timerName, int calendarUnit, int deltaTime, boolean oneTime, boolean startWhenFirstCreated) {
        timers.add(new TimerEventContext(handler, timerName, calendarUnit, deltaTime, oneTime, startWhenFirstCreated));
    }

    /**
     * Add a path to translations files.
     *
     * @param basename
     *            the path and prefix of translation file (e.g: /com/foilen/infra/plugin/v1/example/messages )
     */
    public void addTranslations(String basename) {
        translations.add(basename);
    }

    /**
     * Add an update event handler. Uses the simple class name as the update handler name.
     *
     * @param handler
     *            the event handler
     */
    public void addUpdateHandler(UpdateEventHandler<?> handler) {
        String updateHandlerName = handler.getClass().getSimpleName();
        updateHandlers.add(new UpdateEventContext(handler, updateHandlerName));
    }

    /**
     * Add an update event handler.
     *
     * @param handler
     *            the event handler
     * @param updateHandlerName
     *            the name of the handler
     */
    public void addUpdateHandler(UpdateEventHandler<?> handler, String updateHandlerName) {
        updateHandlers.add(new UpdateEventContext(handler, updateHandlerName));
    }

    public List<IPResourceDefinition> getCustomResources() {
        return customResources;
    }

    public String getPluginDescription() {
        return pluginDescription;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getPluginVendor() {
        return pluginVendor;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public List<ResourceEditorContext> getResourceEditors() {
        return resourceEditors;
    }

    public List<TimerEventContext> getTimers() {
        return timers;
    }

    public List<String> getTranslations() {
        return translations;
    }

    public List<UpdateEventContext> getUpdateHandlers() {
        return updateHandlers;
    }

    public IPPluginDefinitionV1 setPluginDescription(String pluginDescription) {
        this.pluginDescription = pluginDescription;
        return this;
    }

    public IPPluginDefinitionV1 setPluginName(String pluginName) {
        this.pluginName = pluginName;
        return this;
    }

    public IPPluginDefinitionV1 setPluginVendor(String pluginVendor) {
        this.pluginVendor = pluginVendor;
        return this;
    }

    public IPPluginDefinitionV1 setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("IPPluginDefinitionV1 [pluginVendor=");
        builder.append(pluginVendor);
        builder.append(", pluginName=");
        builder.append(pluginName);
        builder.append(", pluginDescription=");
        builder.append(pluginDescription);
        builder.append(", pluginVersion=");
        builder.append(pluginVersion);
        builder.append("]");
        return builder.toString();
    }

}
