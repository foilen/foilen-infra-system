/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IPApplicationDefinitionBuildStep {

    private IPApplicationDefinitionBuildStepType type;
    private String step;

    public IPApplicationDefinitionBuildStep() {
    }

    public IPApplicationDefinitionBuildStep(IPApplicationDefinitionBuildStepType type, String step) {
        this.type = type;
        this.step = step;
    }

    public String getStep() {
        return step;
    }

    public IPApplicationDefinitionBuildStepType getType() {
        return type;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public void setType(IPApplicationDefinitionBuildStepType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("IPApplicationDefinitionBuildStep [type=");
        builder.append(type);
        builder.append(", step=");
        builder.append(step);
        builder.append("]");
        return builder.toString();
    }

}
