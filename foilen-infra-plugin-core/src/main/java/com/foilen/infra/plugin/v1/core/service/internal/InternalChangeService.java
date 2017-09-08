/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.service.internal;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;

/**
 * To manage the changes.
 */
public interface InternalChangeService {

    /**
     * Execute the changes and clear the changes context.
     *
     * @param changes
     *            the changes
     */
    void changesExecute(ChangesContext changes);

}
