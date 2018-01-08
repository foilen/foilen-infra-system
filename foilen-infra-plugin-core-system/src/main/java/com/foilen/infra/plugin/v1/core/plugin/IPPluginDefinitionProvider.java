/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.plugin;

import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;

/**
 * Implement this interface to make your plugin aware from the system.
 */
public interface IPPluginDefinitionProvider {

    IPPluginDefinitionV1 getIPPluginDefinition();

    /**
     * Once the system is ready, it will call this method to let the plugin initialize any helper.
     *
     * @param commonServicesContext
     *            the services
     */
    void initialize(CommonServicesContext commonServicesContext);
}
