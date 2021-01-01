/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.foilen.smalltools.tools.AbstractBasics;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerStateIp extends AbstractBasics {

    private String ip;
    private Date lastUsed;

    public String getIp() {
        return ip;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public DockerStateIp setIp(String ip) {
        this.ip = ip;
        return this;
    }

    public DockerStateIp setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
        return this;
    }

}
