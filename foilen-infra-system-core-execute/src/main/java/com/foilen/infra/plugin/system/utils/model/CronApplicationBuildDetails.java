/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.model;

public class CronApplicationBuildDetails extends ApplicationBuildDetails {

    private String cronTime;

    public String getCronTime() {
        return cronTime;
    }

    public CronApplicationBuildDetails setCronTime(String cronTime) {
        this.cronTime = cronTime;
        return this;
    }

}
