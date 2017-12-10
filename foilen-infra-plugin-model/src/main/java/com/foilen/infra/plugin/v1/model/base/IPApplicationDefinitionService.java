/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.foilen.smalltools.tools.AbstractBasics;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IPApplicationDefinitionService extends AbstractBasics {

    private String name;
    private String workingDirectory = null;
    private String command;
    private Integer runAs = null;

    public IPApplicationDefinitionService() {
    }

    public IPApplicationDefinitionService(String name, String command) {
        this.name = name;
        this.command = command;
    }

    public IPApplicationDefinitionService(String name, String command, Integer runAs) {
        this.name = name;
        this.command = command;
        this.runAs = runAs;
    }

    public String getCommand() {
        return command;
    }

    public String getName() {
        return name;
    }

    public Integer getRunAs() {
        return runAs;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRunAs(Integer runAs) {
        this.runAs = runAs;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

}
