/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.base;

public final class IPApplicationDefinitionHelper {

    /**
     * When the container runs as root, you can have services running as another user.
     *
     * @param applicationDefinition
     *            the app definition
     * @param serviceName
     *            the service name
     * @param command
     *            the command to run
     * @param runAsContainerUser
     *            the container's user name to run the command as
     */
    public static void addServiceForUser(IPApplicationDefinition applicationDefinition, String serviceName, String command, String runAsContainerUser) {
        applicationDefinition.addService(serviceName, "su -c '" + command + "' -s /bin/sh " + runAsContainerUser);
    }

    /**
     * Create the user in the container and set the running container as it.
     *
     * @param applicationDefinition
     *            the app definition
     * @param hostUserId
     *            the host user id
     * @param containerUserName
     *            the container user name that will have the host's user id
     */
    public static void createAndRunAsUser(IPApplicationDefinition applicationDefinition, int hostUserId, String containerUserName) {
        applicationDefinition.addBuildStepCommand("useradd -u " + hostUserId + " " + containerUserName);
        applicationDefinition.setRunAs(hostUserId);
    }

    private IPApplicationDefinitionHelper() {
    }

}
