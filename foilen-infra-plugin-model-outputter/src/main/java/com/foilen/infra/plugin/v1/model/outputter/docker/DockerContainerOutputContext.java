/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.outputter.docker;

import java.util.HashMap;
import java.util.Map;

public class DockerContainerOutputContext {

    private String imageName;
    private String containerName;
    private String hostName;

    private String buildDirectory;

    // Redirection details
    private Map<String, Integer> redirectPortByMachineContainerEndpoint = new HashMap<>();
    private Map<String, String> redirectIpByMachineContainerEndpoint = new HashMap<>();

    public DockerContainerOutputContext(String imageName, String containerName) {
        this.imageName = imageName;
        this.containerName = containerName;
    }

    public DockerContainerOutputContext(String imageName, String containerName, String hostName) {
        this.imageName = imageName;
        this.containerName = containerName;
        this.hostName = hostName;
    }

    public DockerContainerOutputContext(String imageName, String containerName, String hostName, String buildDirectory) {
        this.imageName = imageName;
        this.containerName = containerName;
        this.hostName = hostName;
        this.buildDirectory = buildDirectory;
    }

    public String getBuildDirectory() {
        return buildDirectory;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getHostName() {
        return hostName;
    }

    public String getImageName() {
        return imageName;
    }

    public Map<String, String> getRedirectIpByMachineContainerEndpoint() {
        return redirectIpByMachineContainerEndpoint;
    }

    public Map<String, Integer> getRedirectPortByMachineContainerEndpoint() {
        return redirectPortByMachineContainerEndpoint;
    }

    public DockerContainerOutputContext setBuildDirectory(String buildDirectory) {
        this.buildDirectory = buildDirectory;
        return this;
    }

    public DockerContainerOutputContext setContainerName(String containerName) {
        this.containerName = containerName;
        return this;
    }

    public DockerContainerOutputContext setHostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    public DockerContainerOutputContext setImageName(String imageName) {
        this.imageName = imageName;
        return this;
    }

    public void setRedirectIpByMachineContainerEndpoint(Map<String, String> redirectIpByMachineContainerEndpoint) {
        this.redirectIpByMachineContainerEndpoint = redirectIpByMachineContainerEndpoint;
    }

    public void setRedirectPortByMachineContainerEndpoint(Map<String, Integer> redirectPortByMachineContainerEndpoint) {
        this.redirectPortByMachineContainerEndpoint = redirectPortByMachineContainerEndpoint;
    }

}
