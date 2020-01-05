/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;

@SpringBootApplication
public class LaunchWebApp {

    public static void main(String[] args) {
        main(args, LaunchWebApp.class);
    }

    public static void main(String[] args, Class<? extends LaunchWebApp> launchWebAppClass) {

        ConfigurableApplicationContext ctx = SpringApplication.run(launchWebAppClass, args);

        LaunchWebApp launchWebApp = ctx.getBean(launchWebAppClass);
        IPResourceService resourceService = ctx.getBean(IPResourceService.class);
        InternalChangeService internalChangeService = ctx.getBean(InternalChangeService.class);
        launchWebApp.createFakeData(resourceService, internalChangeService);

    }

    protected void createFakeData(IPResourceService resourceService, InternalChangeService internalChangeService) {
    }

}
