/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.event.Level;

import com.foilen.infra.plugin.system.utils.DockerUtils;
import com.foilen.infra.plugin.system.utils.UnixShellAndFsUtils;
import com.foilen.infra.plugin.system.utils.UtilsException;
import com.foilen.infra.plugin.system.utils.model.ApplicationBuildDetails;
import com.foilen.infra.plugin.system.utils.model.ContainersManageContext;
import com.foilen.infra.plugin.system.utils.model.DockerNetworkInspect;
import com.foilen.infra.plugin.system.utils.model.DockerPs;
import com.foilen.infra.plugin.system.utils.model.DockerPsStatus;
import com.foilen.infra.plugin.system.utils.model.DockerState;
import com.foilen.infra.plugin.system.utils.model.DockerStateFailed;
import com.foilen.infra.plugin.system.utils.model.DockerStateIds;
import com.foilen.infra.plugin.system.utils.model.DockerStateIp;
import com.foilen.infra.plugin.system.utils.model.DockerStep;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionAssetsBundle;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionPortRedirect;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionService;
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
import com.foilen.smalltools.tools.SpaceConverterTools;
import com.foilen.smalltools.tools.SystemTools;
import com.foilen.smalltools.tools.ThreadTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;

public class DockerUtilsImpl extends AbstractBasics implements DockerUtils {

    public static final String NETWORK_NAME = "fcloud";

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

    private UnixShellAndFsUtils unixShellAndFsUtils;

    private Cache<String, List<DockerPs>> containerPsCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

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
    public boolean containerIsRunningByContainerNameOrIdWithCaching(String containerNameOrId) {
        Optional<DockerPs> optional = containerPsFindByContainerNameOrIdWithCaching(containerNameOrId);
        if (!optional.isPresent()) {
            return false;
        }
        return optional.get().getStatus() == DockerPsStatus.Up;
    }

    @Override
    public void containerPsCacheClear() {
        containerPsCache.invalidateAll();
    }

    @Override
    public List<DockerPs> containerPsFindAll() {

        // Get details
        String output = unixShellAndFsUtils.executeCommandQuietAndGetOutput("Docker", "ps", //
                "/usr/bin/docker", //
                "ps", "-a", "--no-trunc", "-s", "--format", "{{.ID}}\t{{.Names}}\t{{.CreatedAt}}\t{{.RunningFor}}\t{{.Status}}\t{{.Size}}");
        logger.debug("Docker ps output: {}", output);

        List<DockerPs> containers = convertToDockerPs(output);

        // Sort
        Collections.sort(containers, (a, b) -> a.getName().compareTo(b.getName()));

        // Cache
        containerPsCache.put("_VALUE_", containers);
        return containers;
    }

    @Override
    public List<DockerPs> containerPsFindAllWithCaching() {
        try {
            return containerPsCache.get("_VALUE_", () -> containerPsFindAll());
        } catch (ExecutionException e) {
            throw new UtilsException("Could not list the containers", e);
        }
    }

    @Override
    public Optional<DockerPs> containerPsFindByContainerNameOrId(String containerNameOrId) {
        return containerPsFindAll().stream().filter(it -> containerNameOrId.equals(it.getName()) || containerNameOrId.equals(it.getId())).findAny();
    }

    @Override
    public Optional<DockerPs> containerPsFindByContainerNameOrIdWithCaching(String containerNameOrId) {
        return containerPsFindAllWithCaching().stream().filter(it -> containerNameOrId.equals(it.getName()) || containerNameOrId.equals(it.getId())).findAny();
    }

    @Override
    public List<String> containersManage(ContainersManageContext containersManageContext) {

        String baseOutputDirectory = containersManageContext.getBaseOutputDirectory();
        AssertTools.assertNotNull(baseOutputDirectory);

        containerPsCacheClear();

        List<String> modifiedContainerNames = new ArrayList<>();
        DockerState dockerState = containersManageContext.getDockerState();

        List<ApplicationBuildDetails> allApplicationBuildDetails = new ArrayList<>();
        allApplicationBuildDetails.addAll(containersManageContext.getAlwaysRunningApplications());

        // Transform cron applications to normal applications
        allApplicationBuildDetails.addAll(containersManageContext.getCronApplications().stream() //
                .map(cronApplicationBuildDetails -> {

                    // Move the main command to a cron entry
                    IPApplicationDefinition applicationDefinition = cronApplicationBuildDetails.getApplicationDefinition();
                    String cronEntry = cronApplicationBuildDetails.getCronTime() + " uid_" + applicationDefinition.getRunAs() + " " + applicationDefinition.getCommand();
                    String containerName = cronApplicationBuildDetails.getOutputContext().getContainerName();
                    applicationDefinition.setCommand(null);
                    logger.info("[MANAGER] [{}] Transforming the main command as cron entry: {}", containerName, cronEntry);
                    applicationDefinition.addAssetContent("/etc/cron.d/main", cronEntry + "\n");

                    // Add the cron service
                    applicationDefinition.getServices().add(new IPApplicationDefinitionService("_cron", "/startCron.sh", 0L));
                    applicationDefinition.addAssetResource("/startCron.sh", "/com/foilen/infra/plugin/system/utils/impl/startCron.sh");
                    applicationDefinition.addBuildStepCommand("chmod +x /startCron.sh && chmod 644 /etc/cron.d/main");

                    // Prepare entries for /etc/passwd
                    StringBuilder cronUsersToInstall = new StringBuilder();
                    cronUsersToInstall.append("uid_").append(applicationDefinition.getRunAs()).append(":").append(applicationDefinition.getRunAs()).append("\n");
                    applicationDefinition.addAssetContent("/cron_users.txt", cronUsersToInstall.toString());

                    return cronApplicationBuildDetails;
                }) //
                .collect(Collectors.toList()) //
        );

        // Check if needs the ports redirector applications (in and out) and add them if needed
        MultiDependenciesResolverTools dependenciesResolver = new MultiDependenciesResolverTools();
        boolean needsRedirectorEntry = allApplicationBuildDetails.stream() //
                .filter(it -> it.getApplicationDefinition().getPortsRedirect().stream() //
                        .filter(pr -> !pr.isToLocalMachine())//
                        .findAny().isPresent()) //
                .findAny().isPresent();
        boolean needsRedirectorExit = false;
        Map<String, ApplicationBuildDetails> applicationBuildDetailsByName = new HashMap<>();
        List<String> remoteMachineContainerEndpointsToSet = new ArrayList<>();
        for (ApplicationBuildDetails applicationBuildDetails : allApplicationBuildDetails) {

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
            DockerContainerOutputContext outputContext = new DockerContainerOutputContext(DockerContainerOutput.REDIRECTOR_ENTRY_CONTAINER_NAME, DockerContainerOutput.REDIRECTOR_ENTRY_CONTAINER_NAME)
                    .setOutputDirectory(baseOutputDirectory + "/" + DockerContainerOutput.REDIRECTOR_ENTRY_CONTAINER_NAME);
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
            Set<RedirectPortRegistryEntry> redirectPortRegistryEntriesEntries = new HashSet<>();
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
                                    redirectPortRegistryEntriesEntries
                                            .add(new RedirectPortRegistryEntry(port, pr.getToMachine(), dockerState.getRedirectorBridgePort(), pr.getToContainerName(), pr.getToEndpoint()));
                                });

                    });

            redirectPortRegistryEntries.setEntries(redirectPortRegistryEntriesEntries.stream().sorted().collect(Collectors.toList()));
            applicationDefinition.addCopyWhenStartedContent("/data/entry.json", JsonTools.prettyPrint(redirectPortRegistryEntries));// We want hot change

            arguments.add("--bridgePort");
            arguments.add(String.valueOf(dockerState.getRedirectorBridgePort()));
            arguments.add("--entryBridgeRegistryFile");
            arguments.add("/data/entry.json");
            applicationDefinition.setCommand(Joiner.on(" ").join(arguments));

            // Add to the list
            applicationBuildDetailsByName.put(DockerContainerOutput.REDIRECTOR_ENTRY_CONTAINER_NAME, new ApplicationBuildDetails() //
                    .setApplicationDefinition(applicationDefinition) //
                    .setOutputContext(outputContext));
        }
        if (needsRedirectorExit) {
            DockerContainerOutputContext outputContext = new DockerContainerOutputContext(DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME, DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME)
                    .setOutputDirectory(baseOutputDirectory + "/" + DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME);
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
            applicationDefinition.setCommand(Joiner.on(" ").join(arguments));

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

        // Remove IPs that were not used in the last hour
        long expired = System.currentTimeMillis() - 60000 * 60;
        dockerState.getIpStateByName().entrySet().removeIf(entry -> entry.getValue().getLastUsed().getTime() < expired);

        // Add the logic to choose an IP
        List<String> applicationsWithoutIp = startOrder.stream().filter(appName -> !dockerState.getIpStateByName().containsKey(appName)).collect(Collectors.toList());
        if (!applicationsWithoutIp.isEmpty()) {
            SearchNextIp searchNextIp = new SearchNextIp(dockerState.getIpStateByName().values().stream().map(it -> it.getIp()));
            applicationsWithoutIp.forEach(appName -> {
                logger.info("[MANAGER] Choose IP for application {}", appName);
                String ip = searchNextIp.getNext();
                logger.info("[MANAGER] Choose IP for application {} : {}", appName, ip);
                dockerState.getIpStateByName().put(appName, new DockerStateIp().setIp(ip));
            });
        }

        // Update time for IPs
        Date now = new Date();
        dockerState.getIpStateByName().values().forEach(it -> {
            it.setLastUsed(now);
        });

        // Update IPs
        logger.debug("[MANAGER] Retrieving current IPs");
        startOrder.forEach(appName -> {
            String ip = dockerState.getIpStateByName().get(appName).getIp();
            logger.debug("[MANAGER] IP of {} is {}", appName, ip);

            String appNameSlash = appName + "/";
            for (String containerEndpoint : dockerState.getRedirectIpByMachineContainerEndpoint().keySet().stream().sorted().collect(Collectors.toList())) {
                if (containerEndpoint.startsWith(appNameSlash)) {
                    logger.debug("[MANAGER] Updating IP of {}", containerEndpoint);
                    dockerState.getRedirectIpByMachineContainerEndpoint().put(containerEndpoint, ip);
                }
            }

        });

        // Set redirectIp/PortByMachineContainerEndpoint for all the endpoints
        DockerStateIp redirectorDockerStateIp = dockerState.getIpStateByName().get(DockerContainerOutput.REDIRECTOR_ENTRY_CONTAINER_NAME);
        if (redirectorDockerStateIp != null) {
            String redirectorEntryIp = redirectorDockerStateIp.getIp();
            for (String mce : remoteMachineContainerEndpointsToSet) {
                dockerState.getRedirectIpByMachineContainerEndpoint().put(mce, redirectorEntryIp);
            }
        }

        // Remove the failed containers details of no more existing applications
        Iterator<String> previousFailedContainersNamesIt = dockerState.getFailedContainersByName().keySet().iterator();
        while (previousFailedContainersNamesIt.hasNext()) {
            String next = previousFailedContainersNamesIt.next();
            if (!startOrder.contains(next)) {
                logger.info("[MANAGER] Remove failed container details for [{}] because it is no more present", next);
                previousFailedContainersNamesIt.remove();
            }
        }

        // Process the applications
        Map<String, DockerStateIds> transformedDockerStateIdsByApplicationName = new HashMap<>();
        for (String applicationNameToStart : startOrder) {
            logger.info("[MANAGER] Processing application [{}]", applicationNameToStart);

            // Get all the needed state and status
            Optional<DockerPs> currentDockerPsOptional = containerPsFindByContainerNameOrIdWithCaching(applicationNameToStart);
            boolean needStart = !currentDockerPsOptional.isPresent();
            if (currentDockerPsOptional.isPresent()) {
                needStart |= currentDockerPsOptional.get().getStatus() != DockerPsStatus.Up;
            }
            DockerStateIds lastRunningContainerIds = dockerState.getRunningContainersByName().remove(applicationNameToStart);
            ApplicationBuildDetails applicationBuildDetails = applicationBuildDetailsByName.get(applicationNameToStart);
            DockerContainerOutputContext ctx = applicationBuildDetails.getOutputContext();
            IPApplicationDefinition applicationDefinition = applicationBuildDetails.getApplicationDefinition();

            // Add redirection details on the context
            ctx.setRedirectIpByMachineContainerEndpoint(dockerState.getRedirectIpByMachineContainerEndpoint());
            ctx.setRedirectPortByMachineContainerEndpoint(dockerState.getRedirectPortByMachineContainerEndpoint());

            // Add network information
            ctx.setNetworkName(NETWORK_NAME);
            ctx.setNetworkIp(dockerState.getIpStateByName().get(applicationNameToStart).getIp());

            // Save redirectPortRegistryExits in the applicationDetails if is that container
            if (DockerContainerOutput.REDIRECTOR_EXIT_CONTAINER_NAME.equals(applicationNameToStart)) {
                applicationDefinition.addCopyWhenStartedContent("/data/exit.json", JsonTools.prettyPrint(redirectPortRegistryExits));// We want hot change
            }

            IPApplicationDefinition transformedApplicationDefinition = DockerContainerOutput.addInfrastructure(applicationDefinition, ctx);
            DockerStateIds currentTransformedDockerStateIds = new DockerStateIds( //
                    transformedApplicationDefinition.toImageUniqueId(), //
                    transformedApplicationDefinition.toContainerRunUniqueId() + ctx.toContainerRunUniqueId(), //
                    transformedApplicationDefinition.toContainerStartUniqueId());
            containersManageContext.getTransformedApplicationDefinitionCallback().handler(applicationNameToStart, transformedApplicationDefinition);

            logger.debug("[MANAGER] [{}] The transformed application definition has ids {}", //
                    applicationNameToStart, currentTransformedDockerStateIds);
            transformedDockerStateIdsByApplicationName.put(applicationNameToStart, currentTransformedDockerStateIds);

            // Exposing endpoints
            String applicationIp = dockerState.getIpStateByName().get(applicationNameToStart).getIp();
            logger.info("[MANAGER] [{}] Has {} enpoints to expose to the exit container ; exposing with ip {}", //
                    applicationNameToStart, transformedApplicationDefinition.getPortsEndpoint().size(), applicationIp);
            if (!transformedApplicationDefinition.getPortsEndpoint().isEmpty()) {
                for (Entry<Integer, String> portEndpoint : transformedApplicationDefinition.getPortsEndpoint().entrySet()) {
                    // Fill redirectPortRegistryExits
                    String endpoint = portEndpoint.getValue();
                    Integer port = portEndpoint.getKey();
                    redirectPortRegistryExits.getExits().add(new RedirectPortRegistryExit(applicationNameToStart, endpoint, applicationIp, port));

                    // Save endpoints in state ("localhost" and current machine name)
                    String machineContainerEndpoint = IPApplicationDefinitionPortRedirect.LOCAL_MACHINE + "/" + applicationNameToStart + "/" + endpoint;
                    dockerState.getRedirectIpByMachineContainerEndpoint().put(machineContainerEndpoint, applicationIp);
                    dockerState.getRedirectPortByMachineContainerEndpoint().put(machineContainerEndpoint, port);
                    machineContainerEndpoint = machineName + "/" + applicationNameToStart + "/" + endpoint;
                    dockerState.getRedirectIpByMachineContainerEndpoint().put(machineContainerEndpoint, applicationIp);
                    dockerState.getRedirectPortByMachineContainerEndpoint().put(machineContainerEndpoint, port);

                }
                Collections.sort(redirectPortRegistryExits.getExits());
            }

            // Check the steps to execute
            DockerStep startStep = DockerStep.COMPLETED;
            if (lastRunningContainerIds == null) {
                logger.debug("[MANAGER] [{}] is not currently running. Will build and start", applicationNameToStart);
                startStep = DockerStep.BUILD_IMAGE;
            } else {
                if (!lastRunningContainerIds.getImageUniqueId().equals(currentTransformedDockerStateIds.getImageUniqueId())) {
                    logger.debug("[MANAGER] [{}] has a different image {} -> {}. Will build and start", applicationNameToStart, //
                            lastRunningContainerIds.getImageUniqueId(), currentTransformedDockerStateIds.getImageUniqueId());
                    startStep = DockerStep.BUILD_IMAGE;
                } else if (!lastRunningContainerIds.getContainerRunUniqueId().equals(currentTransformedDockerStateIds.getContainerRunUniqueId())) {
                    logger.debug("[MANAGER] [{}] has a different run command {} -> {}. Will restart", applicationNameToStart, //
                            lastRunningContainerIds.getContainerRunUniqueId(), currentTransformedDockerStateIds.getContainerRunUniqueId());
                    startStep = DockerStep.RESTART_CONTAINER;
                } else if (needStart) {
                    logger.debug("[MANAGER] [{}] is not running. Will build and start", applicationNameToStart);
                    containerStopAndRemove(ctx);
                    startStep = DockerStep.BUILD_IMAGE;
                } else if (!lastRunningContainerIds.getContainerStartedUniqueId().equals(currentTransformedDockerStateIds.getContainerStartedUniqueId())) {
                    logger.debug("[MANAGER] [{}] has a different execute when started commands. Will execute", applicationNameToStart);
                    startStep = DockerStep.COPY_AND_EXECUTE_IN_RUNNING_CONTAINER;
                }
            }
            if (startStep != DockerStep.COMPLETED) {
                modifiedContainerNames.add(applicationNameToStart);
            }

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
                DockerStateFailed dockerStateFailed = dockerState.getFailedContainersByName().get(applicationNameToStart);
                boolean saveFailedState = true;
                if (dockerStateFailed != null) {
                    if (currentTransformedDockerStateIds.idsEquals(dockerStateFailed.getDockerStateIds())) {
                        saveFailedState = false;
                    }
                }
                if (saveFailedState) {
                    logger.error("[MANAGER] [{}] Updating the last failure date", applicationNameToStart);
                    dockerState.getFailedContainersByName().put(applicationNameToStart, new DockerStateFailed(currentTransformedDockerStateIds, new Date()));
                }

                // Keep previous success in running if still running
                if (lastRunningContainerIds != null && containerIsRunningByContainerNameOrIdWithCaching(applicationNameToStart)) {
                    dockerState.getRunningContainersByName().put(applicationNameToStart, lastRunningContainerIds);
                }
                continue;
            }

            if (startStep == DockerStep.BUILD_IMAGE || startStep == DockerStep.RESTART_CONTAINER) {
                logger.info("[MANAGER] [{}] Output image", applicationNameToStart);
                imageOutput(transformedApplicationDefinition, ctx);
            }

            switch (startStep) {
            case BUILD_IMAGE:
                logger.info("[MANAGER] [{}] Building image", applicationNameToStart);
                currentTransformedDockerStateIds.setLastState(DockerStep.BUILD_IMAGE);
                if (!imageBuild(transformedApplicationDefinition, ctx)) {
                    logger.error("[MANAGER] [{}] Could not build the image", applicationNameToStart);
                    dockerState.getFailedContainersByName().put(applicationNameToStart, new DockerStateFailed(currentTransformedDockerStateIds, new Date()));

                    // Keep previous success in running if still running
                    if (lastRunningContainerIds != null && containerIsRunningByContainerNameOrIdWithCaching(applicationNameToStart)) {
                        dockerState.getRunningContainersByName().put(applicationNameToStart, lastRunningContainerIds);
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
                    dockerState.getFailedContainersByName().put(applicationNameToStart, new DockerStateFailed(currentTransformedDockerStateIds, new Date()));
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
                dockerState.getFailedContainersByName().remove(applicationNameToStart);
                break;
            }

        }

        // Check ending state
        ThreadTools.sleep(5000);
        containerPsCacheClear();
        for (String applicationNameThatShouldBeStarted : startOrder) {
            Optional<DockerPs> containerOptional = containerPsFindByContainerNameOrIdWithCaching(applicationNameThatShouldBeStarted);
            if (containerOptional.isPresent()) {
                dockerState.getRunningContainersByName().put(applicationNameThatShouldBeStarted, transformedDockerStateIdsByApplicationName.get(applicationNameThatShouldBeStarted));
            } else {
                logger.error("[MANAGER] [{}] Could not find it running", applicationNameThatShouldBeStarted);
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

        boolean success = false;
        try {
            unixShellAndFsUtils.executeCommandOrFailWithWorkDir(Level.DEBUG, "CONTAINER/" + containerName, //
                    ctx.getOutputDirectory(), //
                    "./start-restart.sh");
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

        boolean success = false;

        try {
            logger.info("[CONTAINER] [{}] REMOVE", containerNameOrId);
            unixShellAndFsUtils.executeCommandOrFail(Level.DEBUG, "CONTAINER/" + containerNameOrId, //
                    "/usr/bin/docker", //
                    "rm", "-f", containerNameOrId);
            success = true;
        } catch (Exception e) {
        }

        logger.debug("[CONTAINER] [{}] REMOVE - success: {}", containerNameOrId, success);
        return success;

    }

    protected List<DockerPs> convertToDockerPs(String output) {
        List<DockerPs> results = new ArrayList<>();
        FileLinesIterable fileLinesIterable = new FileLinesIterable();
        fileLinesIterable.openString(output);
        for (String line : fileLinesIterable) {
            String[] parts = line.split("\t");
            if (parts.length < 6) {
                continue;
            }
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

            String instanceSizePart = sizeParts[0];
            try {
                long instanceSize = SpaceConverterTools.convertToBytes(instanceSizePart);
                dockerPs.setSize(instanceSize);
            } catch (Exception e) {
                logger.warn("Could not convert {}", instanceSizePart, e);
            }

            String totalSizePart = sizeParts[1].split("\\)")[0];
            try {
                long totalSize = SpaceConverterTools.convertToBytes(totalSizePart);
                dockerPs.setTotalSize(totalSize);
            } catch (Exception e) {
                logger.warn("Could not convert {}", totalSizePart, e);
            }

            results.add(dockerPs);
        }

        return results;
    }

    @Override
    public boolean imageBuild(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx) {
        String imageName = ctx.getImageName();
        logger.info("[IMAGE] [{}] BUILD", imageName);

        // Build image
        boolean success = false;
        try {

            TimeoutRunnableHandler timeoutRunnableHandler = new TimeoutRunnableHandler(15L * 60L * 1000L, () -> {
                unixShellAndFsUtils.executeCommandOrFailWithWorkDir(Level.DEBUG, "CONTAINER/" + imageName, //
                        ctx.getOutputDirectory(), //
                        "./build.sh");
            });
            timeoutRunnableHandler.run();
            success = true;
        } catch (Exception e) {
        }

        logger.debug("[IMAGE] [{}] BUILD - success: {}", imageName, success);

        return success;
    }

    @Override
    public void imageOutput(IPApplicationDefinition applicationDefinition, DockerContainerOutputContext ctx) {
        String imageName = ctx.getImageName();
        logger.info("[IMAGE] [{}] OUTPUT", imageName);

        // Prepare the build directory
        DockerContainerOutput.toDockerBuildDirectory(applicationDefinition, ctx);

        logger.debug("[IMAGE] [{}] OUTPUT - done", imageName);

    }

    protected List<String> networkConvertToNames(String output) {
        FileLinesIterable linesIterable = new FileLinesIterable();
        linesIterable.openString(output);
        linesIterable.next(); // Skip header
        List<String> names = new ArrayList<>();
        while (linesIterable.hasNext()) {
            String line = linesIterable.next();
            int start = 20;
            int end = line.indexOf(' ', start);
            names.add(line.substring(start, end));
        }
        return names;
    }

    @Override
    public void networkCreateIfNotExists(String name, String subnet) {
        if (!networkListNames().contains(name)) {
            logger.info("[NETWORK/{}] Creating network with subnet {}", subnet);
            unixShellAndFsUtils.executeCommandOrFail(Level.DEBUG, "NETWORK/" + name, //
                    "/usr/bin/docker", "network", //
                    "create", "--subnet", subnet, name);
        }

    }

    @Override
    public Map<String, String> networkListIpByContainerName(String name) {

        // Get details
        String output = unixShellAndFsUtils.executeCommandQuietAndGetOutput("NETWORK", "inspect", "/usr/bin/docker", "network", //
                "inspect", name);
        logger.debug("Docker network inspect output: {}", output);

        Map<String, String> ipByContainerName = networkListIpByContainerNameConvert(output);

        return ipByContainerName;
    }

    protected Map<String, String> networkListIpByContainerNameConvert(String output) {
        Map<String, String> ipByContainerName = new TreeMap<String, String>();

        List<DockerNetworkInspect> inspects = JsonTools.readFromStringAsList(output, DockerNetworkInspect.class);
        inspects.forEach(inspect -> {
            inspect.getContainers().values().forEach(container -> {
                String ipv4 = container.getIpv4().split("/")[0];
                ipByContainerName.put(container.getName(), ipv4);
            });
        });

        return ipByContainerName;
    }

    @Override
    public List<String> networkListNames() {
        String output = unixShellAndFsUtils.executeCommandQuietAndGetOutput("NETWORK", "listing", "/usr/bin/docker", "network", //
                "ls");

        return networkConvertToNames(output);
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
                            unixShellAndFsUtils.folderCreateWithParentOwnerAndGroup(hostFs + volume.getHostFolder());
                        }
                    }
                } else {
                    logger.info("[VOLUME HOST] Skipping volume {} since it is only used internally (no mount points)", volume);
                }
            }
        }
    }

}
