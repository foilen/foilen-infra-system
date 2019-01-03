/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.app.test.docker;

import java.io.File;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.foilen.infra.plugin.core.system.common.service.IPPluginServiceImpl;
import com.foilen.infra.plugin.core.system.fake.CommonServicesContextBean;
import com.foilen.infra.plugin.core.system.fake.InitSystemBean;
import com.foilen.infra.plugin.core.system.fake.InternalServicesContextBean;
import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.reflection.ReflectionTools;
import com.foilen.smalltools.tools.CharsetTools;
import com.foilen.smalltools.tools.DirectoryTools;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.LogbackTools;

@Configuration
public class CreateSampleResourcesApp {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("You need to provide a directory where to output all the templates");
            System.exit(1);
        }

        // Create the directory if missing
        String outputDirectoryName = args[0];
        File outputFolder = new File(outputDirectoryName);
        if (outputFolder.exists()) {
            if (!outputFolder.isDirectory()) {
                System.out.println("The path " + outputDirectoryName + " is not a directory");
                System.exit(1);
            }
        } else {
            if (!outputFolder.mkdir()) {
                System.out.println("Could not create " + outputDirectoryName + " directory");
                System.exit(1);
            }
        }

        // Start the plugin service
        LogbackTools.changeConfig("/logback-quiet.xml");
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(CreateSampleResourcesApp.class);
        applicationContext.register(CommonServicesContextBean.class);
        applicationContext.register(InitSystemBean.class);
        applicationContext.register(InternalServicesContextBean.class);
        applicationContext.register(IPPluginServiceImpl.class);
        applicationContext.scan("com.foilen.infra.plugin.core.system.fake.service");
        applicationContext.refresh();

        // Export a sample of all the resource types
        IPResourceService resourceService = applicationContext.getBean(IPResourceService.class);
        for (IPResourceDefinition resourceDefinition : resourceService.getResourceDefinitions()) {
            String resourceType = resourceDefinition.getResourceType();
            System.out.println("Exporting " + resourceType);
            IPResource resource = ReflectionTools.instantiate(resourceDefinition.getResourceClass());
            String resourceDirectory = outputDirectoryName + "/" + resourceType;
            DirectoryTools.createPath(resourceDirectory);
            JsonTools.writeToFile(resourceDirectory + "/_template.json", resource);
        }

        // Create sample tags and links
        FileTools.writeFile("r1;t1\nr1;t2\n", outputDirectoryName + "/tags.txt");
        FileTools.writeFile("r1;INSTALLED_ON;m1\nr2;USES;r1\n", outputDirectoryName + "/links.txt");

        // End
        applicationContext.close();
    }

    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.addBasenames("classpath:/WEB-INF/webui/messages/messages");

        messageSource.setCacheSeconds(60);
        messageSource.setDefaultEncoding(CharsetTools.UTF_8.name());
        messageSource.setUseCodeAsDefaultMessage(true);

        return messageSource;
    }

}
