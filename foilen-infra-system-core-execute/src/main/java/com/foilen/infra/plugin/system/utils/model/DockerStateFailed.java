/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.foilen.smalltools.tools.AbstractBasics;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerStateFailed extends AbstractBasics {

    private DockerStateIds dockerStateIds;
    private Date lastFail;

    public DockerStateFailed() {
    }

    public DockerStateFailed(DockerStateIds dockerStateIds, Date lastFail) {
        this.dockerStateIds = dockerStateIds;
        this.lastFail = lastFail;
    }

    public DockerStateIds getDockerStateIds() {
        return dockerStateIds;
    }

    public Date getLastFail() {
        return lastFail;
    }

    public void setDockerStateIds(DockerStateIds dockerStateIds) {
        this.dockerStateIds = dockerStateIds;
    }

    public void setLastFail(Date lastFail) {
        this.lastFail = lastFail;
    }

}
