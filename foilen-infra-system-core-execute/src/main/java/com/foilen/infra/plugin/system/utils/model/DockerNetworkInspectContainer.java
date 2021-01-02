/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.foilen.smalltools.tools.AbstractBasics;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerNetworkInspectContainer extends AbstractBasics {

    @JsonProperty("Name")
    private String name;
    @JsonProperty("IPv4Address")
    private String ipv4;

    public String getIpv4() {
        return ipv4;
    }

    public String getName() {
        return name;
    }

    public void setIpv4(String ipv4) {
        this.ipv4 = ipv4;
    }

    public void setName(String name) {
        this.name = name;
    }

}
