/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.junits;

public class ResourcesStateLink {

    private String type;
    private String to;

    public ResourcesStateLink(String type, String to) {
        this.type = type;
        this.to = to;
    }

    public String getTo() {
        return to;
    }

    public String getType() {
        return type;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setType(String type) {
        this.type = type;
    }

}
