/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InfraLoginConfigDetails {

    private String appId;
    private String baseUrl;

    // Optional if have a trusted certificate
    private String certFile;
    private String certText;

    public String getAppId() {
        return appId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getCertFile() {
        return certFile;
    }

    public String getCertText() {
        return certText;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setCertFile(String certFile) {
        this.certFile = certFile;
    }

    public void setCertText(String certText) {
        this.certText = certText;
    }

}
