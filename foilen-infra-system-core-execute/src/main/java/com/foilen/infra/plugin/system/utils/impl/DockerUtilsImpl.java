/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.impl;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.event.Level;

import com.foilen.infra.plugin.system.utils.DockerUtils;
import com.foilen.infra.plugin.system.utils.UnixShellAndFsUtils;
import com.foilen.infra.plugin.system.utils.UtilsException;
import com.foilen.infra.plugin.system.utils.model.ApplicationBuildDetails;
import com.foilen.infra.plugin.system.utils.model.ContainersManageContext;
import com.foilen.infra.plugin.system.utils.model.CronApplicationBuildDetails;
import com.foilen.infra.plugin.system.utils.model.DockerPs;
import com.foilen.infra.plugin.system.utils.model.DockerPsStatus;
import com.foilen.infra.plugin.system.utils.model.DockerState;
import com.foilen.infra.plugin.system.utils.model.DockerStateIds;
import com.foilen.infra.plugin.system.utils.model.DockerStep;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionAssetsBundle;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionPortRedirect;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionVolume;
import com.foilen.infra.plugin.v1.model.outputter.docker.DockerContainerOutput;
import com.foilen.infra.plugin.v1.model.outputter.docker.DockerContainerOutputContext;
import com.foilen.infra.plugin.v1.model.redirectportregistry.RedirectPortRegistryEntries;
import com.foilen.infra.plugin.v1.model.redirectportregistry.RedirectPortRegistryEntry;
import com.foilen.infra.plugin.v1.model.redirectportregistry.RedirectPortRegistryExit;
import com.foilen.infra.plugin.v1.model.redirectportregistry.RedirectPortRegistryExits;
import com.foilen.smalltools.JavaEnvironmentValues;
import com.foilen.smalltools.TimeoutRunnableHandler;
import com.foilen.smalltools.iterable.FileLinesIterable;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.AssertTools;
import com.foilen.smalltools.tools.CollectionsTools;
import com.foilen.smalltools.tools.DirectoryTools;
import com.foilen.smalltools.tools.ExecutorsTools;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.MultiDependenciesResolverTools;
import com.foilen.smalltools.tools.SearchingAvailabilityIntTools;
import com.foilen.smalltools.tools.SpaceConverterTool;
import com.foilen.smalltools.tools.StringTools;
import com.foilen.smalltools.tools.SystemTools;
import com.foilen.smalltools.tools.ThreadTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.google.common.base.Strings;
import com.google.common.io.Files;

public class DockerUtilsImpl extends AbstractBasics implements DockerUtils {

    private static final ThreadLocal<SimpleDateFormat> createdAtSdf = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        }
    };

    public static final List<DockerPsStatus> stoppedStatuses = Collections.unmodifiableList(Arrays.asList( //
            DockerPsStatus.Created, //
            DockerPsStatus.Dead, //
            DockerPsStatus.Exited, //
            DockerPsStatus.Removal //
    ));

    private static final String hostFs = SystemTools.getPropertyOrEnvironment("HOSTFS", "/");
    private static final String machineName = JavaEnvironmentValues.getHostName();

    private Scheduler cronScheduler;
    private UnixShellAndFsUtils unixShellAndFsUtils;

    public DockerUtilsImpl() {
        unixShellAndFsUtils = new UnixShellAndFsUtilsImpl();

        initQuartz();
    }

    public DockerUtilsImpl(UnixShellAndFsUtils unixShellAndFsUtils) {
        this.unixShellAndFsUtils = unixShellAndFsUtils;

        initQuartz();
    }

    @Override
    public boolean containerCopyFileContent(String containerName, String path, String content) {
        logger.info("[CONTAINER] [{}] COPY file in [{}]", containerName, path);

        File tmpDir = Files.createTempDir();
        File containerPath = new File(path);
        String containerFolder = containerPath.getParent();
        if (containerFolder == null) {
            containerFolder = "/";
        }
        containerFolder = DirectoryTools.pathTrailingSlash(containerFolder);
        String fileName = containerPath.getName();

        FileTools.writeFile(content, tmpDir.getAbsolutePath() + "/" + fileName);

        boolean success = false;
        try {
            unixShellAndFsUtils.executeCommandOrFail(Level.DEBUG, "CONTAINER/" + containerName, //
                    "/usr/bin/docker", //
                    "cp", tmpDir.getAbsolutePath() + "/.", containerName + ":" + containerFolder);
            success = true;
        } catch (Exception e) {
        }

        logger.debug("[CONTAINER] [{}] COPY file - success: {}", containerName, success);

        DirectoryTools.deleteFolder(tmpDir);

        return success;
    }

    @Override
    public boolean containerCopyFiles(String containerName, List<Tuple2<String, String>> pathAndContentFiles) {
        boolean success = true;
        for (Tuple2<String, String> pathAndContentFile : pathAndContentFiles) {
            success &= containerCopyFileContent(containerName, pathAndContentFile.getA(), pathAndContentFile.getB());
        }
        return success;
    }

    @Override
    public boolean containerExecCommand(String containerName, String command) {

        logger.info("[CONTAINER] [{}] EXEC command [{}]", containerName, command);

        boolean success = false;
        try {
            unixShellAndFsUtils.executeCommandOrFail(Level.DEBUG, "CONTAINER/" + containerName, //
                    "/usr/bin/docker", //
                    "exec", "-i", containerName, "/bin/bash", "-c", command);
            success = true;
        } catch (Exception e) {
        }

        logger.debug("[CONTAINER] [{}] EXEC command - success: {}", containerName, success);

        return success;
    }

    @Override
    public boolean containerExecCommands(String containerName, List<String> commands) {
        boolean success = true;
        for (String command : commands) {
            success &= containerExecCommand(containerName, command);
        }
        return success;
    }

    @Override
    public boolean containerIsRunningByContainerNameOrId(String containerNameOrId) {
        Optional<DockerPs> optional = containerPsFindByContainerNameOrId(containerNameOrId);
        if (!optional.isPresent()) {
            return false;
        }
        return optional.get().getStatus() == DockerPsStatus.Up;
    }

    @Override
    public List<DockerPs> containerPsFindAll() {

        // Get details
        String output = unixShellAndFsUtils.executeCommandQuietAndGetOutput("Docker", "ps", //
                "/usr/bin/docker", //
                "ps", "-a", "--no-trunc", "-s", "--format", "{{.ID}}\t{{.Names}}\t{{.CreatedAt}}\t{{.RunningFor}}\t{{.Status}}\t{{.Size}}");

        List<DockerPs> containers = convertToDockerPs(output);

        // Sort
        Collections.sort(containers, (a, b) -> a.getName().compareTo(b.getName()));
        return containers;
    }

    @Override
    public Optional<DockerPs> containerPsFindByContainerNameOrId(String containerNameOrId) {
        return containerPsFindAll().stream().filter(it -> containerNameOrId.equals(it.getName()) || containerNameOrId.equals(it.getId())).findAny();
    }

    @Override
    public List<String> containersManage(ContainersManageContext containersManageContext) {

        List<String> modifiedContainerNames = new ArrayList<>();
        DockerState dockerState = containersManageContext.getDockerState();

        // Check if needs the ports redirector applications (in and out) and add them if needed
        MultiDependenciesResolverTools dependenciesResolver = new MultiDependenciesResolverTools();
        List<ApplicationBuildDetails> allApplicationBuildDetails = new ArrayList<>();
        allApplicationBuildDetails.addAll(containersManageContext.getAlwaysRunningApplications());
        allApplicationBuildDetails.addAll(containersManageContext.getCronApplications());
        boolean needsRedirectorEntry = allApplicationBuildDetails.stream() //
                .filter(it -> it.getApplicationDefinition().getPortsRedirect().stream() //
                        .filter(pr -> !pr.isToLocalMachine())//
                        .findAny().isPresent()) //
                .findAny().isPresent();
        boolean needsRedirectorExit = false;
        Map<String, ApplicationBuildDetails> applicationBuildDetailsByName = new HashMap<>();
        List<String> remoteMachineContainerEndpointsToSet = new ArrayList<>();
        for (ApplicationBuildDetails applicationBuildDetails : containersManageContext.getAlwaysRunningApplications()) {

            // Add the app to the dependencies resolver
            String containerName = applicationBuildDetails.getOutputContext().getContainerName();
            dependenciesResolver.addItems(containerName);

            // Add to the map
            applicationBuildDetailsByName.put(containerName, applicationBuildDetails);

            // Exposing endpoints -> needs exit
            IPApplicationDefinition applicationDefinition = applicationBuildDetails.getApplicationDefinition();
            if (!applicationDefinition.getPortsEndpoint().isEmpty()) {
                logger.info("[MANAGER] [{}] is exposing endpoints and needs the redirector exit", containerName);
                needsRedirectorExit = true;
                dependenciesResolver.addDependency(DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME, containerName);
            }

            // Using remote endpoints -> needs entry
            List<IPApplicationDefinitionPortRedirect> portsRedirect = applicationDefinition.getPortsRedirect();
            for (IPApplicationDefinitionPortRedirect portRedirect : portsRedirect) {
                if (portRedirect.isToLocalMachine()) {
                    logger.debug("[MANAGER] [{}] is accessing a local endpoint [{}]. Depends on {}", containerName, portRedirect, portRedirect.getToContainerName());
                    dependenciesResolver.addDependency(containerName, portRedirect.getToContainerName());
                } else {
                    logger.debug("[MANAGER] [{}] is accessing a remote endpoint [{}]. Needs redirector entry", containerName, portRedirect);
                    needsRedirectorEntry = true;
                    dependenciesResolver.addDependency(containerName, DockerContainerOutput.REDIRECTOR_ENTRY_CONTAINER_NAME);
                }
            }
        }
        // Add the Redirectors applications if needed
        RedirectPortRegistryExits redirectPortRegistryExits = new RedirectPortRegistryExits();
        if (needsRedirectorEntry) {
            RedirectPortRegistryEntries redirectPortRegistryEntries = new RedirectPortRegistryEntries();
            DockerContainerOutputContext outputContext = new DockerContainerOutputContext(DockerContainerOutput.REDIRECTOR_ENTRY_CONTAINER_NAME, DockerContainerOutput.REDIRECTOR_ENTRY_CONTAINER_NAME);
            IPApplicationDefinition applicationDefinition = new IPApplicationDefinition();
            applicationDefinition.setFrom("foilen/redirectport-registry:1.1.0");
            applicationDefinition.setRunAs(65534L); // Nobody

            IPApplicationDefinitionAssetsBundle assetBundle = applicationDefinition.addAssetsBundle();
            boolean isRedirectorSsl = CollectionsTools.isAllItemNotNullOrEmpty(dockerState.getRedirectorNodeCert(), dockerState.getRedirectorNodeKey());
            if (dockerState.getRedirectorCaCerts().isEmpty()) {
                isRedirectorSsl = false;
            }
            List<String> arguments = new ArrayList<>();
            if (isRedirectorSsl) {
                assetBundle.addAssetContent("/data/ca-certs.json", JsonTools.prettyPrint(dockerState.getRedirectorCaCerts()));
                assetBundle.addAssetContent("/data/node-cert.pem", dockerState.getRedirectorNodeCert());
                assetBundle.addAssetContent("/data/node-key.pem", dockerState.getRedirectorNodeKey());

                arguments.add("--caCertsFile");
                arguments.add("/data/ca-certs.json");
                arguments.add("--bridgeCertFile");
                arguments.add("/data/node-cert.pem");
                arguments.add("--bridgePrivateKeyFile");
                arguments.add("/data/node-key.pem");
            }

            // For all the apps that needs to redirect to a remote machine
            Set<Integer> usedRedirectPort = new HashSet<>();
            allApplicationBuildDetails.stream() //
                    .filter(it -> it.getApplicationDefinition().getPortsRedirect().stream() //
                            .filter(pr -> !pr.isToLocalMachine())//
                            .findAny().isPresent()) //
                    .forEach(applicationBuildDetails -> {
                        applicationBuildDetails.getApplicationDefinition().getPortsRedirect().stream() //
                                .filter(pr -> !pr.isToLocalMachine())//
                                .forEach(pr -> {
                                    Integer next = dockerState.getRedirectPortByMachineContainerEndpoint().get(pr.getMachineContainerEndpoint());
                                    if (next != null) {
                                        usedRedirectPort.add(next);
                                    }
                                });
                    });

            SearchingAvailabilityIntTools searchingAvailabilityRedirectPort = new SearchingAvailabilityIntTools(2000, 65000, 1, (from, to) -> {
                for (int i = from; i <= to; ++i) {
                    if (!usedRedirectPort.contains(i)) {
                        return Optional.of(i);
                    }
                }
                return Optional.empty();
            });
            allApplicationBuildDetails.stream() //
                    .filter(it -> it.getApplicationDefinition().getPortsRedirect().stream() //
                            .filter(pr -> !pr.isToLocalMachine())//
                            .findAny().isPresent()) //
                    .forEach(applicationBuildDetails -> {
                        applicationBuildDetails.getApplicationDefinition().getPortsRedirect().stream() //
                                .filter(pr -> !pr.isToLocalMachine())//
                                .forEach(pr -> {
                                    // Save endpoints in state
                                    String machineContainerEndpoint = pr.getMachineContainerEndpoint();

                                    Integer port = dockerState.getRedirectPortByMachineContainerEndpoint().get(machineContainerEndpoint);
                                    if (port == null) {
                                        Optional<Integer> portOptional = searchingAvailabilityRedirectPort.getNext();
                                        AssertTools.assertTrue(portOptional.isPresent(), "There is no more available ports for the redirections");
                                        port = portOptional.get();
                                        usedRedirectPort.add(port);
                                    }

                                    remoteMachineContainerEndpointsToSet.add(machineContainerEndpoint);
                                    dockerState.getRedirectPortByMachineContainerEndpoint().put(machineContainerEndpoint, port);

                                    // Add to redirectPortRegistryEntries
                                    redirectPortRegistryEntries.getEntries().add(new RedirectPortRegistryEntry(port, pr.getToMachine(), pr.getToContainerName(), pr.getToEndpoint()));
                                });

                    });

            applicationDefinition.addCopyWhenStartedContent("/data/entry.json", JsonTools.prettyPrint(redirectPortRegistryEntries));// We want hot change

            arguments.add("--bridgePort");
            arguments.add(String.valueOf(dockerState.getRedirectorBridgePort()));
            arguments.add("--entryBridgeRegistryFile");
            arguments.add("/data/entry.json");
            applicationDefinition.setCommand(JsonTools.compactPrint(arguments));

            // Add to the list
            applicationBuildDetailsByName.put(DockerContainerOutput.REDIRECTOR_ENTRY_CONTAINER_NAME, new ApplicationBuildDetails() //
                    .setApplicationDefinition(applicationDefinition) //
                    .setOutputContext(outputContext));
        }
        if (needsRedirectorExit) {
            DockerContainerOutputContext outputContext = new DockerContainerOutputContext(DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME, DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME);
            IPApplicationDefinition applicationDefinition = new IPApplicationDefinition();
            applicationDefinition.setFrom("foilen/redirectport-registry:1.1.0");
            applicationDefinition.setRunAs(65534L); // Nobody
            applicationDefinition.addPortExposed(dockerState.getRedirectorBridgePort(), dockerState.getRedirectorBridgePort());

            IPApplicationDefinitionAssetsBundle assetBundle = applicationDefinition.addAssetsBundle();
            boolean isRedirectorSsl = CollectionsTools.isAllItemNotNullOrEmpty(dockerState.getRedirectorNodeCert(), dockerState.getRedirectorNodeKey());
            if (dockerState.getRedirectorCaCerts().isEmpty()) {
                isRedirectorSsl = false;
            }
            List<String> arguments = new ArrayList<>();
            if (isRedirectorSsl) {
                assetBundle.addAssetContent("/data/ca-certs.json", JsonTools.prettyPrint(dockerState.getRedirectorCaCerts()));
                assetBundle.addAssetContent("/data/node-cert.pem", dockerState.getRedirectorNodeCert());
                assetBundle.addAssetContent("/data/node-key.pem", dockerState.getRedirectorNodeKey());

                arguments.add("--caCertsFile");
                arguments.add("/data/ca-certs.json");
                arguments.add("--bridgeCertFile");
                arguments.add("/data/node-cert.pem");
                arguments.add("--bridgePrivateKeyFile");
                arguments.add("/data/node-key.pem");
            }

            arguments.add("--bridgePort");
            arguments.add(String.valueOf(dockerState.getRedirectorBridgePort()));
            arguments.add("--exitBridgeRegistryFile");
            arguments.add("/data/exit.json");
            applicationDefinition.setCommand(JsonTools.compactPrint(arguments));

            // Add to the list
            applicationBuildDetailsByName.put(DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME, new ApplicationBuildDetails() //
                    .setApplicationDefinition(applicationDefinition) //
                    .setOutputContext(outputContext));
        }

        // Stop any non-needed applications
        List<String> neededContainerNames = allApplicationBuildDetails.stream() //
                .map(it -> it.getOutputContext().getContainerName()) //
                .sorted() //
                .collect(Collectors.toList());
        List<String> runningContainerNames = dockerState.getRunningContainersByName().keySet().stream() //
                .sorted() //
                .collect(Collectors.toList());
        if (needsRedirectorEntry) {
            neededContainerNames.add(DockerContainerOutput.REDIRECTOR_ENTRY_CONTAINER_NAME);
        }
        if (needsRedirectorExit) {
            neededContainerNames.add(DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME);
        }
        runningContainerNames.removeAll(neededContainerNames);
        for (String containerToStop : runningContainerNames) {
            logger.info("[MANAGER] Stop extra applications [{}]", containerToStop);
            containerStopAndRemove(containerToStop);
        }

        // Check starting order by dependencies
        List<String> startOrder = dependenciesResolver.getExecution();
        logger.debug("[MANAGER] Starting order: {}", startOrder);

        // Update IPs
        logger.debug("[MANAGER] Retrieving current IPs");
        startOrder.forEach(appName -> {
            String ip = getIp(appName);
            logger.debug("[MANAGER] IP of {} is {}", appName, ip);

            if (Strings.isNullOrEmpty(ip)) {
                dockerState.getIpByName().remove(appName);
            } else {
                dockerState.getIpByName().put(appName, ip);
            }

            String appNameSlash = appName + "/";
            for (String containerEndpoint : dockerState.getRedirectIpByMachineContainerEndpoint().keySet().stream().sorted().collect(Collectors.toList())) {
                if (containerEndpoint.startsWith(appNameSlash)) {
                    if (Strings.isNullOrEmpty(ip)) {
                        logger.debug("[MANAGER] Removing IP of {}", containerEndpoint);
                        dockerState.getRedirectIpByMachineContainerEndpoint().remove(containerEndpoint);
                    } else {
                        logger.debug("[MANAGER] Updating IP of {}", containerEndpoint);
                        dockerState.getRedirectIpByMachineContainerEndpoint().put(containerEndpoint, ip);
                    }
                }
            }

        });

        dockerState.getFailedContainersByName().clear();
        boolean existingRedirectorEntryPortOrHostChanged = false;
        for (String applicationNameToStart : startOrder) {
            logger.info("[MANAGER] Processing application [{}]", applicationNameToStart);

            // Get all the needed state and status
            Optional<DockerPs> currentDockerPsOptional = containerPsFindByContainerNameOrId(applicationNameToStart);
            boolean needStart = !currentDockerPsOptional.isPresent();
            if (currentDockerPsOptional.isPresent()) {
                needStart |= currentDockerPsOptional.get().getStatus() != DockerPsStatus.Up;
            }
            DockerStateIds lastRunningContainerIds = dockerState.getRunningContainersByName().remove(applicationNameToStart);
            ApplicationBuildDetails applicationBuildDetails = applicationBuildDetailsByName.get(applicationNameToStart);
            DockerContainerOutputContext ctx = applicationBuildDetails.getOutputContext();
            IPApplicationDefinition applicationDefinition = applicationBuildDetails.getApplicationDefinition();
            boolean dependsOnRedirectorEntry = applicationDefinition.getPortsRedirect().stream() //
                    .filter(pr -> !pr.isToLocalMachine()) //
                    .findAny().isPresent();

            // Add redirection details on the context
            ctx.setRedirectIpByMachineContainerEndpoint(dockerState.getRedirectIpByMachineContainerEndpoint());
            ctx.setRedirectPortByMachineContainerEndpoint(dockerState.getRedirectPortByMachineContainerEndpoint());

            // Save redirectPortRegistryExits in the applicationDetails if is that container
            if (DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME.equals(applicationNameToStart)) {
                applicationDefinition.addCopyWhenStartedContent("/data/exit.json", JsonTools.prettyPrint(redirectPortRegistryExits));// We want hot change
            }

            IPApplicationDefinition transformedApplicationDefinition = DockerContainerOutput.addInfrastructure(applicationDefinition, ctx);
            DockerStateIds currentTransformedDockerStateIds = new DockerStateIds( //
                    transformedApplicationDefinition.toImageUniqueId(), //
                    transformedApplicationDefinition.toContainerRunUniqueId(), //
                    transformedApplicationDefinition.toContainerStartUniqueId());
            containersManageContext.getTransformedApplicationDefinitionCallback().handler(applicationNameToStart, transformedApplicationDefinition);

            logger.debug("[MANAGER] [{}] The transformed application definition has ids {}", //
                    applicationNameToStart, currentTransformedDockerStateIds);

            // Check the steps to execute
            DockerStep startStep = DockerStep.COMPLETED;
            if (lastRunningContainerIds == null) {
                logger.debug("[MANAGER] [{}] is not currently running. Will build and start", applicationNameToStart);
                startStep = DockerStep.BUILD_IMAGE;
            } else {
                if (dependsOnRedirectorEntry && existingRedirectorEntryPortOrHostChanged) {
                    logger.debug("[MANAGER] [{}] needs the redirector entry and its port or host changed. Will build and start", applicationNameToStart);
                    startStep = DockerStep.BUILD_IMAGE;
                } else if (!lastRunningContainerIds.getImageUniqueId().equals(transformedApplicationDefinition.toImageUniqueId())) {
                    logger.debug("[MANAGER] [{}] has a different image {} -> {}. Will build and start", applicationNameToStart, //
                            lastRunningContainerIds.getImageUniqueId(), transformedApplicationDefinition.toImageUniqueId());
                    startStep = DockerStep.BUILD_IMAGE;
                } else if (!lastRunningContainerIds.getContainerRunUniqueId().equals(transformedApplicationDefinition.toContainerRunUniqueId())) {
                    logger.debug("[MANAGER] [{}] has a different run command {} -> {}. Will restart", applicationNameToStart, //
                            lastRunningContainerIds.getContainerRunUniqueId(), transformedApplicationDefinition.toContainerRunUniqueId());
                    startStep = DockerStep.RESTART_CONTAINER;
                } else if (needStart) {
                    logger.debug("[MANAGER] [{}] is not running. Will restart", applicationNameToStart);
                    startStep = DockerStep.RESTART_CONTAINER;
                } else if (!lastRunningContainerIds.getContainerStartedUniqueId().equals(transformedApplicationDefinition.toContainerStartUniqueId())) {
                    logger.debug("[MANAGER] [{}] has a different execute when started commands. Will execute", applicationNameToStart);
                    startStep = DockerStep.COPY_AND_EXECUTE_IN_RUNNING_CONTAINER;
                }
            }
            if (startStep != DockerStep.COMPLETED) {
                modifiedContainerNames.add(applicationNameToStart);
            }

            // Clear the state
            String lastRunningIp = dockerState.getIpByName().remove(applicationNameToStart);

            // If needs any change, stop any running command
            if (startStep != DockerStep.COMPLETED) {
                Future<Boolean> executionFuture = dockerState.getExecutionsFutures().get(applicationNameToStart);
                if (executionFuture != null && !executionFuture.isDone()) {
                    logger.debug("[MANAGER] [{}] Has a running execution. Stopping it");
                    executionFuture.cancel(true);
                }
            }

            // Check if should proceed
            if (!containersManageContext.getContainerManagementCallback().proceedWithTransformedContainer(applicationNameToStart, currentTransformedDockerStateIds)) {
                logger.error("[MANAGER] [{}] The callback requested to not proceed with this container", applicationNameToStart);
                dockerState.getFailedContainersByName().put(applicationNameToStart, currentTransformedDockerStateIds);

                // Keep previous success in running if still running
                if (lastRunningContainerIds != null && containerIsRunningByContainerNameOrId(applicationNameToStart)) {
                    dockerState.getRunningContainersByName().put(applicationNameToStart, lastRunningContainerIds);
                    dockerState.getIpByName().put(applicationNameToStart, lastRunningIp);
                }
                continue;
            }

            switch (startStep) {
            case BUILD_IMAGE:
                logger.info("[MANAGER] [{}] Building image", applicationNameToStart);
                currentTransformedDockerStateIds.setLastState(DockerStep.BUILD_IMAGE);
                if (!imageBuild(transformedApplicationDefinition, ctx)) {
                    logger.error("[MANAGER] [{}] Could not build the image", applicationNameToStart);
                    dockerState.getFailedContainersByName().put(applicationNameToStart, currentTransformedDockerStateIds);

                    // Keep previous success in running if still running
                    if (lastRunningContainerIds != null && containerIsRunningByContainerNameOrId(applicationNameToStart)) {
                        dockerState.getRunningContainersByName().put(applicationNameToStart, lastRunningContainerIds);
                        dockerState.getIpByName().put(applicationNameToStart, lastRunningIp);
                    }
                    continue;
                }
                volumeHostCreate(transformedApplicationDefinition);
            case RESTART_CONTAINER:
                logger.info("[MANAGER] [{}] Starting/Restarting container", applicationNameToStart);
                currentTransformedDockerStateIds.setLastState(DockerStep.RESTART_CONTAINER);
                containerStopAndRemove(ctx);
                if (!containerStartWithRestart(transformedApplicationDefinition, ctx)) {
                    logger.error("[MANAGER] [{}] Could not start the container", applicationNameToStart);
                    dockerState.getFailedContainersByName().put(applicationNameToStart, currentTransformedDockerStateIds);
                    continue;
                }
            case COPY_AND_EXECUTE_IN_RUNNING_CONTAINER:
                logger.info("[MANAGER] [{}] Copying files in the running container", applicationNameToStart);
                currentTransformedDockerStateIds.setLastState(DockerStep.COPY_AND_EXECUTE_IN_RUNNING_CONTAINER);
                containerCopyFiles(applicationNameToStart, transformedApplicationDefinition.getCopyWhenStartedPathAndContentFiles());
                logger.info("[MANAGER] [{}] Executing commands in the running container", applicationNameToStart);
                if (!transformedApplicationDefinition.getExecuteWhenStartedCommands().isEmpty()) {
                    Future<Boolean> executionFuture = ExecutorsTools.getCachedThreadPool().submit(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return containerExecCommands(applicationNameToStart, transformedApplicationDefinition.getExecuteWhenStartedCommands());
                        }
                    });
                    dockerState.getExecutionsFutures().put(applicationNameToStart, executionFuture);
                }
            case COMPLETED:
                logger.info("[MANAGER] [{}] Ready", applicationNameToStart);
                currentTransformedDockerStateIds.setLastState(DockerStep.COMPLETED);
                break;
            }

            // Note IP and new state
            String ip = null;
            for (int i = 0; i < 5 && (!dockerState.getIpByName().containsKey(applicationNameToStart)); ++i) {
                if (i != 0) {
                    ThreadTools.sleep(1000);
                }
                Optional<DockerPs> containerOptional = containerPsFindByContainerNameOrId(applicationNameToStart);
                if (containerOptional.isPresent()) {
                    DockerPs container = containerOptional.get();
                    ip = getIp(container.getName());
                    dockerState.getIpByName().put(applicationNameToStart, ip);

                    // Special case for Redirection Entry
                    if (DockerContainerOutput.REDIRECTOR_ENTRY_CONTAINER_NAME.equals(applicationNameToStart)) {

                        // Check IP changed
                        if (!StringTools.safeEquals(lastRunningIp, ip)) {
                            existingRedirectorEntryPortOrHostChanged = true;
                        }

                        // Set redirectIp/PortByMachineContainerEndpoint for all the endpoints
                        for (String mce : remoteMachineContainerEndpointsToSet) {
                            dockerState.getRedirectIpByMachineContainerEndpoint().put(mce, ip);
                        }
                    }
                    dockerState.getRunningContainersByName().put(applicationNameToStart, currentTransformedDockerStateIds);
                } else {
                    logger.error("[MANAGER] [{}] Could not find it running", applicationNameToStart);
                }
            }

            // Exposing endpoints
            logger.info("[MANAGER] [{}] Has {} enpoints to expose to the exit container ; exposing with ip {}", applicationNameToStart, transformedApplicationDefinition.getPortsEndpoint().size(), ip);
            if (!transformedApplicationDefinition.getPortsEndpoint().isEmpty() && ip != null) {
                for (Entry<Integer, String> portEndpoint : transformedApplicationDefinition.getPortsEndpoint().entrySet()) {
                    // Fill redirectPortRegistryExits
                    String endpoint = portEndpoint.getValue();
                    Integer port = portEndpoint.getKey();
                    redirectPortRegistryExits.getExits().add(new RedirectPortRegistryExit(applicationNameToStart, endpoint, ip, port));

                    // Save endpoints in state ("localhost" and current machine name)
                    String machineContainerEndpoint = IPApplicationDefinitionPortRedirect.LOCAL_MACHINE + "/" + applicationNameToStart + "/" + endpoint;
                    dockerState.getRedirectIpByMachineContainerEndpoint().put(machineContainerEndpoint, ip);
                    dockerState.getRedirectPortByMachineContainerEndpoint().put(machineContainerEndpoint, port);
                    machineContainerEndpoint = machineName + "/" + applicationNameToStart + "/" + endpoint;
                    dockerState.getRedirectIpByMachineContainerEndpoint().put(machineContainerEndpoint, ip);
                    dockerState.getRedirectPortByMachineContainerEndpoint().put(machineContainerEndpoint, port);

                }
                Collections.sort(redirectPortRegistryExits.getExits());
            }

        }

        CronJob.dockerUtils = this;
        for (CronApplicationBuildDetails applicationBuildDetails : containersManageContext.getCronApplications()) {
            String containerName = applicationBuildDetails.getOutputContext().getContainerName();
            logger.info("[MANAGER] Processing cron [{}]", containerName);

            // Get all the needed state and status
            DockerStateIds cronContainerStateId = dockerState.getCronContainersByName().get(containerName);
            DockerContainerOutputContext ctx = applicationBuildDetails.getOutputContext();
            IPApplicationDefinition applicationDefinition = applicationBuildDetails.getApplicationDefinition();
            boolean dependsOnRedirectorEntry = applicationDefinition.getPortsRedirect().stream() //
                    .filter(pr -> !pr.isToLocalMachine()) //
                    .findAny().isPresent();

            // Add redirection details on the context
            ctx.setRedirectIpByMachineContainerEndpoint(dockerState.getRedirectIpByMachineContainerEndpoint());
            ctx.setRedirectPortByMachineContainerEndpoint(dockerState.getRedirectPortByMachineContainerEndpoint());

            // Check the steps to execute
            boolean needToBuild = false;
            boolean needToStop = false;
            if (cronContainerStateId == null) {
                logger.debug("[MANAGER] [{}] was never built. Will build", containerName);
                needToBuild = true;
            } else {
                if ((dependsOnRedirectorEntry && existingRedirectorEntryPortOrHostChanged) || !cronContainerStateId.getImageUniqueId().equals(applicationDefinition.toImageUniqueId())) {
                    logger.debug("[MANAGER] [{}] has a different image. Will build and stop", containerName);
                    needToBuild = true;
                    needToStop = true;
                } else if (!cronContainerStateId.getContainerRunUniqueId().equals(applicationDefinition.toContainerRunUniqueId())) {
                    logger.debug("[MANAGER] [{}] has a different run command. Will stop", containerName);
                    needToStop = true;
                } else if (!cronContainerStateId.getContainerStartedUniqueId().equals(applicationDefinition.toContainerStartUniqueId())) {
                    logger.debug("[MANAGER] [{}] has a different execute when started commands. Will stop", containerName);
                    needToStop = true;
                }
            }

            // Clear the state
            dockerState.getCronContainersByName().remove(containerName);

            // If needs any change, stop any running command
            if (needToStop) {
                Future<Boolean> executionFuture = dockerState.getExecutionsFutures().get(containerName);
                if (executionFuture != null && !executionFuture.isDone()) {
                    logger.debug("[MANAGER] [{}] Has a running execution. Stopping it");
                    executionFuture.cancel(true);
                }
            }

            // Execute the steps
            IPApplicationDefinition transformedApplicationDefinition = DockerContainerOutput.addInfrastructure(applicationDefinition, ctx);
            DockerStateIds dockerStateIds = new DockerStateIds( //
                    transformedApplicationDefinition.toImageUniqueId(), //
                    transformedApplicationDefinition.toContainerRunUniqueId(), //
                    transformedApplicationDefinition.toContainerStartUniqueId());

            // Check if should proceed
            if (!containersManageContext.getContainerManagementCallback().proceedWithTransformedContainer(containerName, dockerStateIds)) {
                logger.error("[MANAGER] [{}] The callback requested to not proceed with this container", containerName);
                dockerState.getFailedContainersByName().put(containerName, dockerStateIds);
                try {
                    cronScheduler.deleteJob(new JobKey(containerName));
                } catch (SchedulerException e) {
                    logger.error("[MANAGER] [{}] Cannot delete the job", containerName);
                }
                continue;
            }

            // Build
            if (needToBuild) {
                logger.info("[MANAGER] [{}] Building image", containerName);
                dockerStateIds.setLastState(DockerStep.BUILD_IMAGE);
                if (!imageBuild(transformedApplicationDefinition, ctx)) {
                    logger.error("[MANAGER] [{}] Could not build the image", containerName);
                    dockerState.getFailedContainersByName().put(containerName, dockerStateIds);
                    try {
                        cronScheduler.deleteJob(new JobKey(containerName));
                    } catch (SchedulerException e) {
                        logger.error("[MANAGER] [{}] Cannot delete the job", containerName, e);
                    }
                    continue;
                }
                volumeHostCreate(transformedApplicationDefinition);
            }

            if (needToStop) {
                logger.info("[MANAGER] [{}] Stopping container", containerName);
                dockerStateIds.setLastState(DockerStep.RESTART_CONTAINER);
                containerStopAndRemove(ctx);
            }

            logger.info("[MANAGER] [{}] Ready", containerName);
            dockerStateIds.setLastState(DockerStep.COMPLETED);
            dockerState.getCronContainersByName().put(containerName, dockerStateIds);

            // If schedule changed, update it
            try {
                String cronTime = applicationBuildDetails.getCronTime();
                if (!Objects.equals(cronTime, dockerState.getCronTimeByName().get(containerName))) {
                    // Delete
                    cronScheduler.deleteJob(new JobKey(containerName));

                    // Create
                    CronTrigger trigger = TriggerBuilder.newTrigger() //
                            .withIdentity(containerName) //
                            .withSchedule(CronScheduleBuilder.cronSchedule(cronTime)) //
                            .build();
                    cronScheduler.scheduleJob(JobBuilder.newJob(CronJob.class) //
                            .withIdentity(containerName) //
                            .usingJobData("applicationDefinition", JsonTools.compactPrintWithoutNulls(transformedApplicationDefinition))//
                            .usingJobData("dockerContainerOutputContext", JsonTools.compactPrintWithoutNulls(ctx))//
                            .build(), trigger);

                    dockerState.getCronTimeByName().put(containerName, cronTime);
                }
            } catch (SchedulerException e) {
                logger.error("[MANAGER] [{}] Cannot update the job", containerName, e);
            }

        }

        return modifiedContainerNames;

    }

    @Override
    public boolean containerStartOnce(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx) {
        String containerName = ctx.getContainerName();
        logger.info("[CONTAINER] [{}] START", containerName);

        String[] runArguments = DockerContainerOutput.toRunArgumentsSinglePassDetached(applicationDefinition, ctx, false);
        logger.debug("[CONTAINER] [{}] START - Arguments: {}", containerName, runArguments);

        boolean success = false;
        try {
            unixShellAndFsUtils.executeCommandOrFail(Level.DEBUG, "CONTAINER/" + containerName, //
                    "/usr/bin/docker", //
                    runArguments);
            success = true;
        } catch (Exception e) {
        }

        logger.debug("[CONTAINER] [{}] START - success: {}", containerName, success);
        return success;
    }

    @Override
    public boolean containerStartWithRestart(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx) {
        String containerName = ctx.getContainerName();
        logger.info("[CONTAINER] [{}] START", containerName);

        String[] runArguments = DockerContainerOutput.toRunArgumentsWithRestart(applicationDefinition, ctx);
        logger.debug("[CONTAINER] [{}] START - Arguments: {}", containerName, runArguments);

        boolean success = false;
        try {
            unixShellAndFsUtils.executeCommandOrFail(Level.DEBUG, "CONTAINER/" + containerName, //
                    "/usr/bin/docker", //
                    runArguments);
            success = true;
        } catch (Exception e) {
        }

        logger.debug("[CONTAINER] [{}] START - success: {}", containerName, success);
        return success;
    }

    @Override
    public boolean containerStopAndRemove(DockerContainerOutputContext ctx) {
        return containerStopAndRemove(ctx.getContainerName());
    }

    @Override
    public boolean containerStopAndRemove(String containerNameOrId) {

        Optional<DockerPs> container = containerPsFindByContainerNameOrId(containerNameOrId);
        boolean exists = container.isPresent();
        boolean success = false;

        if (exists) {

            DockerPs dockerPs = container.get();
            if (!stoppedStatuses.contains(dockerPs.getStatus())) {

                try {
                    logger.info("[CONTAINER] [{}] STOP", containerNameOrId);
                    unixShellAndFsUtils.executeCommandOrFail(Level.DEBUG, "CONTAINER/" + containerNameOrId, //
                            "/usr/bin/docker", //
                            "stop", containerNameOrId);
                    success = true;
                } catch (Exception e) {
                }

                logger.debug("[CONTAINER] [{}] STOP - success: {}", containerNameOrId, success);
            }

            if (success) {
                try {
                    logger.info("[CONTAINER] [{}] REMOVE", containerNameOrId);
                    unixShellAndFsUtils.executeCommandOrFail(Level.DEBUG, "CONTAINER/" + containerNameOrId, //
                            "/usr/bin/docker", //
                            "rm", containerNameOrId);
                    success = true;
                } catch (Exception e) {
                }

                logger.debug("[CONTAINER] [{}] REMOVE - success: {}", containerNameOrId, success);
            }
        } else {
            logger.info("[CONTAINER] [{}] STOP and REMOVE. Already not present", containerNameOrId);
        }

        return success;

    }

    protected List<DockerPs> convertToDockerPs(String output) {
        List<DockerPs> results = new ArrayList<>();
        FileLinesIterable fileLinesIterable = new FileLinesIterable();
        fileLinesIterable.openString(output);
        for (String line : fileLinesIterable) {
            String[] parts = line.split("\t");
            DockerPs dockerPs = new DockerPs();
            int i = 0;
            dockerPs.setId(parts[i++]);
            dockerPs.setName(parts[i++].split(",", 2)[0]);
            try {
                dockerPs.setCreatedAt(createdAtSdf.get().parse(parts[i++]));
            } catch (ParseException e) {
            }
            dockerPs.setRunningFor(parts[i++]);
            String fullStatus = parts[i++];
            int spacePos = fullStatus.indexOf(' ');
            if (spacePos == -1) {
                spacePos = fullStatus.length();
            }
            dockerPs.setStatus(DockerPsStatus.valueOf(fullStatus.substring(0, spacePos)));
            String sizePart = parts[i++];
            String[] sizeParts = sizePart.split(" \\(virtual ");
            long instanceSize = SpaceConverterTool.convertToBytes(sizeParts[0]);
            dockerPs.setSize(instanceSize);
            long totalSize = SpaceConverterTool.convertToBytes(sizeParts[1].split("\\)")[0]);
            dockerPs.setTotalSize(totalSize);
            results.add(dockerPs);
        }

        return results;
    }

    @Override
    public String getIp(String containerNameOrId) {
        try {
            String output = unixShellAndFsUtils.executeCommandQuietAndGetOutput("Docker", "get ip", //
                    "/usr/bin/docker", //
                    "inspect", "--format", "{{ .NetworkSettings.IPAddress }}", containerNameOrId);
            output = output.replaceAll("\r", "");
            output = output.replaceAll("\n", "");
            return output;
        } catch (UtilsException e) {
            logger.warn("Could not get the IP of {}", containerNameOrId);
            return null;
        }
    }

    @Override
    public boolean imageBuild(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx) {
        String imageName = ctx.getImageName();
        logger.info("[IMAGE] [{}] BUILD", imageName);

        // Prepare the build directory
        DockerContainerOutput.toDockerBuildDirectory(applicationDefinition, ctx);

        // Build image
        boolean success = false;
        try {

            TimeoutRunnableHandler timeoutRunnableHandler = new TimeoutRunnableHandler(15L * 60L * 1000L, () -> {
                unixShellAndFsUtils.executeCommandOrFailWithWorkDir(Level.DEBUG, "CONTAINER/" + imageName, //
                        ctx.getBuildDirectory(), //
                        "/usr/bin/docker", //
                        "build", "-t", imageName, ".");
            });
            timeoutRunnableHandler.run();
            success = true;
        } catch (Exception e) {
        }

        logger.debug("[IMAGE] [{}] BUILD - success: {}", imageName, success);

        return success;
    }

    protected void initQuartz() {
        try {
            SchedulerFactory sf = new StdSchedulerFactory();
            cronScheduler = sf.getScheduler();
        } catch (SchedulerException e) {
            throw new UtilsException("Could not initialize Quartz", e);
        }
    }

    @Override
    public void volumeHostCreate(IPApplicationDefinition applicationDefinition) {
        if (!applicationDefinition.getVolumes().isEmpty()) {
            for (IPApplicationDefinitionVolume volume : applicationDefinition.getVolumes()) {
                logger.info("[VOLUME HOST] Creating volume {}", volume);

                if (volume.getHostFolder() != null) {
                    if (CollectionsTools.isAllItemNotNull(volume.getOwnerId(), volume.getGroupId(), volume.getPermissions())) {
                        unixShellAndFsUtils.folderCreate(hostFs + volume.getHostFolder(), volume.getOwnerId(), volume.getGroupId(), volume.getPermissions());
                    } else {
                        if (!unixShellAndFsUtils.folderExists(hostFs + volume.getHostFolder())) {
                            throw new UtilsException("The folder " + volume.getHostFolder() + " does not exists. Cannot create since there is no specified owner/group/permissions");
                        }
                    }
                } else {
                    logger.info("[VOLUME HOST] Skipping volume {} since it is only used internally (no mount points)", volume);
                }
            }
        }
    }

}
