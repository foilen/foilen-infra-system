/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.service;

import java.util.List;

import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.model.resource.IPResource;

public interface ResourceDefinitionService {

    IPResourceDefinition getResourceDefinition(Class<? extends IPResource> resourceClass);

    IPResourceDefinition getResourceDefinition(IPResource resource);

    IPResourceDefinition getResourceDefinition(String resourceType);

    List<IPResourceDefinition> getResourceDefinitions();

    List<IPResourceDefinition> getResourceDefinitions(Class<? extends IPResource> resourceClass);

    void resourceAdd(IPResourceDefinition resourceDefinition);

}
