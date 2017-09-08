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
        HaProxyConfigPortHttp configPort = new HaProxyConfigPortHttp();
        ports.put(port, configPort);
        return configPort;
    }

    public HaProxyConfigPortHttps addPortHttps(int port, String certificatesDirectory) {
        HaProxyConfigPortHttps configPort = new HaProxyConfigPortHttps(certificatesDirectory);
        ports.put(port, configPort);
        return configPort;
    }

    @SafeVarargs
    public final HaProxyConfigPortTcp addPortTcp(int port, Tuple2<String, Integer>... endpointHostPorts) {
        HaProxyConfigPortTcp configPort = new HaProxyConfigPortTcp(endpointHostPorts);
        ports.put(port, configPort);
        return configPort;
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
