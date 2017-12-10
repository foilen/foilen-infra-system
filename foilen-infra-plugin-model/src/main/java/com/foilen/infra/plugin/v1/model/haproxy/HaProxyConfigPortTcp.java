/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.haproxy;

import java.util.Set;
import java.util.TreeSet;

import com.foilen.smalltools.tuple.Tuple2;

public class HaProxyConfigPortTcp extends HaProxyConfigPort {

    protected Set<String> endpointHostPorts = new TreeSet<>();

    @SafeVarargs
    public HaProxyConfigPortTcp(Tuple2<String, Integer>... endpointHostPorts) {
        addEndpoints(endpointHostPorts);
    }

    @SafeVarargs
    public final void addEndpoints(Tuple2<String, Integer>... endpointHostPorts) {
        for (Tuple2<String, Integer> endpointHostPort : endpointHostPorts) {
            String host = endpointHostPort.getA();
            if (host == null) {
                host = "192.168.255.1";
            }
            this.endpointHostPorts.add(host + ":" + endpointHostPort.getB());
        }
    }

    public Set<String> getEndpointHostPorts() {
        return endpointHostPorts;
    }

    public void setEndpointHostPorts(Set<String> endpointHostPorts) {
        this.endpointHostPorts = endpointHostPorts;
    }

}
