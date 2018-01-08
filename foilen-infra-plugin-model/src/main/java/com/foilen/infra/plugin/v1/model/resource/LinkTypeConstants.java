/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.resource;

public abstract class LinkTypeConstants {

    /**
     * To tell that this resource is installed on the Machine.
     */
    public static final String INSTALLED_ON = "INSTALLED_ON";

    /**
     * To tell that this resource is pointing to another resource.
     */
    public static final String POINTS_TO = "POINTS_TO";

    /**
     * To tell a resource is managing created/modified/deleted the linked resource.
     */
    public static final String MANAGES = "MANAGES";

    /**
     * To tell that this resource needs a UnixUser.
     */
    public static final String RUN_AS = "RUN_AS";

    /**
     * To tell that this resource is using another resource.
     */
    public static final String USES = "USES";

}
