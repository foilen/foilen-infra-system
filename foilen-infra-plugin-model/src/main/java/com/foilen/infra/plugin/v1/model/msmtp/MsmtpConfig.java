/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.msmtp;

public class MsmtpConfig {

    private String hostname;
    private int port = 25;

    public MsmtpConfig() {
    }

    public MsmtpConfig(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SsmtpConfig [hostname=");
        builder.append(hostname);
        builder.append(", port=");
        builder.append(port);
        builder.append("]");
        return builder.toString();
    }

}
