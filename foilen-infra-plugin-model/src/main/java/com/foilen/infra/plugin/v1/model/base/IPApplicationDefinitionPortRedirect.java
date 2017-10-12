/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IPApplicationDefinitionPortRedirect {

    private Integer localPort;
    private String toMachine;
    private String toInstanceName;
    private String toEndpoint;

    public IPApplicationDefinitionPortRedirect() {
    }

    public IPApplicationDefinitionPortRedirect(Integer localPort, String toMachine, String toInstanceName, String toEndpoint) {
        this.localPort = localPort;
        this.toMachine = toMachine;
        this.toInstanceName = toInstanceName;
        this.toEndpoint = toEndpoint;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public String getToEndpoint() {
        return toEndpoint;
    }

    public String getToInstanceName() {
        return toInstanceName;
    }

    public String getToMachine() {
        return toMachine;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public void setToEndpoint(String toEndpoint) {
        this.toEndpoint = toEndpoint;
    }

    public void setToInstanceName(String toInstanceName) {
        this.toInstanceName = toInstanceName;
    }

    public void setToMachine(String toMachine) {
        this.toMachine = toMachine;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("IPApplicationDefinitionPortRedirect [localPort=");
        builder.append(localPort);
        builder.append(", toMachine=");
        builder.append(toMachine);
        builder.append(", toInstanceName=");
        builder.append(toInstanceName);
        builder.append(", toEndpoint=");
        builder.append(toEndpoint);
        builder.append("]");
        return builder.toString();
    }

}
