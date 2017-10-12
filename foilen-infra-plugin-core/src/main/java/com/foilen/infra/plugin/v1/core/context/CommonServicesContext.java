/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.context;

import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.MessagingService;
import com.foilen.infra.plugin.v1.core.service.IPPluginService;
import com.foilen.infra.plugin.v1.core.service.TimerService;
import com.foilen.infra.plugin.v1.core.service.TranslationService;

/**
 * All the services.
 */
public class CommonServicesContext {

    private MessagingService messagingService;
    private IPPluginService pluginService;
    private IPResourceService resourceService;
    private TimerService timerService;
    private TranslationService translationService;

    public CommonServicesContext( //
            MessagingService messagingService, //
            IPPluginService pluginService, //
            IPResourceService resourceService, //
            TimerService timerService, //
            TranslationService translationService //
    ) {
        this.messagingService = messagingService;
        this.pluginService = pluginService;
        this.resourceService = resourceService;
        this.timerService = timerService;
        this.translationService = translationService;
    }

    public MessagingService getMessagingService() {
        return messagingService;
    }

    public IPPluginService getPluginService() {
        return pluginService;
    }

    public IPResourceService getResourceService() {
        return resourceService;
    }

    public TimerService getTimerService() {
        return timerService;
    }

    public TranslationService getTranslationService() {
        return translationService;
    }

}
