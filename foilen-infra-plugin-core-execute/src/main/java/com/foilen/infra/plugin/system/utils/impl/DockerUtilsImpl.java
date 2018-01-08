/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.event.Level;

import com.foilen.infra.plugin.system.utils.DockerUtils;
import com.foilen.infra.plugin.system.utils.UnixShellAndFsUtils;
import com.foilen.infra.plugin.system.utils.callback.DockerContainerManagementCallback;
import com.foilen.infra.plugin.system.utils.callback.NoOpDockerContainerManagementCallback;
import com.foilen.infra.plugin.system.utils.model.DockerPs;
import com.foilen.infra.plugin.system.utils.model.DockerPsStatus;
import com.foilen.infra.plugin.system.utils.model.DockerState;
import com.foilen.infra.plugin.system.utils.model.DockerStateIds;
import com.foilen.infra.plugin.system.utils.model.DockerStep;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionAssetsBundle;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionPortRedirect;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionVolume;
import com.foilen.infra.plugin.v1.model.outputter.DockerMissingDependencyException;
import com.foilen.infra.plugin.v1.model.outputter.docker.DockerContainerOutput;
import com.foilen.infra.plugin.v1.model.outputter.docker.DockerContainerOutputContext;
import com.foilen.infra.plugin.v1.model.redirectportregistry.RedirectPortRegistryExit;
import com.foilen.infra.plugin.v1.model.redirectportregistry.RedirectPortRegistryExits;
import com.foilen.smalltools.JavaEnvironmentValues;
import com.foilen.smalltools.consolerunner.ConsoleRunner;
import com.foilen.smalltools.iterable.FileLinesIterable;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.CollectionsTools;
import com.foilen.smalltools.tools.DirectoryTools;
import com.foilen.smalltools.tools.ExecutorsTools;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.MultiDependenciesResolverTools;
import com.foilen.smalltools.tools.SpaceConverterTool;
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

    public static void main(String[] args) {
        DockerUtils dockerUtils = new DockerUtilsImpl();

        System.out.println("---[ Listing all containers ]---");
        for (DockerPs dockerPs : dockerUtils.containerPsFindAll()) {
            System.out.println(dockerPs);
        }
    }

    private String hostFs = SystemTools.getPropertyOrEnvironment("HOSTFS", "/");

    private String machineName = JavaEnvironmentValues.getHostName();

    private UnixShellAndFsUtils unixShellAndFsUtils;

    public DockerUtilsImpl() {
        unixShellAndFsUtils = new UnixShellAndFsUtilsImpl();
    }

    public DockerUtilsImpl(UnixShellAndFsUtils unixShellAndFsUtils) {
        this.unixShellAndFsUtils = unixShellAndFsUtils;
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

        int statusCode = new ConsoleRunner().setCommand("/usr/bin/docker") //
                .addArguments("cp", tmpDir.getAbsolutePath() + "/.", containerName + ":" + containerFolder) //
                .executeWithLogger(logger, Level.DEBUG);

        logger.debug("[CONTAINER] [{}] COPY file - statusCode: {}", containerName, statusCode);

        DirectoryTools.deleteFolder(tmpDir);

        return statusCode == 0;
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

        int statusCode = new ConsoleRunner().setCommand("/usr/bin/docker") //
                .addArguments("exec", "-i", containerName, "/bin/bash", "-c", command) //
                .executeWithLogger(logger, Level.DEBUG);

        logger.debug("[CONTAINER] [{}] EXEC command - statusCode: {}", containerName, statusCode);

        return statusCode == 0;
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
        String output = new ConsoleRunner().setCommand("/usr/bin/docker") //
                .addArguments("ps", "-a", "--no-trunc", "-s", "--format", "{{.ID}}\t{{.Names}}\t{{.CreatedAt}}\t{{.RunningFor}}\t{{.Status}}\t{{.Size}}") //
                .executeForString();
        List<DockerPs> containers = convertToDockerPs(output);

        // Get IP
        for (DockerPs container : containers) {
            output = new ConsoleRunner().setCommand("/usr/bin/docker") //
                    .addArguments("inspect", "--format", "{{ .NetworkSettings.IPAddress }}", container.getName()) //
                    .executeForString();
            output = output.replaceAll("\r", "");
            output = output.replaceAll("\n", "");
            if (!Strings.isNullOrEmpty(output)) {
                container.setIp(output);
            }
        }

        // Sort
        Collections.sort(containers, (a, b) -> a.getName().compareTo(b.getName()));
        return containers;
    }

    @Override
    public Optional<DockerPs> containerPsFindByContainerNameOrId(String containerNameOrId) {
        return containerPsFindAll().stream().filter(it -> containerNameOrId.equals(it.getName()) || containerNameOrId.equals(it.getId())).findAny();
    }

    @Override
    public void containersManage(DockerState dockerState, List<Tuple2<DockerContainerOutputContext, IPApplicationDefinition>> outputContextAndApplicationDefinitions) {
        containersManage(dockerState, outputContextAndApplicationDefinitions, new NoOpDockerContainerManagementCallback());
    }

    @Override
    public void containersManage(DockerState dockerState, List<Tuple2<DockerContainerOutputContext, IPApplicationDefinition>> outputContextAndApplicationDefinitions,
            DockerContainerManagementCallback containerManagementCallback) {

        // Check if needs the ports redirector applications (in and out) and add them if needed
        MultiDependenciesResolverTools dependenciesResolver = new MultiDependenciesResolverTools();
        boolean needsRedirectorEntry = false;
        boolean needsRedirectorExit = false;
        Map<String, Tuple2<DockerContainerOutputContext, IPApplicationDefinition>> outputContextAndApplicationDefinitionByName = new HashMap<>();
        for (Tuple2<DockerContainerOutputContext, IPApplicationDefinition> outputContextAndApplicationDefinition : outputContextAndApplicationDefinitions) {

            // Add the app to the dependencies resolver
            String containerName = outputContextAndApplicationDefinition.getA().getContainerName();
            dependenciesResolver.addItems(containerName);

            // Add to the map
            outputContextAndApplicationDefinitionByName.put(containerName, outputContextAndApplicationDefinition);

            // Exposing endpoints -> needs exit
            IPApplicationDefinition applicationDefinition = outputContextAndApplicationDefinition.getB();
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
            // TODO REDIRECTOR_ENTRY - Implement application
            throw new DockerMissingDependencyException("Not implemented " + DockerContainerOutput.REDIRECTOR_ENTRY_CONTAINER_NAME);
        }
        if (needsRedirectorExit) {
            DockerContainerOutputContext outputContext = new DockerContainerOutputContext(DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME, DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME);
            IPApplicationDefinition applicationDefinition = new IPApplicationDefinition();
            applicationDefinition.setFrom("foilen/redirectport-registry:1.1.0");
            applicationDefinition.setRunAs(65534); // Nobody

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
            assetBundle.addAssetContent("/data/exit.json", JsonTools.prettyPrint(redirectPortRegistryExits));

            arguments.add("--bridgePort");
            arguments.add(String.valueOf(dockerState.getRedirectorBridgePort()));
            arguments.add("--exitBridgeRegistryFile");
            arguments.add("/data/exit.json");
            applicationDefinition.setCommand(JsonTools.compactPrint(arguments));

            // Add to the list
            Tuple2<DockerContainerOutputContext, IPApplicationDefinition> outputContextAndApplicationDefinition = new Tuple2<>(outputContext, applicationDefinition);
            outputContextAndApplicationDefinitions.add(outputContextAndApplicationDefinition);
            outputContextAndApplicationDefinitionByName.put(DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME, outputContextAndApplicationDefinition);
        }

        // Stop any non-needed applications
        List<String> neededContainerNames = outputContextAndApplicationDefinitions.stream() //
                .map(it -> it.getA().getContainerName()) //
                .sorted() //
                .collect(Collectors.toList());
        List<String> runningContainerNames = dockerState.getRunningContainersByName().keySet().stream() //
                .sorted() //
                .collect(Collectors.toList());
        runningContainerNames.removeAll(neededContainerNames);
        for (String containerToStop : runningContainerNames) {
            logger.info("[MANAGER] Stop extra applications [{}]", containerToStop);
            containerStopAndRemove(containerToStop);
        }

        // Check starting order by dependencies
        List<String> startOrder = dependenciesResolver.getExecution();
        logger.debug("[MANAGER] Starting order: {}", startOrder);

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
            boolean dependsOnRedirectorEntry = false; // TODO REDIRECTOR_ENTRY - dependsOnRedirectorEntry
            DockerStateIds runningContainer = dockerState.getRunningContainersByName().get(applicationNameToStart);
            Tuple2<DockerContainerOutputContext, IPApplicationDefinition> outputContextAndApplicationDefinition = outputContextAndApplicationDefinitionByName.get(applicationNameToStart);
            DockerContainerOutputContext ctx = outputContextAndApplicationDefinition.getA();
            IPApplicationDefinition applicationDefinition = outputContextAndApplicationDefinition.getB();

            // Add redirection details on the context
            ctx.setRedirectIpByMachineContainerEndpoint(dockerState.getRedirectIpByMachineContainerEndpoint());
            ctx.setRedirectPortByMachineContainerEndpoint(dockerState.getRedirectPortByMachineContainerEndpoint());

            // Save redirectPortRegistryExits in the applicationDetails if is that container
            if (DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME.equals(applicationNameToStart)) {
                applicationDefinition.addCopyWhenStartedContent("/data/exit.json", JsonTools.prettyPrint(redirectPortRegistryExits));
            }

            // Check the steps to execute
            DockerStep startStep = DockerStep.COMPLETED;
            if (runningContainer == null) {
                logger.debug("[MANAGER] [{}] is not currently running. Will build and start", applicationNameToStart);
                startStep = DockerStep.BUILD_IMAGE;
            } else {
                if ((dependsOnRedirectorEntry && existingRedirectorEntryPortOrHostChanged) || !runningContainer.getImageUniqueId().equals(applicationDefinition.toImageUniqueId())) {
                    logger.debug("[MANAGER] [{}] has a different image. Will build and start", applicationNameToStart);
                    startStep = DockerStep.BUILD_IMAGE;
                } else if (!runningContainer.getContainerRunUniqueId().equals(applicationDefinition.toContainerRunUniqueId())) {
                    logger.debug("[MANAGER] [{}] has a different run command. Will restart", applicationNameToStart);
                    startStep = DockerStep.RESTART_CONTAINER;
                } else if (needStart) {
                    logger.debug("[MANAGER] [{}] is not running. Will restart", applicationNameToStart);
                    startStep = DockerStep.RESTART_CONTAINER;
                } else if (!runningContainer.getContainerStartedUniqueId().equals(applicationDefinition.toContainerStartUniqueId())) {
                    logger.debug("[MANAGER] [{}] has a different execute when started commands. Will execute", applicationNameToStart);
                    startStep = DockerStep.COPY_AND_EXECUTE_IN_RUNNING_CONTAINER;
                }
            }

            // Clear the state
            String lastRunningIp = dockerState.getIpByName().remove(applicationNameToStart);
            DockerStateIds lastRunningDockerStateIds = dockerState.getRunningContainersByName().remove(applicationNameToStart);

            // If needs any change, stop any running command
            if (startStep != DockerStep.COMPLETED) {
                Future<Boolean> executionFuture = dockerState.getExecutionsFutures().get(applicationNameToStart);
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
            if (!containerManagementCallback.proceedWithTransformedContainer(applicationNameToStart, dockerStateIds)) {
                logger.error("[MANAGER] [{}] The callback requested to not proceed with this container", applicationNameToStart);
                dockerState.getFailedContainersByName().put(applicationNameToStart, dockerStateIds);

                // Keep previous success in running if still running
                if (lastRunningDockerStateIds != null && containerIsRunningByContainerNameOrId(applicationNameToStart)) {
                    dockerState.getRunningContainersByName().put(applicationNameToStart, lastRunningDockerStateIds);
                    dockerState.getIpByName().put(applicationNameToStart, lastRunningIp);
                }
                continue;
            }

            switch (startStep) {
            case BUILD_IMAGE:
                logger.info("[MANAGER] [{}] Building image", applicationNameToStart);
                dockerStateIds.setLastState(DockerStep.BUILD_IMAGE);
                if (!imageBuild(transformedApplicationDefinition, ctx)) {
                    logger.error("[MANAGER] [{}] Could not build the image", applicationNameToStart);
                    dockerState.getFailedContainersByName().put(applicationNameToStart, dockerStateIds);

                    // Keep previous success in running if still running
                    if (lastRunningDockerStateIds != null && containerIsRunningByContainerNameOrId(applicationNameToStart)) {
                        dockerState.getRunningContainersByName().put(applicationNameToStart, lastRunningDockerStateIds);
                        dockerState.getIpByName().put(applicationNameToStart, lastRunningIp);
                    }
                    continue;
                }
                volumeHostCreate(transformedApplicationDefinition);
            case RESTART_CONTAINER:
                logger.info("[MANAGER] [{}] Starting/Restarting container", applicationNameToStart);
                dockerStateIds.setLastState(DockerStep.RESTART_CONTAINER);
                containerStopAndRemove(ctx);
                if (!containerStartWithRestart(transformedApplicationDefinition, ctx)) {
                    logger.error("[MANAGER] [{}] Could not start the container", applicationNameToStart);
                    dockerState.getFailedContainersByName().put(applicationNameToStart, dockerStateIds);
                    continue;
                }
            case COPY_AND_EXECUTE_IN_RUNNING_CONTAINER:
                logger.info("[MANAGER] [{}] Copying files in the running container", applicationNameToStart);
                dockerStateIds.setLastState(DockerStep.COPY_AND_EXECUTE_IN_RUNNING_CONTAINER);
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
                dockerStateIds.setLastState(DockerStep.COMPLETED);
                break;
            }

            // Note IP and new state
            String ip = null;
            for (int i = 0; i < 3 && (!dockerState.getIpByName().containsKey(applicationNameToStart)); ++i) {
                if (i != 0) {
                    ThreadTools.sleep(1000);
                }
                Optional<DockerPs> containerOptional = containerPsFindByContainerNameOrId(applicationNameToStart);
                if (containerOptional.isPresent()) {
                    DockerPs container = containerOptional.get();
                    ip = container.getIp();
                    dockerState.getIpByName().put(applicationNameToStart, ip);
                    dockerState.getRunningContainersByName().put(applicationNameToStart, dockerStateIds);
                } else {
                    logger.error("[MANAGER] [{}] Could not find it running", applicationNameToStart);
                }
            }

            // Exposing endpoints
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
            // TODO REDIRECTOR_ENTRY - (also the redirector entries if it is this one) ; see if setting existingRedirectorEntryPortOrHostChanged to true

        }

    }

    @Override
    public boolean containerStartOnce(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx) {
        String containerName = ctx.getContainerName();
        logger.info("[CONTAINER] [{}] START", containerName);

        String[] runArguments = DockerContainerOutput.toRunArgumentsSinglePassDetached(applicationDefinition, ctx, false);
        logger.debug("[CONTAINER] [{}] START - Arguments: {}", containerName, runArguments);

        int statusCode = new ConsoleRunner().setCommand("/usr/bin/docker") //
                .addArguments(runArguments) //
                .executeWithLogger(logger, Level.DEBUG);

        logger.debug("[CONTAINER] [{}] START - statusCode: {}", containerName, statusCode);
        return statusCode == 0;
    }

    @Override
    public boolean containerStartWithRestart(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx) {
        String containerName = ctx.getContainerName();
        logger.info("[CONTAINER] [{}] START", containerName);

        String[] runArguments = DockerContainerOutput.toRunArgumentsWithRestart(applicationDefinition, ctx);
        logger.debug("[CONTAINER] [{}] START - Arguments: {}", containerName, runArguments);

        int statusCode = new ConsoleRunner().setCommand("/usr/bin/docker") //
                .addArguments(runArguments) //
                .executeWithLogger(logger, Level.DEBUG);

        logger.debug("[CONTAINER] [{}] START - statusCode: {}", containerName, statusCode);
        return statusCode == 0;
    }

    @Override
    public boolean containerStopAndRemove(DockerContainerOutputContext ctx) {
        return containerStopAndRemove(ctx.getContainerName());
    }

    @Override
    public boolean containerStopAndRemove(String containerNameOrId) {

        Optional<DockerPs> container = containerPsFindByContainerNameOrId(containerNameOrId);
        boolean exists = container.isPresent();
        int statusCode = 0;

        if (exists) {

            DockerPs dockerPs = container.get();
            if (!stoppedStatuses.contains(dockerPs.getStatus())) {

                logger.info("[CONTAINER] [{}] STOP", containerNameOrId);
                statusCode = new ConsoleRunner().setCommand("/usr/bin/docker") //
                        .addArguments("stop", containerNameOrId) //
                        .executeWithLogger(logger, Level.DEBUG);

                logger.debug("[CONTAINER] [{}] STOP - statusCode: {}", containerNameOrId, statusCode);
            }

            if (statusCode == 0) {
                logger.info("[CONTAINER] [{}] REMOVE", containerNameOrId);
                statusCode = new ConsoleRunner().setCommand("/usr/bin/docker") //
                        .addArguments("rm", containerNameOrId) //
                        .executeWithLogger(logger, Level.DEBUG);

                logger.debug("[CONTAINER] [{}] REMOVE - statusCode: {}", containerNameOrId, statusCode);
            }
        } else {
            logger.info("[CONTAINER] [{}] STOP and REMOVE. Already not present", containerNameOrId);
        }

        return statusCode == 0;

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
    public boolean imageBuild(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx) {
        String imageName = ctx.getImageName();
        logger.info("[IMAGE] [{}] BUILD", imageName);

        // Prepare the build directory
        DockerContainerOutput.toDockerBuildDirectory(applicationDefinition, ctx);

        // Build image
        int statusCode = new ConsoleRunner().setCommand("/usr/bin/docker") //
                .addArguments("build", "-t", imageName, ".") //
                .setWorkingDirectory(ctx.getBuildDirectory()) //
                .setRedirectErrorStream(true) //
                .setTimeoutInMilliseconds(15L * 60L * 1000L) //
                .executeWithLogger(logger, Level.DEBUG);

        logger.debug("[IMAGE] [{}] BUILD - statusCode: {}", imageName, statusCode);

        return statusCode == 0;
    }

    @Override
    public void volumeHostCreate(IPApplicationDefinition applicationDefinition) {
        if (!applicationDefinition.getVolumes().isEmpty()) {
            for (IPApplicationDefinitionVolume volume : applicationDefinition.getVolumes()) {
                logger.info("[VOLUME HOST] Creating volume {}", volume.getHostFolder());
                unixShellAndFsUtils.folderCreate(hostFs + volume.getHostFolder(), volume.getOwnerId(), volume.getGroupId(), volume.getPermissions());
            }
        }
    }

}
