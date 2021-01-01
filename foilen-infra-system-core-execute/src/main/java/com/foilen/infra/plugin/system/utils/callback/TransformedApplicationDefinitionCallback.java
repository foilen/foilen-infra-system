/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.callback;

import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;

public interface TransformedApplicationDefinitionCallback {

    /**
     * The callback.
     *
     * @param applicationName
     *            the application name
     * @param applicationDefinition
     *            the transformed application definition
     */
    void handler(String applicationName, IPApplicationDefinition applicationDefinition);

}
