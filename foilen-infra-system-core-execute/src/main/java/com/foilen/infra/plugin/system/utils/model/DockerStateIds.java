/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.foilen.smalltools.tools.AbstractBasics;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerStateIds extends AbstractBasics {

    private String imageUniqueId;
    private String containerRunUniqueId;
    private String containerStartedUniqueId;

    private DockerStep lastState = DockerStep.BUILD_IMAGE;

    public DockerStateIds() {
    }

    public DockerStateIds(String imageUniqueId, String containerRunUniqueId, String containerStartedUniqueId) {
        this.imageUniqueId = imageUniqueId;
        this.containerRunUniqueId = containerRunUniqueId;
        this.containerStartedUniqueId = containerStartedUniqueId;
    }

    public String getContainerRunUniqueId() {
        return containerRunUniqueId;
    }

    public String getContainerStartedUniqueId() {
        return containerStartedUniqueId;
    }

    public String getImageUniqueId() {
        return imageUniqueId;
    }

    public DockerStep getLastState() {
        return lastState;
    }

    public void setContainerRunUniqueId(String containerRunUniqueId) {
        this.containerRunUniqueId = containerRunUniqueId;
    }

    public void setContainerStartedUniqueId(String containerStartedUniqueId) {
        this.containerStartedUniqueId = containerStartedUniqueId;
    }

    public void setImageUniqueId(String imageUniqueId) {
        this.imageUniqueId = imageUniqueId;
    }

    public void setLastState(DockerStep lastState) {
        this.lastState = lastState;
    }

}
