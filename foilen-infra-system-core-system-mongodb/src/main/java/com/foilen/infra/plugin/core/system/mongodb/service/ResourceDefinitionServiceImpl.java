/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.reflection.ReflectionTools;
import com.foilen.smalltools.tools.AbstractBasics;

@Service
public class ResourceDefinitionServiceImpl extends AbstractBasics implements ResourceDefinitionService {

    private Map<Class<? extends IPResource>, List<Class<?>>> allClassesByResourceClass = new HashMap<>();
    private Map<Class<? extends IPResource>, IPResourceDefinition> resourceDefinitionByResourceClass = new HashMap<>();
    private Map<String, IPResourceDefinition> resourceDefinitionByResourceType = new HashMap<>();

    @Override
    public IPResourceDefinition getResourceDefinition(Class<? extends IPResource> resourceClass) {
        return resourceDefinitionByResourceClass.get(resourceClass);
    }

    @Override
    public IPResourceDefinition getResourceDefinition(IPResource resource) {
        return resourceDefinitionByResourceClass.get(resource.getClass());
    }

    @Override
    public IPResourceDefinition getResourceDefinition(String resourceType) {
        return resourceDefinitionByResourceType.get(resourceType);
    }

    @Override
    public List<IPResourceDefinition> getResourceDefinitions() {
        return Collections.unmodifiableList(resourceDefinitionByResourceClass.values().stream().collect(Collectors.toList()));
    }

    @Override
    public List<IPResourceDefinition> getResourceDefinitions(Class<? extends IPResource> resourceClass) {
        return allClassesByResourceClass.entrySet().stream() //
                .filter(it -> it.getValue().contains(resourceClass)) //
                .map(it -> resourceDefinitionByResourceClass.get(it.getKey())) //
                .filter(it -> it != null) //
                .collect(Collectors.toList());

    }

    @Override
    public void resourceAdd(IPResourceDefinition resourceDefinition) {
        resourceDefinitionByResourceClass.put(resourceDefinition.getResourceClass(), resourceDefinition);
        resourceDefinitionByResourceType.put(resourceDefinition.getResourceType(), resourceDefinition);

        allClassesByResourceClass.put(resourceDefinition.getResourceClass(), ReflectionTools.allTypes(resourceDefinition.getResourceClass()));
    }

}
