/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.eventhandler;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.TimerEventContext;

/**
 * To handle a time triggered event.
 */
public interface TimerEventHandler {

    /**
     * The handler.
     *
     * @param services
     *            the services you can use.
     * @param changes
     *            the context where to add the changes you want to do to resources.
     * @param event
     *            the event that triggered
     */
    void timerHandler(CommonServicesContext services, ChangesContext changes, TimerEventContext event);

}
