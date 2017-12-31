/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils;

import java.util.List;
import java.util.Optional;

import com.foilen.infra.plugin.system.utils.callback.DockerContainerManagementCallback;
import com.foilen.infra.plugin.system.utils.model.DockerPs;
import com.foilen.infra.plugin.system.utils.model.DockerState;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.outputter.docker.DockerContainerOutputContext;
import com.foilen.smalltools.tuple.Tuple2;

public interface DockerUtils {

    boolean containerCopyFileContent(String containerName, String path, String content);

    boolean containerCopyFiles(String containerName, List<Tuple2<String, String>> pathAndContentFiles);

    boolean containerExecCommand(String containerName, String command);

    boolean containerExecCommands(String containerName, List<String> commands);

    boolean containerIsRunningByContainerNameOrId(String applicationNameToStart);

    List<DockerPs> containerPsFindAll();

    Optional<DockerPs> containerPsFindByContainerNameOrId(String containerNameOrId);

    /**
     * Actions:
     * <ul>
     * <li>Check if needs the ports redirector applications (in and out) and add them if needed</li>
     * <li>Stop any non-needed applications</li>
     * <li>Check starting dependencies</li>
     * <li>Build any images that are different than the running ones</li>
     * <li>Start/Restart any rebuilded or that needs a restart</li>
     * <li>Execute any steps that needs to be ran when the container is running</li>
     * </ul>
     *
     * It will use the dockerState and update it.
     *
     * @param dockerState
     *            the current state
     * @param outputContextAndApplicationDefinitions
     *            the list of applications to run in containers
     */
    void containersManage(DockerState dockerState, List<Tuple2<DockerContainerOutputContext, IPApplicationDefinition>> outputContextAndApplicationDefinitions);

    /**
     * Actions:
     * <ul>
     * <li>Check if needs the ports redirector applications (in and out) and add them if needed</li>
     * <li>Stop any non-needed applications</li>
     * <li>Check starting dependencies</li>
     * <li>Build any images that are different than the running ones</li>
     * <li>Start/Restart any rebuilded or that needs a restart</li>
     * <li>Execute any steps that needs to be ran when the container is running</li>
     * </ul>
     *
     * It will use the dockerState and update it.
     *
     * @param dockerState
     *            the current state
     * @param outputContextAndApplicationDefinitions
     *            the list of applications to run in containers
     * @param containerManagementCallback
     *            a callback method at some points
     */
    void containersManage(DockerState dockerState, List<Tuple2<DockerContainerOutputContext, IPApplicationDefinition>> outputContextAndApplicationDefinitions,
            DockerContainerManagementCallback containerManagementCallback);

    boolean containerStartOnce(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx);

    boolean containerStartWithRestart(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx);

    boolean containerStopAndRemove(DockerContainerOutputContext ctx);

    boolean containerStopAndRemove(String containerNameOrId);

    boolean imageBuild(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx);

    void volumeHostCreate(IPApplicationDefinition applicationDefinition);

}
