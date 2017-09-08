/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.outputter.docker;

public class DockerContainerOutputContext {

    private String imageName;
    private String instanceName;
    private String hostName;

    public DockerContainerOutputContext(String imageName, String instanceName, String hostName) {
        this.imageName = imageName;
        this.instanceName = instanceName;
        this.hostName = hostName;
    }

    public String getHostName() {
        return hostName;
    }

    public String getImageName() {
        return imageName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

}
