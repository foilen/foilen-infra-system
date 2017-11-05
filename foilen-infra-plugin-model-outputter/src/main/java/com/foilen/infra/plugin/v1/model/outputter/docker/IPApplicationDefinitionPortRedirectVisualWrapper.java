/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.outputter.docker;

import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionPortRedirect;
import com.foilen.smalltools.reflection.ReflectionTools;

public class IPApplicationDefinitionPortRedirectVisualWrapper extends IPApplicationDefinitionPortRedirect {

    public IPApplicationDefinitionPortRedirectVisualWrapper(IPApplicationDefinitionPortRedirect applicationDefinitionPortRedirect) {
        ReflectionTools.copyAllProperties(applicationDefinitionPortRedirect, this);
    }

    public String getLocalPortHex() {
        Integer localPort = getLocalPort();
        if (localPort == null) {
            return null;
        }

        return String.format("%04X", localPort);
    }

}
