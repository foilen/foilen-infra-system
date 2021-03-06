/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.model;

import java.util.ArrayList;
import java.util.List;

import com.foilen.infra.plugin.system.utils.callback.DockerContainerManagementCallback;
import com.foilen.infra.plugin.system.utils.callback.NoOpDockerContainerManagementCallback;
import com.foilen.infra.plugin.system.utils.callback.NoOpTransformedApplicationDefinitionCallback;
import com.foilen.infra.plugin.system.utils.callback.TransformedApplicationDefinitionCallback;

public class ContainersManageContext {

    private static final NoOpDockerContainerManagementCallback NO_OP_DOCKER_CONTAINER_MANAGEMENT_CALLBACK = new NoOpDockerContainerManagementCallback();
    private static final NoOpTransformedApplicationDefinitionCallback NO_OP_TRANSFORMED_APPLICATION_DEFINITION_CALLBACK = new NoOpTransformedApplicationDefinitionCallback();

    private String baseOutputDirectory;
    private DockerState dockerState;
    private List<ApplicationBuildDetails> applicationBuildDetails = new ArrayList<>();

    private DockerContainerManagementCallback containerManagementCallback = NO_OP_DOCKER_CONTAINER_MANAGEMENT_CALLBACK;
    private TransformedApplicationDefinitionCallback transformedApplicationDefinitionCallback = NO_OP_TRANSFORMED_APPLICATION_DEFINITION_CALLBACK;

    public List<ApplicationBuildDetails> getApplicationBuildDetails() {
        return applicationBuildDetails;
    }

    public String getBaseOutputDirectory() {
        return baseOutputDirectory;
    }

    public DockerContainerManagementCallback getContainerManagementCallback() {
        return containerManagementCallback;
    }

    public DockerState getDockerState() {
        return dockerState;
    }

    public TransformedApplicationDefinitionCallback getTransformedApplicationDefinitionCallback() {
        return transformedApplicationDefinitionCallback;
    }

    public ContainersManageContext setApplicationBuildDetails(List<ApplicationBuildDetails> applicationBuildDetails) {
        this.applicationBuildDetails = applicationBuildDetails;
        return this;
    }

    public ContainersManageContext setBaseOutputDirectory(String baseOutputDirectory) {
        this.baseOutputDirectory = baseOutputDirectory;
        return this;
    }

    public ContainersManageContext setContainerManagementCallback(DockerContainerManagementCallback containerManagementCallback) {
        this.containerManagementCallback = containerManagementCallback;
        return this;
    }

    public ContainersManageContext setDockerState(DockerState dockerState) {
        this.dockerState = dockerState;
        return this;
    }

    public ContainersManageContext setTransformedApplicationDefinitionCallback(TransformedApplicationDefinitionCallback transformedApplicationDefinitionCallback) {
        this.transformedApplicationDefinitionCallback = transformedApplicationDefinitionCallback;
        return this;
    }

}
