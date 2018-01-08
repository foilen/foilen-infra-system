/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.example;

import java.util.Arrays;
import java.util.Calendar;

import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionProvider;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionV1;
import com.foilen.infra.plugin.v1.example.form.EmployeeResourceRawForm;
import com.foilen.infra.plugin.v1.example.form.EmployeeResourceSimpleForm;
import com.foilen.infra.plugin.v1.example.resource.EmployeeResource;
import com.foilen.infra.plugin.v1.example.resource.Ex1Resource;

/**
 * An example plugin.
 */
public class ExamplePluginDefinitionProvider implements IPPluginDefinitionProvider {

    @Override
    public IPPluginDefinitionV1 getIPPluginDefinition() {

        // Description
        IPPluginDefinitionV1 pluginDefinitionV1 = new IPPluginDefinitionV1("Infra", "Example", "An example of a plugin", "1.0.0");

        // Timers
        LoggingTimerEventHandler loggingTimerEventHandler = new LoggingTimerEventHandler();
        pluginDefinitionV1.addTimer(loggingTimerEventHandler, "20 seconds timer", Calendar.SECOND, 20);
        pluginDefinitionV1.addTimer(loggingTimerEventHandler, "1 minute timer", Calendar.MINUTE, 1);

        // Resources
        pluginDefinitionV1.addCustomResource(Ex1Resource.class, "Exemple 1", //
                Arrays.asList("name"), //
                Arrays.asList("year"));
        pluginDefinitionV1.addCustomResource(EmployeeResource.class, "Employee", //
                Arrays.asList("firstName", "lastName"), //
                Arrays.asList("birthday"));

        // Resource page provider
        pluginDefinitionV1.addResourceEditor(new EmployeeResourceRawForm(), "EmployeeResourceRawForm");
        pluginDefinitionV1.addResourceEditor(new EmployeeResourceSimpleForm(), "EmployeeResourceSimpleForm");

        // Translation
        pluginDefinitionV1.addTranslations("/com/foilen/infra/plugin/v1/example/messages");

        return pluginDefinitionV1;
    }

    @Override
    public void initialize(CommonServicesContext commonServicesContext) {
    }

}
