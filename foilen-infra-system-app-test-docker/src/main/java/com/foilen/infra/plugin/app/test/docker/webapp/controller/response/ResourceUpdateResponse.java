/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.app.test.docker.webapp.controller.response;

import java.util.HashMap;
import java.util.Map;

import com.foilen.infra.plugin.v1.model.resource.IPResource;

public class ResourceUpdateResponse {

    private IPResource successResource;
    private String successResourceId;
    private String topError = "";
    private Map<String, String> fieldsValues = new HashMap<>();
    private Map<String, String> fieldsErrors = new HashMap<>();

    public Map<String, String> getFieldsErrors() {
        return fieldsErrors;
    }

    public Map<String, String> getFieldsValues() {
        return fieldsValues;
    }

    public IPResource getSuccessResource() {
        return successResource;
    }

    public String getSuccessResourceId() {
        return successResourceId;
    }

    public String getTopError() {
        return topError;
    }

    public void setFieldsErrors(Map<String, String> fieldsErrors) {
        this.fieldsErrors = fieldsErrors;
    }

    public void setFieldsValues(Map<String, String> fieldsValues) {
        this.fieldsValues = fieldsValues;
    }

    public void setSuccessResource(IPResource successResource) {
        this.successResource = successResource;
    }

    public void setSuccessResourceId(String successResourceId) {
        this.successResourceId = successResourceId;
    }

    public void setTopError(String topError) {
        this.topError = topError;
    }

}
