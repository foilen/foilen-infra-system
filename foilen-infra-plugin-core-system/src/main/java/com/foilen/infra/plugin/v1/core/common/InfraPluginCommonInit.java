/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.common;

import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;

/**
 * Some common initialization parts of a system.
 */
public class InfraPluginCommonInit {

    public static void init(CommonServicesContext commonServicesContext, InternalServicesContext internalServicesContext) {
        // Load all the plugins
        commonServicesContext.getPluginService().loadPlugins(commonServicesContext, internalServicesContext);
    }

}
