/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.haproxy;

import java.util.Map;
import java.util.TreeMap;

import com.foilen.smalltools.tuple.Tuple2;

public class HaProxyConfigPortHttp extends HaProxyConfigPort {

    protected HaProxyConfigPortHttpService defaultService;
    protected Map<String, HaProxyConfigPortHttpService> serviceByHostname = new TreeMap<>();

    @SafeVarargs
    public final void addService(String hostname, Tuple2<String, Integer>... endpointHostPorts) {
        serviceByHostname.put(hostname, new HaProxyConfigPortHttpService(endpointHostPorts));
    }

    public HaProxyConfigPortHttpService getDefaultService() {
        return defaultService;
    }

    public Map<String, HaProxyConfigPortHttpService> getServiceByHostname() {
        return serviceByHostname;
    }

    public void setDefaultService(HaProxyConfigPortHttpService defaultService) {
        this.defaultService = defaultService;
    }

    public void setServiceByHostname(Map<String, HaProxyConfigPortHttpService> serviceByHostname) {
        this.serviceByHostname = serviceByHostname;
    }

}
