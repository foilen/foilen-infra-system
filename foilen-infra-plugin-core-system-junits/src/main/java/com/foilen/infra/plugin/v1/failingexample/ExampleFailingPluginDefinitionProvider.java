/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.failingexample;

import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionProvider;
import com.foilen.infra.plugin.v1.core.plugin.RealmPluginDefinitionV1;

/**
 * An example of failing plugin.
 */
public class ExampleFailingPluginDefinitionProvider implements IPPluginDefinitionProvider {

    @Override
    public RealmPluginDefinitionV1 getRealmPluginDefinition() {
        throw new RuntimeException("FAILING PLUGIN");
    }

    @Override
    public void initialize(CommonServicesContext commonServicesContext) {
    }

}
