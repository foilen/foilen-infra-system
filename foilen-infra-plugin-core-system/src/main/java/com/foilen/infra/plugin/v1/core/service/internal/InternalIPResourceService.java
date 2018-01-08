/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.service.internal;

import java.util.List;

import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.model.resource.IPResource;

/**
 * To manage the resources.
 */
public interface InternalIPResourceService {

    void resourceAdd(IPResourceDefinition resourceDefinition);

    /**
     * Get all the resources in the database.
     *
     * WARNING: Use mostly for testing when you know there is not much resources.
     *
     * @return the resources
     */
    List<? extends IPResource> resourceFindAll();

}
