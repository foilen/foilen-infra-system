/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.common.context;

import org.springframework.beans.factory.annotation.Autowired;

import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.service.IPPluginService;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.MessagingService;
import com.foilen.infra.plugin.v1.core.service.TimerService;
import com.foilen.infra.plugin.v1.core.service.TranslationService;

public class CommonServicesContextBean extends CommonServicesContext {

    @Autowired
    private MessagingService messagingService;
    @Autowired
    private IPPluginService ipPluginService;
    @Autowired
    private IPResourceService resourceService;
    @Autowired
    private TimerService timerService;
    @Autowired
    private TranslationService translationService;

    public CommonServicesContextBean() {
        super(null, null, null, null, null);
    }

    @Override
    public MessagingService getMessagingService() {
        return messagingService;
    }

    @Override
    public IPPluginService getPluginService() {
        return ipPluginService;
    }

    @Override
    public IPResourceService getResourceService() {
        return resourceService;
    }

    @Override
    public TimerService getTimerService() {
        return timerService;
    }

    @Override
    public TranslationService getTranslationService() {
        return translationService;
    }

}
