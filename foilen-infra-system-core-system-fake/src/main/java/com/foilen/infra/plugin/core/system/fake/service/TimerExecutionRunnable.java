/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.service;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.TimerEventContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;

public class TimerExecutionRunnable implements Runnable {

    private CommonServicesContext commonServicesContext;
    private InternalServicesContext internalServicesContext;

    private TimerEventContext timer;

    public TimerExecutionRunnable(CommonServicesContext commonServicesContext, InternalServicesContext internalServicesContext, TimerEventContext timer) {
        this.commonServicesContext = commonServicesContext;
        this.internalServicesContext = internalServicesContext;
        this.timer = timer;
    }

    @Override
    public void run() {
        ChangesContext changes = new ChangesContext(commonServicesContext.getResourceService());
        timer.getTimerEventHandler().timerHandler(commonServicesContext, changes, timer);
        internalServicesContext.getInternalChangeService().changesExecute(changes);
    }
}