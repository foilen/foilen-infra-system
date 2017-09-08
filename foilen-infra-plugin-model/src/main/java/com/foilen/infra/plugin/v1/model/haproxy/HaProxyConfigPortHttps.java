/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.haproxy;

public class HaProxyConfigPortHttps extends HaProxyConfigPortHttp {

    protected String certificatesDirectory;

    public HaProxyConfigPortHttps() {
    }

    public HaProxyConfigPortHttps(String certificatesDirectory) {
        this.certificatesDirectory = certificatesDirectory;
    }

    public String getCertificatesDirectory() {
        return certificatesDirectory;
    }

    public void setCertificatesDirectory(String certificatesDirectory) {
        this.certificatesDirectory = certificatesDirectory;
    }

}
