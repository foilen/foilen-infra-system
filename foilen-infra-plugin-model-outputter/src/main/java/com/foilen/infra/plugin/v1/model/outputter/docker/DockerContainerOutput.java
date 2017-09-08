/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.outputter.docker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionBuildStep;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.google.common.base.Strings;

/**
 * To get different outputs from a {@link IPApplicationDefinition}.
 */
public class DockerContainerOutput {

    protected static String sanitize(String text) {
        text = text.replaceAll("'", "\\\\'");
        text = text.replaceAll("\\\"", "\\\\\"");
        return text;
    }

    static public String toDockerfile(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx) {
        StringBuilder content = new StringBuilder();
        // From
        content.append("FROM ").append(applicationDefinition.getFrom()).append("\n\n");

        // Steps
        for (IPApplicationDefinitionBuildStep step : applicationDefinition.getBuildSteps()) {
            switch (step.getType()) {
            case COMMAND:
                content.append("RUN ");
                break;
            case COPY:
                content.append("COPY ");
                break;
            }

            content.append(step.getStep()).append("\n");
        }

        // Fix permissions
        content.append("\n");
        for (Tuple2<String, Integer> containerUserAndId : applicationDefinition.getContainerUsersToChangeId()) {
            content.append("RUN ");
            content.append("FIX_CONTAINER_USER_ID=$(id -u ").append(containerUserAndId.getA()).append(") ;");
            content.append("FIX_CONTAINER_GROUP_ID=$(id -g ").append(containerUserAndId.getA()).append(") ;");
            content.append("usermod -u ").append(containerUserAndId.getB()).append(" ").append(containerUserAndId.getA()).append(" -o ;");
            content.append("groupmod -g ").append(containerUserAndId.getB()).append(" ").append(containerUserAndId.getA()).append(" -o ;");
            content.append("find /etc /home /opt /root /run /srv /tmp /usr /var -uid $FIX_CONTAINER_USER_ID -exec chown ").append(containerUserAndId.getA()).append(" {} \\; ;");
            content.append("find /etc /home /opt /root /run /srv /tmp /usr /var -gid $FIX_CONTAINER_GROUP_ID -exec chgrp ").append(containerUserAndId.getA()).append(" {} \\; ;");
            content.append("\n");
        }
        content.append("\n");

        // Exposed ports
        if (!applicationDefinition.getPortsExposed().isEmpty()) {
            content.append("EXPOSE");
            for (Integer next : applicationDefinition.getPortsExposed().values()) {
                content.append(" ").append(next);
            }
            content.append("\n");
        }
        if (!applicationDefinition.getUdpPortsExposed().isEmpty()) {
            content.append("EXPOSE");
            for (Integer next : applicationDefinition.getUdpPortsExposed().values()) {
                content.append(" ").append(next).append("/udp");
            }
            content.append("\n");
        }
        content.append("\n");

        // Volumes
        if (!applicationDefinition.getVolumes().isEmpty()) {
            content.append("VOLUME ").append(JsonTools.compactPrint(applicationDefinition.getVolumes().values())).append("\n");
        }
        content.append("\n");

        // User
        content.append("USER ").append(applicationDefinition.getRunAs()).append("\n\n");

        // Working directory
        String workingDirectory = applicationDefinition.getWorkingDirectory();
        if (!Strings.isNullOrEmpty(workingDirectory)) {
            content.append("WORKDIR ").append(workingDirectory).append("\n\n");
        }

        // Command
        content.append("CMD ");
        content.append(applicationDefinition.getCommand()).append("\n");

        return content.toString();
    }

    public static String[] toRunCommandArgumentsSinglePassAttached(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx) {
        List<String> arguments = new ArrayList<>();

        arguments.add("run");

        arguments.add("-i");

        arguments.add("--rm");

        // Volumes
        for (Entry<String, String> entry : applicationDefinition.getVolumes().entrySet()) {
            arguments.add("--volume");
            arguments.add(sanitize(entry.getKey() + ":" + sanitize(entry.getValue())));
        }

        // Exposed ports
        if (!applicationDefinition.getPortsExposed().isEmpty()) {
            for (Entry<Integer, Integer> entry : applicationDefinition.getPortsExposed().entrySet()) {
                arguments.add("--publish");
                arguments.add(entry.getKey() + ":" + entry.getValue());
            }
        }
        if (!applicationDefinition.getUdpPortsExposed().isEmpty()) {
            for (Entry<Integer, Integer> entry : applicationDefinition.getUdpPortsExposed().entrySet()) {
                arguments.add("--publish");
                arguments.add(entry.getKey() + ":" + entry.getValue() + "/udp");
            }
        }

        // IP
        String ip = applicationDefinition.getIp();
        if (!Strings.isNullOrEmpty(ip)) {
            arguments.add("--ip");
            arguments.add(ip);
            arguments.add("--net");
            arguments.add("infra");
        }

        // Host to IP mapping
        for (Tuple2<String, String> hostToIp : applicationDefinition.getHostToIpMapping()) {
            arguments.add("--add-host");
            arguments.add(hostToIp.getA() + ":" + hostToIp.getB());
        }

        // Add user
        if (applicationDefinition.getRunAs() != null) {
            arguments.add("-u");
            arguments.add(applicationDefinition.getRunAs().toString());
        }

        // Instance name and hostname
        if (ctx.getInstanceName() != null) {
            arguments.add("--name");
            arguments.add(ctx.getInstanceName());
        }

        if (ctx.getHostName() != null) {
            arguments.add("--hostname");
            arguments.add(ctx.getHostName());
        }

        arguments.add(ctx.getImageName());

        return arguments.toArray(new String[arguments.size()]);
    }

    public static String[] toRunCommandArgumentsWithRestart(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx) {
        List<String> arguments = new ArrayList<>();

        arguments.add("run");

        arguments.add("--detach");

        arguments.add("--restart");
        arguments.add("always");

        // Volumes
        for (Entry<String, String> entry : applicationDefinition.getVolumes().entrySet()) {
            arguments.add("--volume");
            arguments.add(sanitize(entry.getKey() + ":" + sanitize(entry.getValue())));
        }

        // Exposed ports
        if (!applicationDefinition.getPortsExposed().isEmpty()) {
            for (Entry<Integer, Integer> entry : applicationDefinition.getPortsExposed().entrySet()) {
                arguments.add("--publish");
                arguments.add(entry.getKey() + ":" + entry.getValue());
            }
        }
        if (!applicationDefinition.getUdpPortsExposed().isEmpty()) {
            for (Entry<Integer, Integer> entry : applicationDefinition.getUdpPortsExposed().entrySet()) {
                arguments.add("--publish");
                arguments.add(entry.getKey() + ":" + entry.getValue() + "/udp");
            }
        }

        // IP
        String ip = applicationDefinition.getIp();
        if (!Strings.isNullOrEmpty(ip)) {
            arguments.add("--ip");
            arguments.add(ip);
            arguments.add("--net");
            arguments.add("infra");
        }

        // Host to IP mapping
        for (Tuple2<String, String> hostToIp : applicationDefinition.getHostToIpMapping()) {
            arguments.add("--add-host");
            arguments.add(hostToIp.getA() + ":" + hostToIp.getB());
        }

        // Add user
        if (applicationDefinition.getRunAs() != null) {
            arguments.add("-u");
            arguments.add(applicationDefinition.getRunAs().toString());
        }

        // Instance name and hostname
        if (ctx.getInstanceName() != null) {
            arguments.add("--name");
            arguments.add(ctx.getInstanceName());
        }

        if (ctx.getHostName() != null) {
            arguments.add("--hostname");
            arguments.add(ctx.getHostName());
        }

        arguments.add(ctx.getImageName());

        return arguments.toArray(new String[arguments.size()]);
    }

}
