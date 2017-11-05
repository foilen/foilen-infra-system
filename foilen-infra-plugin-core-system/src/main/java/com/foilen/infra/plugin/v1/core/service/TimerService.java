/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.service;

import com.foilen.infra.plugin.v1.core.context.TimerEventContext;
import com.foilen.infra.plugin.v1.core.eventhandler.TimerEventHandler;

/**
 * To manage timers.
 */
public interface TimerService {

    /**
     * Something to execute a bit later.
     *
     * @param eventHandler
     *            what to execute
     */
    void executeLater(TimerEventHandler eventHandler);

    void timerAdd(TimerEventContext timer);

}
