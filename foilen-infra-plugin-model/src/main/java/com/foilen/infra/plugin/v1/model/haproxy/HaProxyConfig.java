/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.haproxy;

import java.util.Map;
import java.util.TreeMap;

import com.foilen.infra.plugin.v1.model.ModelsException;
import com.foilen.smalltools.tools.StringTools;
import com.foilen.smalltools.tuple.Tuple2;

public class HaProxyConfig {

    private boolean daemon = false;

    private String chroot = null;
    private String user = "haproxy";
    private String group = "haproxy";

    private long timeoutConnectionMs = 5000L;
    private long timeoutClientMs = 50000L;
    private long timeoutServerMs = 50000L;

    private String pidfile = null;

    private Map<Integer, HaProxyConfigPort> ports = new TreeMap<>();

    public HaProxyConfigPortHttp addPortHttp(int port) {
        HaProxyConfigPort configPort = ports.get(port);
        if (configPort != null) {
            if (configPort instanceof HaProxyConfigPortHttp) {
                return (HaProxyConfigPortHttp) configPort;
            } else {
                throw new ModelsException("Port " + port + " is already used for another type of port");
            }
        }
        HaProxyConfigPortHttp configPortHttp = new HaProxyConfigPortHttp();
        ports.put(port, configPortHttp);
        return configPortHttp;
    }

    public HaProxyConfigPortHttps addPortHttps(int port, String certificatesDirectory) {
        HaProxyConfigPort configPort = ports.get(port);
        if (configPort != null) {
            if (configPort instanceof HaProxyConfigPortHttps) {
                HaProxyConfigPortHttps configPortHttps = (HaProxyConfigPortHttps) configPort;
                if (!StringTools.safeEquals(configPortHttps.getCertificatesDirectory(), certificatesDirectory)) {
                    throw new ModelsException("Port " + port + " is already used for HTTPS, but with anothercertificate folder");
                }
                return configPortHttps;
            } else {
                throw new ModelsException("Port " + port + " is already used for another type of port");
            }
        }
        HaProxyConfigPortHttps configPortHttps = new HaProxyConfigPortHttps(certificatesDirectory);
        ports.put(port, configPortHttps);
        return configPortHttps;
    }

    @SafeVarargs
    public final HaProxyConfigPortTcp addPortTcp(int port, Tuple2<String, Integer>... endpointHostPorts) {
        HaProxyConfigPort configPort = ports.get(port);
        if (configPort != null) {
            if (configPort instanceof HaProxyConfigPortTcp) {
                HaProxyConfigPortTcp configPortTcp = (HaProxyConfigPortTcp) configPort;
                configPortTcp.addEndpoints(endpointHostPorts);
                return configPortTcp;
            } else {
                throw new ModelsException("Port " + port + " is already used for another type of port");
            }
        }
        HaProxyConfigPortTcp configPortTcp = new HaProxyConfigPortTcp(endpointHostPorts);
        ports.put(port, configPortTcp);
        return configPortTcp;
    }

    public String getChroot() {
        return chroot;
    }

    public String getGroup() {
        return group;
    }

    public String getPidfile() {
        return pidfile;
    }

    public Map<Integer, HaProxyConfigPort> getPorts() {
        return ports;
    }

    public long getTimeoutClientMs() {
        return timeoutClientMs;
    }

    public long getTimeoutConnectionMs() {
        return timeoutConnectionMs;
    }

    public long getTimeoutServerMs() {
        return timeoutServerMs;
    }

    public String getUser() {
        return user;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setChroot(String chroot) {
        this.chroot = chroot;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setPidfile(String pidfile) {
        this.pidfile = pidfile;
    }

    public void setPorts(Map<Integer, HaProxyConfigPort> ports) {
        this.ports = ports;
    }

    public void setTimeoutClientMs(long timeoutClientMs) {
        this.timeoutClientMs = timeoutClientMs;
    }

    public void setTimeoutConnectionMs(long timeoutConnectionMs) {
        this.timeoutConnectionMs = timeoutConnectionMs;
    }

    public void setTimeoutServerMs(long timeoutServerMs) {
        this.timeoutServerMs = timeoutServerMs;
    }

    public void setUser(String user) {
        this.user = user;
    }

}
