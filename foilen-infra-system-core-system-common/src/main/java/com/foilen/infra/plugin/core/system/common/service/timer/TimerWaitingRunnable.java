/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.common.service.timer;

import java.util.Date;
import java.util.concurrent.ExecutorService;

import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.TimerEventContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.DateTools;
import com.foilen.smalltools.tools.ThreadTools;

public class TimerWaitingRunnable extends AbstractBasics implements Runnable {

    private CommonServicesContext commonServicesContext;
    private InternalServicesContext internalServicesContext;

    private ExecutorService executingExecutorService;
    private TimerEventContext timer;

    public TimerWaitingRunnable(CommonServicesContext commonServicesContext, InternalServicesContext internalServicesContext, ExecutorService executingExecutorService, TimerEventContext timer) {
        this.commonServicesContext = commonServicesContext;
        this.internalServicesContext = internalServicesContext;
        this.executingExecutorService = executingExecutorService;
        this.timer = timer;
    }

    @Override
    public void run() {

        // Change the thread name
        ThreadTools.nameThread().setSeparator("/").clear() //
                .appendText("Timer").appendText(timer.getTimerName()) //
                .change();

        logger.debug("Start");

        boolean ranOnce = false;
        if (timer.isStartWhenFirstCreated()) {
            executingExecutorService.submit(new TimerExecutionRunnable(commonServicesContext, internalServicesContext, timer));
            ranOnce = true;
        }

        try {

            while (!timer.isOneTime() // Frequent run
                    || !ranOnce // Run once only
            ) {
                // Check delay before running
                Date now = new Date();
                Date nextRun = DateTools.addDate(now, timer.getCalendarUnit(), timer.getDeltaTime());

                // Waiting
                long waitFor = nextRun.getTime() - now.getTime();
                logger.debug("Wait for {} ms", waitFor);
                ThreadTools.sleep(waitFor);

                // Execute
                executingExecutorService.submit(new TimerExecutionRunnable(commonServicesContext, internalServicesContext, timer));
                ranOnce = true;
            }

        } catch (Exception e) {
            logger.error("Problem waiting", e);
        }

        logger.debug("Done");
    }

}
