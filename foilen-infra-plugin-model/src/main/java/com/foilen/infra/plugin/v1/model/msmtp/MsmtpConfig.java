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
    private String username;
    private String password;

    public MsmtpConfig() {
    }

    public MsmtpConfig(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public MsmtpConfig setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public MsmtpConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public MsmtpConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public MsmtpConfig setUsername(String username) {
        this.username = username;
        return this;
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
