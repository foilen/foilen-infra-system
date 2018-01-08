/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.foilen.smalltools.JavaEnvironmentValues;
import com.foilen.smalltools.tools.AbstractBasics;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IPApplicationDefinitionPortRedirect extends AbstractBasics {

    public static final String LOCAL_MACHINE = "localhost";

    private Integer localPort;
    private String toMachine;
    private String toContainerName;
    private String toEndpoint;

    public IPApplicationDefinitionPortRedirect() {
    }

    public IPApplicationDefinitionPortRedirect(Integer localPort, String toMachine, String toContainerName, String toEndpoint) {
        this.localPort = localPort;
        this.toMachine = toMachine;
        this.toContainerName = toContainerName;
        this.toEndpoint = toEndpoint;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public String getMachineContainerEndpoint() {
        return toMachine + "/" + toContainerName + "/" + toEndpoint;
    }

    public String getToContainerName() {
        return toContainerName;
    }

    public String getToEndpoint() {
        return toEndpoint;
    }

    public String getToMachine() {
        return toMachine;
    }

    public boolean isToLocalMachine() {
        return IPApplicationDefinitionPortRedirect.LOCAL_MACHINE.equals(toMachine) || JavaEnvironmentValues.getHostName().equals(toMachine);
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public void setToContainerName(String toInstanceName) {
        this.toContainerName = toInstanceName;
    }

    public void setToEndpoint(String toEndpoint) {
        this.toEndpoint = toEndpoint;
    }

    public void setToMachine(String toMachine) {
        this.toMachine = toMachine;
    }

}
