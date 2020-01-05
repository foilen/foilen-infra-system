/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.spring;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.foilen.infra.plugin.core.system.common.context.CommonServicesContextBean;
import com.foilen.infra.plugin.core.system.common.context.InternalServicesContextBean;
import com.foilen.infra.plugin.core.system.common.service.IPPluginServiceImpl;
import com.foilen.infra.plugin.core.system.common.service.TimerServiceInExecutorImpl;
import com.foilen.infra.plugin.core.system.common.service.TranslationServiceImpl;
import com.foilen.infra.plugin.core.system.mongodb.service.MessagingServiceMongoDbImpl;
import com.foilen.infra.plugin.core.system.mongodb.service.ResourceServicesInMongoDbImpl;
import com.foilen.infra.plugin.v1.core.common.InfraPluginCommonInit;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.service.IPPluginService;
import com.foilen.infra.plugin.v1.core.service.MessagingService;
import com.foilen.infra.plugin.v1.core.service.TimerService;
import com.foilen.infra.plugin.v1.core.service.TranslationService;

@Configuration
@ComponentScan("com.foilen.infra.plugin.core.system.mongodb.service")
public class ResourceServicesMongoDBSpringConfig {

    @Autowired
    private CommonServicesContext commonServicesContext;
    @Autowired
    private InternalServicesContext internalServicesContext;

    @Bean
    public CommonServicesContext commonServicesContext() {
        return new CommonServicesContextBean();
    }

    @PostConstruct
    public void init() {
        InfraPluginCommonInit.init(commonServicesContext, internalServicesContext);
    }

    @Bean
    public InternalServicesContext internalServicesContext() {
        return new InternalServicesContextBean();
    }

    @Bean
    public IPPluginService ipPluginService() {
        return new IPPluginServiceImpl();
    }

    @Bean
    public MessagingService messagingService() {
        return new MessagingServiceMongoDbImpl();
    }

    @Bean
    public ResourceServicesInMongoDbImpl resourceServices() {
        return new ResourceServicesInMongoDbImpl();
    }

    @Bean
    public TimerService timerService() {
        return new TimerServiceInExecutorImpl();// TODO - MongoDB Specific TimerService
    }

    @Bean
    public TranslationService translationService() {
        return new TranslationServiceImpl();
    }

}
