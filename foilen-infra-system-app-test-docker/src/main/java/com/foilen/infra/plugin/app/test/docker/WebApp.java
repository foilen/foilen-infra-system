/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.app.test.docker;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.foilen.infra.plugin.core.system.junits.JunitsHelper;
import com.foilen.infra.plugin.core.system.junits.ResourcesDump;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.LogbackTools;

@SpringBootApplication
@ComponentScan("com.foilen.infra.plugin.core.system.fake")
public class WebApp {

    public static void main(String[] args) {
        main(args, WebApp.class);
    }

    public static void main(String[] args, Class<? extends WebApp> webAppClass) {

        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        // Check if debug mode
        boolean isDebug = false;
        boolean isInfo = false;
        if (arguments.remove("--debug")) {
            isDebug = true;
        }
        if (arguments.remove("--info")) {
            isInfo = true;
        }

        // Start app
        ConfigurableApplicationContext ctx = SpringApplication.run(webAppClass, arguments.toArray(new String[arguments.size()]));

        // Configure loggers
        if (isDebug) {
            LogbackTools.changeConfig("/logback-debug.xml");
        } else if (isInfo) {
            LogbackTools.changeConfig("/logback-info.xml");
        } else {
            LogbackTools.changeConfig("/logback-quiet.xml");
        }

        WebApp webApp = ctx.getBean(webAppClass);

        IPResourceService resourceService = ctx.getBean(IPResourceService.class);
        InternalChangeService internalChangeService = ctx.getBean(InternalChangeService.class);
        webApp.createFakeData(arguments, ctx, resourceService, internalChangeService);

    }

    protected void createFakeData(List<String> paths, ConfigurableApplicationContext ctx, IPResourceService resourceService, InternalChangeService internalChangeService) {

        for (String path : paths) {

            // Check if directory or file mode
            boolean isFile = false;
            File inputFileOrDirectory = new File(path);
            if (inputFileOrDirectory.exists()) {
                isFile = !inputFileOrDirectory.isDirectory();
                if (isFile) {
                    System.out.println("Reading file " + path);
                } else {
                    System.out.println("Reading directory " + path);
                }
            } else {
                System.out.println("The file or directory " + path + " does not exist");
                System.exit(1);
            }

            // Import from directory or file
            if (isFile) {
                CommonServicesContext commonServicesContext = ctx.getBean(CommonServicesContext.class);
                InternalServicesContext internalServicesContext = ctx.getBean(InternalServicesContext.class);
                ResourcesDump resourcesDump = JsonTools.readFromFile(inputFileOrDirectory, ResourcesDump.class);
                JunitsHelper.dumpImport(commonServicesContext, internalServicesContext, resourcesDump);
            } else {
                StartResourcesApp.importFromDirectory(inputFileOrDirectory, ctx);
            }
        }

    }

}
