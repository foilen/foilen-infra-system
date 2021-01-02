/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.memory.junits;

import com.foilen.infra.plugin.core.system.common.service.IPPluginServiceImpl;
import com.foilen.infra.plugin.core.system.common.service.MessagingServiceLoggerImpl;
import com.foilen.infra.plugin.core.system.common.service.TimerServiceInExecutorImpl;
import com.foilen.infra.plugin.core.system.common.service.TranslationServiceImpl;
import com.foilen.infra.plugin.core.system.memory.service.ResourceServicesInMemoryImpl;
import com.foilen.infra.plugin.v1.core.common.InfraPluginCommonInit;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.service.TranslationService;

public class ResourceServicesInMemoryTests {

    public static ResourceServicesInMemoryImpl init() {

        ResourceServicesInMemoryImpl resourceServicesInMemoryImpl = new ResourceServicesInMemoryImpl();
        TimerServiceInExecutorImpl timerService = new TimerServiceInExecutorImpl();

        TranslationService translationService = new TranslationServiceImpl();

        CommonServicesContext commonServicesContext = new CommonServicesContext(new MessagingServiceLoggerImpl(), new IPPluginServiceImpl(), resourceServicesInMemoryImpl, timerService,
                translationService);
        InternalServicesContext internalServicesContext = new InternalServicesContext(resourceServicesInMemoryImpl, resourceServicesInMemoryImpl);

        resourceServicesInMemoryImpl.setCommonServicesContext(commonServicesContext);
        resourceServicesInMemoryImpl.setInternalServicesContext(internalServicesContext);

        timerService.setCommonServicesContext(commonServicesContext);
        timerService.setInternalServicesContext(internalServicesContext);

        InfraPluginCommonInit.init(commonServicesContext, internalServicesContext);

        return resourceServicesInMemoryImpl;
    }

}
