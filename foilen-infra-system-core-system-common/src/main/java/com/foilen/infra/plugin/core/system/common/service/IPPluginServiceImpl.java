/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.common.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.ResourceEditorContext;
import com.foilen.infra.plugin.v1.core.context.TimerEventContext;
import com.foilen.infra.plugin.v1.core.context.UpdateEventContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionProvider;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionV1;
import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.core.service.IPPluginService;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.comparator.ClassNameComparator;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.AssertTools;
import com.foilen.smalltools.tools.SystemTools;
import com.foilen.smalltools.tuple.Tuple3;

public class IPPluginServiceImpl extends AbstractBasics implements IPPluginService {

    private static final Reflections reflections;

    private static final boolean skipUpdateEvents;

    static {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.forPackages("com", "org", "net");
        configurationBuilder.addUrls(ClasspathHelper.forManifest());
        reflections = new Reflections(configurationBuilder);

        skipUpdateEvents = Boolean.valueOf(SystemTools.getPropertyOrEnvironment("FOILEN_PLUGIN_SKIP_UPDATE_EVENTS", "false"));
    }

    private List<Tuple3<Class<? extends IPPluginDefinitionProvider>, IPPluginDefinitionV1, String>> brokenPlugins;
    private List<IPPluginDefinitionV1> availablePlugins;
    private List<UpdateEventContext> updateEvents;

    @Override
    public List<IPPluginDefinitionV1> getAvailablePlugins() {
        return availablePlugins;
    }

    @Override
    public List<Tuple3<Class<? extends IPPluginDefinitionProvider>, IPPluginDefinitionV1, String>> getBrokenPlugins() {
        return brokenPlugins;
    }

    private String getErrorMessage(Exception e) {
        StringBuilder errorMessage = new StringBuilder();
        Throwable t = e;
        boolean first = true;
        while (t != null) {
            if (first) {
                first = false;
            } else {
                errorMessage.append(" | ");
            }
            errorMessage.append(t.getClass().getSimpleName()).append(":").append(t.getMessage());
            t = t.getCause();
        }
        return errorMessage.toString();
    }

    @Override
    public Optional<ResourceEditor<?>> getResourceEditorByName(String editorName) {
        Optional<ResourceEditorContext> resourceCommonServicesContextOptional = availablePlugins.stream() //
                .map(IPPluginDefinitionV1::getResourceEditors) //
                .reduce(new ArrayList<>(), (a, b) -> {
                    a.addAll(b);
                    return a;
                }) //
                .stream() //
                .filter(it -> editorName.equals(it.getEditorName())) //
                .findAny();

        if (!resourceCommonServicesContextOptional.isPresent()) {
            return Optional.empty();
        }

        return Optional.of(resourceCommonServicesContextOptional.get().getEditor());
    }

    @Override
    public List<String> getResourceEditorNamesByResourceType(Class<? extends IPResource> resourceType) {
        return availablePlugins.stream() //
                .map(IPPluginDefinitionV1::getResourceEditors) //
                .reduce(new ArrayList<>(), (a, b) -> {
                    a.addAll(b);
                    return a;
                }) //
                .stream() //
                .filter(it -> resourceType.isAssignableFrom((it.getEditor().getForResourceType()))) //
                .map(ResourceEditorContext::getEditorName) //
                .sorted() //
                .collect(Collectors.toList());
    }

    @Override
    public List<UpdateEventContext> getUpdateEvents() {
        return updateEvents;
    }

    @Override
    public void loadPlugins(CommonServicesContext commonServicesContext, InternalServicesContext internalServicesContext) {

        AssertTools.assertNull(availablePlugins, "Plugin service already loaded");

        // Prepare the list of plugins
        List<Tuple3<Class<? extends IPPluginDefinitionProvider>, IPPluginDefinitionV1, String>> brokenPlugins = new ArrayList<>();
        List<Tuple3<Class<? extends IPPluginDefinitionProvider>, IPPluginDefinitionProvider, IPPluginDefinitionV1>> availablePlugins = new ArrayList<>();

        logger.info("Seaching for plugins");

        // Find the providers
        List<Class<? extends IPPluginDefinitionProvider>> providers = reflections.getSubTypesOf(IPPluginDefinitionProvider.class).stream() //
                .sorted(new ClassNameComparator()) //
                .collect(Collectors.toList());

        logger.info("Found {} plugins", providers.size());
        for (Class<? extends IPPluginDefinitionProvider> provider : providers) {
            logger.debug("Found plugin [{}] . Loading it", provider.getName());
            IPPluginDefinitionV1 pluginDefinition = null;
            try {
                // Load the definition
                IPPluginDefinitionProvider pluginDefinitionProvider = provider.getConstructor().newInstance();
                pluginDefinition = pluginDefinitionProvider.getIPPluginDefinition();

                logger.info("[{}] {}", provider.getName(), pluginDefinition);

                // Load the custom resources
                for (IPResourceDefinition resourceDefinition : pluginDefinition.getCustomResources()) {
                    logger.info("Configure {}", resourceDefinition);
                    internalServicesContext.getInternalIPResourceService().resourceAdd(resourceDefinition);
                }

                // Save as valid plugin
                availablePlugins.add(new Tuple3<>(provider, pluginDefinitionProvider, pluginDefinition));
            } catch (Exception e) {
                logger.error("[{}] Problem initializing the plugin", provider.getName(), e);
                brokenPlugins.add(new Tuple3<>(provider, pluginDefinition, getErrorMessage(e)));
            }

        }

        // Init
        Iterator<Tuple3<Class<? extends IPPluginDefinitionProvider>, IPPluginDefinitionProvider, IPPluginDefinitionV1>> availablePluginsIt = availablePlugins.iterator();
        while (availablePluginsIt.hasNext()) {
            Tuple3<Class<? extends IPPluginDefinitionProvider>, IPPluginDefinitionProvider, IPPluginDefinitionV1> next = availablePluginsIt.next();
            IPPluginDefinitionProvider provider = next.getB();
            try {
                provider.initialize(commonServicesContext);
            } catch (Exception e) {
                logger.error("[{}] Problem initializing the plugin", next.getA().getName(), e);
                availablePluginsIt.remove();
                brokenPlugins.add(new Tuple3<>(next.getA(), next.getC(), getErrorMessage(e)));
            }
        }

        updateResourcesColumnSearch(commonServicesContext.getResourceService().getResourceDefinitions());

        // Enable the events
        List<UpdateEventContext> updateEvents = new ArrayList<>();
        Iterator<Tuple3<Class<? extends IPPluginDefinitionProvider>, IPPluginDefinitionProvider, IPPluginDefinitionV1>> it = availablePlugins.iterator();
        while (it.hasNext()) {
            Tuple3<Class<? extends IPPluginDefinitionProvider>, IPPluginDefinitionProvider, IPPluginDefinitionV1> entry = it.next();
            Class<? extends IPPluginDefinitionProvider> provider = entry.getA();
            IPPluginDefinitionV1 pluginDefinition = entry.getC();
            try {

                // Set timers
                for (TimerEventContext timerEventContext : pluginDefinition.getTimers()) {
                    logger.info("Configure {}", timerEventContext);
                    commonServicesContext.getTimerService().timerAdd(timerEventContext);
                }

                // Set translations
                for (String basename : pluginDefinition.getTranslations()) {
                    commonServicesContext.getTranslationService().translationAdd(basename);
                }

                // Set update events
                for (UpdateEventContext updateEventContext : pluginDefinition.getUpdateHandlers()) {
                    logger.info("Found update event [{}] of type [{}]", updateEventContext.getUpdateHandlerName(), updateEventContext.getUpdateEventHandler().getClass());
                    updateEvents.add(updateEventContext);
                }

            } catch (Exception e) {
                it.remove();
                logger.error("[{}] Problem initializing the plugin", provider.getName(), e);
                brokenPlugins.add(new Tuple3<>(provider, pluginDefinition, getErrorMessage(e)));
            }

        }

        logger.info("Available plugins: {} ; Broken Plugins: {}", availablePlugins.size(), brokenPlugins.size());

        // Save the lists as immutable lists
        this.brokenPlugins = Collections.unmodifiableList(brokenPlugins);
        this.availablePlugins = Collections.unmodifiableList(availablePlugins.stream().map(Tuple3::getC).collect(Collectors.toList()));
        this.updateEvents = Collections.unmodifiableList(updateEvents);
        if (skipUpdateEvents) {
            logger.warn("Skipping all update events: FOILEN_PLUGIN_SKIP_UPDATE_EVENTS=true");
            this.updateEvents = Collections.emptyList();
        }
    }

    /**
     * Update all the column search for the type of resources. You can override it in your system to execute that step.
     *
     * @param resourceDefinitions
     *            all the resource definitions
     */
    protected void updateResourcesColumnSearch(List<IPResourceDefinition> resourceDefinitions) {
    }

}
