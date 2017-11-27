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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedirectPortRegistryEntries {

    private List<RedirectPortRegistryEntry> entries = new ArrayList<>();

    public List<RedirectPortRegistryEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<RedirectPortRegistryEntry> entries) {
        this.entries = entries;
    }

}
