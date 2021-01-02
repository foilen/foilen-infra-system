/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.foilen.smalltools.tools.AbstractBasics;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerNetworkInspect extends AbstractBasics {

    @JsonProperty("Name")
    private String name;
    @JsonProperty("Containers")
    private Map<String, DockerNetworkInspectContainer> containers = new HashMap<>();

    public Map<String, DockerNetworkInspectContainer> getContainers() {
        return containers;
    }

    public String getName() {
        return name;
    }

    public void setContainers(Map<String, DockerNetworkInspectContainer> containers) {
        this.containers = containers;
    }

    public void setName(String name) {
        this.name = name;
    }

}
