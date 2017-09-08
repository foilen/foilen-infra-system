/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.controller.response;

import java.util.HashMap;
import java.util.Map;

public class ResourceUpdateResponse {

    private Long successResourceId;
    private String topError = "";
    private Map<String, String> fieldsValues = new HashMap<>();
    private Map<String, String> fieldsErrors = new HashMap<>();

    public Map<String, String> getFieldsErrors() {
        return fieldsErrors;
    }

    public Map<String, String> getFieldsValues() {
        return fieldsValues;
    }

    public Long getSuccessResourceId() {
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

    public void setSuccessResourceId(Long successResourceId) {
        this.successResourceId = successResourceId;
    }

    public void setTopError(String topError) {
        this.topError = topError;
    }

}
