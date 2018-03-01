/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.exception;

import java.util.Map;

import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.core.resource.IPResourceQuery;
import com.foilen.infra.plugin.v1.model.resource.IPResource;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(Class<? extends IPResource> resourceClass, Map<?, ?> propertyEquals, Long internalId) {
        super("Tried to update an unexisting resource of class [" + resourceClass.getName() + "] ; search term [" + propertyEquals + "] ; internal id [" + internalId + "]");
    }

    public ResourceNotFoundException(IPResource resource) {
        super("Tried to update an unexisting resource " + resource);
    }

    public ResourceNotFoundException(IPResourceDefinition resourceDefinition, IPResourceQuery<?> resourceQuery, Long internalId) {
        super("Tried to update an unexisting resource of type [" + resourceDefinition.getResourceType() + "] ; search term [" + resourceQuery.getPropertyEquals() + "] ; internal id [" + internalId
                + "]");
    }

    public ResourceNotFoundException(Long internalId) {
        super("Tried to update an unexisting resource with id " + internalId);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }

}
