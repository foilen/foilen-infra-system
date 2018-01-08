/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.exception;

import com.foilen.infra.plugin.v1.model.resource.IPResource;

public class ResourceNotFromRepositoryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFromRepositoryException(IPResource resource) {
        super("The resource " + resource + " is not comming from the repository");
    }

}
