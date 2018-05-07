/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.model;

import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.outputter.docker.DockerContainerOutputContext;
import com.foilen.smalltools.tools.AbstractBasics;

public class ApplicationBuildDetails extends AbstractBasics {

    private DockerContainerOutputContext outputContext;
    private IPApplicationDefinition applicationDefinition;

    public IPApplicationDefinition getApplicationDefinition() {
        return applicationDefinition;
    }

    public DockerContainerOutputContext getOutputContext() {
        return outputContext;
    }

    public ApplicationBuildDetails setApplicationDefinition(IPApplicationDefinition applicationDefinition) {
        this.applicationDefinition = applicationDefinition;
        return this;
    }

    public ApplicationBuildDetails setOutputContext(DockerContainerOutputContext outputContext) {
        this.outputContext = outputContext;
        return this;
    }

}
