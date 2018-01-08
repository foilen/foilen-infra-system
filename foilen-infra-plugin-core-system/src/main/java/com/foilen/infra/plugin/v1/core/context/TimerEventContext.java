/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.context;

import java.util.Calendar;

import com.foilen.infra.plugin.v1.core.eventhandler.TimerEventHandler;

public class TimerEventContext {

    private String timerName;
    private int calendarUnit;
    private int deltaTime;

    private boolean oneTime = false;
    private boolean startWhenFirstCreated = false;

    private TimerEventHandler timerEventHandler;

    /**
     * A recurrent timer.
     *
     * @param timerEventHandler
     *            the handler
     * @param timerName
     *            the name of the timer
     * @param calendarUnit
     *            the unit of the delta that is a constant on {@link Calendar}
     * @param deltaTime
     *            the delta between events
     */
    public TimerEventContext(TimerEventHandler timerEventHandler, String timerName, int calendarUnit, int deltaTime) {
        this.timerEventHandler = timerEventHandler;
        this.timerName = timerName;
        this.calendarUnit = calendarUnit;
        this.deltaTime = deltaTime;
    }

    public TimerEventContext(TimerEventHandler timerEventHandler, String timerName, int calendarUnit, int deltaTime, boolean oneTime, boolean startWhenFirstCreated) {
        this.timerEventHandler = timerEventHandler;
        this.timerName = timerName;
        this.calendarUnit = calendarUnit;
        this.deltaTime = deltaTime;
        this.oneTime = oneTime;
        this.startWhenFirstCreated = startWhenFirstCreated;
    }

    public int getCalendarUnit() {
        return calendarUnit;
    }

    public String getCalendarUnitInText() {
        switch (calendarUnit) {
        case Calendar.MILLISECOND:
            return "MILLISECOND";
        case Calendar.SECOND:
            return "SECOND";
        case Calendar.MINUTE:
            return "MINUTE";
        case Calendar.HOUR:
        case Calendar.HOUR_OF_DAY:
            return "HOURLY";
        case Calendar.DAY_OF_MONTH:
        case Calendar.DAY_OF_WEEK:
        case Calendar.DAY_OF_WEEK_IN_MONTH:
        case Calendar.DAY_OF_YEAR:
            return "DAILY";
        case Calendar.WEEK_OF_MONTH:
        case Calendar.WEEK_OF_YEAR:
            return "WEEKLY";
        case Calendar.MONTH:
            return "MONTLY";
        case Calendar.YEAR:
            return "YEARLY";
        default:
            return "Unknown:  " + calendarUnit;
        }
    }

    public int getDeltaTime() {
        return deltaTime;
    }

    public TimerEventHandler getTimerEventHandler() {
        return timerEventHandler;
    }

    public String getTimerName() {
        return timerName;
    }

    public boolean isOneTime() {
        return oneTime;
    }

    public boolean isStartWhenFirstCreated() {
        return startWhenFirstCreated;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TimerEventContext [timerName=");
        builder.append(timerName);
        builder.append(", calendarUnit=");
        builder.append(getCalendarUnitInText());
        builder.append(", deltaTime=");
        builder.append(deltaTime);
        builder.append("]");
        return builder.toString();
    }

}
