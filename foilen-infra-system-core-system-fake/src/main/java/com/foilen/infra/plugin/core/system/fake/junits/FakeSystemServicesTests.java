/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.junits;

import org.springframework.test.util.ReflectionTestUtils;

import com.foilen.infra.plugin.core.system.common.service.IPPluginServiceImpl;
import com.foilen.infra.plugin.core.system.fake.ConfigWebUiConfig;
import com.foilen.infra.plugin.core.system.fake.service.FakeSystemServicesImpl;
import com.foilen.infra.plugin.core.system.fake.service.TimerServiceImpl;
import com.foilen.infra.plugin.core.system.fake.service.TranslationServiceImpl;
import com.foilen.infra.plugin.v1.core.common.InfraPluginCommonInit;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.service.TranslationService;

public class FakeSystemServicesTests {

    public static FakeSystemServicesImpl init() {
        FakeSystemServicesImpl fakeSystemServicesImpl = new FakeSystemServicesImpl();
        TimerServiceImpl timerService = new TimerServiceImpl();

        TranslationService translationService = new TranslationServiceImpl();
        ReflectionTestUtils.setField(translationService, "messageSource", new ConfigWebUiConfig().messageSource());

        CommonServicesContext commonServicesContext = new CommonServicesContext(fakeSystemServicesImpl, new IPPluginServiceImpl(), fakeSystemServicesImpl, timerService, translationService);
        InternalServicesContext internalServicesContext = new InternalServicesContext(fakeSystemServicesImpl, fakeSystemServicesImpl);

        ReflectionTestUtils.setField(fakeSystemServicesImpl, "commonServicesContext", commonServicesContext);
        ReflectionTestUtils.setField(fakeSystemServicesImpl, "internalServicesContext", internalServicesContext);

        ReflectionTestUtils.setField(timerService, "commonServicesContext", commonServicesContext);
        ReflectionTestUtils.setField(timerService, "internalServicesContext", internalServicesContext);
        timerService.init();

        InfraPluginCommonInit.init(commonServicesContext, internalServicesContext);

        return fakeSystemServicesImpl;
    }

}
