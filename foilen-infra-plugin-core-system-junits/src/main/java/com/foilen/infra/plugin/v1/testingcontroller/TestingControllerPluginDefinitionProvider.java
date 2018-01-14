/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.testingcontroller;

import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionProvider;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionV1;

/**
 * An example of failing plugin.
 */
public class TestingControllerPluginDefinitionProvider implements IPPluginDefinitionProvider {

    static private TestingControllerPluginDefinitionProvider instance;

    public static TestingControllerPluginDefinitionProvider getInstance() {
        return instance;
    }

    private TestingControllerInfiniteLoopUpdateHander testingControllerInfiniteLoopUpdateHander = new TestingControllerInfiniteLoopUpdateHander();
    private TestingControllerMockUpdateHander testingControllerMockUpdateHander = new TestingControllerMockUpdateHander();

    public TestingControllerPluginDefinitionProvider() {
        instance = this;
    }

    @Override
    public IPPluginDefinitionV1 getIPPluginDefinition() {

        IPPluginDefinitionV1 pluginDefinitionV1 = new IPPluginDefinitionV1("Foilen", "Testing Controller", "To do some specific tests", "1.0.0");
        pluginDefinitionV1.addUpdateHandler(testingControllerInfiniteLoopUpdateHander);
        pluginDefinitionV1.addUpdateHandler(testingControllerMockUpdateHander);
        return pluginDefinitionV1;
    }

    public TestingControllerInfiniteLoopUpdateHander getTestingControllerInfiniteLoopUpdateHander() {
        return testingControllerInfiniteLoopUpdateHander;
    }

    public TestingControllerMockUpdateHander getTestingControllerMockUpdateHander() {
        return testingControllerMockUpdateHander;
    }

    @Override
    public void initialize(CommonServicesContext commonServicesContext) {
    }

    public void setTestingControllerUpdateHander(TestingControllerInfiniteLoopUpdateHander testingControllerUpdateHander) {
        this.testingControllerInfiniteLoopUpdateHander = testingControllerUpdateHander;
    }

}
