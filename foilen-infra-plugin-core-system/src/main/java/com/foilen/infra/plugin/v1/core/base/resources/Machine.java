/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.resources;

import com.foilen.infra.plugin.v1.model.resource.AbstractIPResource;
import com.foilen.infra.plugin.v1.model.resource.InfraPluginResourceCategory;
import com.google.common.collect.ComparisonChain;

/**
 * This is a machine that FoilenCloud is managing. <br/>
 * Links to:
 * <ul>
 * </ul>
 *
 * Manages:
 * <ul>
 * <li>{@link DnsEntry}: The A and AAAA entries with the public ip</li>
 * </ul>
 */
public class Machine extends AbstractIPResource implements Comparable<Machine> {

    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_PUBLIC_IP = "publicIp";

    private String name;
    private String publicIp;

    public Machine() {
    }

    public Machine(String name) {
        this.name = name;
    }

    public Machine(String name, String publicIp) {
        this.name = name;
        this.publicIp = publicIp;
    }

    @Override
    public int compareTo(Machine o) {
        return ComparisonChain.start() //
                .compare(this.name, o.name) //
                .result();
    }

    public String getName() {
        return name;
    }

    public String getPublicIp() {
        return publicIp;
    }

    @Override
    public InfraPluginResourceCategory getResourceCategory() {
        return InfraPluginResourceCategory.INFRASTRUCTURE;
    }

    @Override
    public String getResourceDescription() {
        if (publicIp == null) {
            return name;
        } else {
            return name + " (" + publicIp + ")";
        }
    }

    @Override
    public String getResourceName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPublicIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            this.publicIp = null;
        } else {
            this.publicIp = ip;
        }
    }

}
