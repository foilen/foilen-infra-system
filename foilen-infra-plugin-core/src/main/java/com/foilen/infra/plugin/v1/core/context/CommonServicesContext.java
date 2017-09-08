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
import com.foilen.infra.plugin.v1.core.service.RealmPluginService;
import com.foilen.infra.plugin.v1.core.service.TimerService;
import com.foilen.infra.plugin.v1.core.service.TranslationService;

/**
 * All the services.
 */
public class CommonServicesContext {

    private MessagingService messagingService;
    private RealmPluginService realmPluginService;
    private IPResourceService resourceService;
    private TimerService timerService;
    private TranslationService translationService;

    public CommonServicesContext( //
            MessagingService messagingService, //
            RealmPluginService realmPluginService, //
            IPResourceService resourceService, //
            TimerService timerService, //
            TranslationService translationService //
    ) {
        this.messagingService = messagingService;
        this.realmPluginService = realmPluginService;
        this.resourceService = resourceService;
        this.timerService = timerService;
        this.translationService = translationService;
    }

    public MessagingService getMessagingService() {
        return messagingService;
    }

    public RealmPluginService getRealmPluginService() {
        return realmPluginService;
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
