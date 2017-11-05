/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.redirectportregistry;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.foilen.smalltools.tools.AbstractBasics;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RedirectPortRegistryExits extends AbstractBasics {

    private List<RedirectPortRegistryExit> exits = new ArrayList<>();

    public List<RedirectPortRegistryExit> getExits() {
        return exits;
    }

    public void setExits(List<RedirectPortRegistryExit> exits) {
        this.exits = exits;
    }

}
