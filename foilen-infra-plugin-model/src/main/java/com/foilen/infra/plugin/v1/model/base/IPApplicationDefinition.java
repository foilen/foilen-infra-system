/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.base;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.foilen.smalltools.hash.HashSha256;
import com.foilen.smalltools.tools.ResourceTools;
import com.foilen.smalltools.tuple.Tuple2;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IPApplicationDefinition {

    // Image - Basic
    private String from = "ubuntu:16.04";
    private List<IPApplicationDefinitionBuildStep> buildSteps = new ArrayList<>();
    private Map<Integer, Integer> portsExposed = new LinkedHashMap<>();
    private Map<Integer, Integer> udpPortsExposed = new LinkedHashMap<>();
    private Map<Integer, String> portsEndpoint = new LinkedHashMap<>();
    private List<IPApplicationDefinitionVolume> volumes = new ArrayList<>();
    private Map<String, String> environments = new LinkedHashMap<>();
    private Integer runAs = null;
    private String workingDirectory;
    private List<String> entrypoint = null;
    private String command = null;

    // Image - Extra features
    private List<IPApplicationDefinitionService> services = new ArrayList<>();
    private List<Tuple2<String, Integer>> containerUsersToChangeId = new ArrayList<>();
    private List<Tuple2<String, String>> assetsPathAndContent = new ArrayList<>();
    private List<IPApplicationDefinitionAssetsBundle> assetsBundles = new ArrayList<>();
    private List<IPApplicationDefinitionPortRedirect> portsRedirect = new ArrayList<>();

    // Container - Run command
    private List<Tuple2<String, String>> hostToIpMapping = new ArrayList<>();

    // Container - Started
    private List<Tuple2<String, String>> copyWhenStartedPathAndContentFiles = new ArrayList<>();
    private List<String> executeWhenStartedCommands = new ArrayList<>();

    // Internal
    @JsonProperty("_nextAssetId")
    private long nextAssetId = 1L;

    public void addAssetContent(String destination, String content) {
        long id = nextAssetId++;
        String assetPath = "_assets/" + id;
        assetsPathAndContent.add(new Tuple2<>(assetPath, content));

        addBuildStepCopy(assetPath, destination);
    }

    public void addAssetResource(String destination, String sourceResource) {
        String content = ResourceTools.getResourceAsString(sourceResource);
        addAssetContent(destination, content);
    }

    public IPApplicationDefinitionAssetsBundle addAssetsBundle() {
        long id = nextAssetId++;
        String assetsPath = "_assets_" + id + "/";

        IPApplicationDefinitionAssetsBundle assetsBundle = new IPApplicationDefinitionAssetsBundle(assetsPath);
        assetsBundles.add(assetsBundle);
        addBuildStepCopy(assetsPath, "/");
        return assetsBundle;
    }

    public void addBuildStepCommand(String command) {
        buildSteps.add(new IPApplicationDefinitionBuildStep(IPApplicationDefinitionBuildStepType.COMMAND, command));
    }

    public void addBuildStepCopy(String source, String destination) {
        buildSteps.add(new IPApplicationDefinitionBuildStep(IPApplicationDefinitionBuildStepType.COPY, source + " " + destination));
    }

    public void addContainerUserToChangeId(String containerUser, int userId) {
        containerUsersToChangeId.add(new Tuple2<>(containerUser, userId));
    }

    public void addCopyWhenStartedContent(String destination, String content) {
        copyWhenStartedPathAndContentFiles.add(new Tuple2<>(destination, content));
    }

    public void addExecuteWhenStartedCommand(String command) {
        executeWhenStartedCommands.add(command);
    }

    public void addHostToIpMapping(String host, String ip) {
        this.getHostToIpMapping().add(new Tuple2<>(host, ip));
    }

    public void addPortEndpoint(int containerPort, String endpoint) {
        portsEndpoint.put(containerPort, endpoint);
    }

    public void addPortExposed(int hostPort, int containerPort) {
        portsExposed.put(hostPort, containerPort);
    }

    public void addPortRedirect(int localPort, String toMachine, String toDockerName, String toEndpoint) {
        portsRedirect.add(new IPApplicationDefinitionPortRedirect(localPort, toMachine, toDockerName, toEndpoint));
    }

    public void addService(String name, String command) {
        services.add(new IPApplicationDefinitionService(name, command));
    }

    public void addUdpPortExposed(int hostPort, int containerPort) {
        udpPortsExposed.put(hostPort, containerPort);
    }

    public void addVolume(IPApplicationDefinitionVolume volume) {
        volumes.add(volume);
    }

    public List<IPApplicationDefinitionAssetsBundle> getAssetsBundles() {
        return assetsBundles;
    }

    public List<Tuple2<String, String>> getAssetsPathAndContent() {
        return assetsPathAndContent;
    }

    public List<IPApplicationDefinitionBuildStep> getBuildSteps() {
        return buildSteps;
    }

    public String getCommand() {
        return command;
    }

    public List<Tuple2<String, Integer>> getContainerUsersToChangeId() {
        return containerUsersToChangeId;
    }

    public List<Tuple2<String, String>> getCopyWhenStartedPathAndContentFiles() {
        return copyWhenStartedPathAndContentFiles;
    }

    public List<String> getEntrypoint() {
        return entrypoint;
    }

    public Map<String, String> getEnvironments() {
        return environments;
    }

    public List<String> getExecuteWhenStartedCommands() {
        return executeWhenStartedCommands;
    }

    public String getFrom() {
        return from;
    }

    public List<Tuple2<String, String>> getHostToIpMapping() {
        return hostToIpMapping;
    }

    public long getNextAssetId() {
        return nextAssetId;
    }

    public Map<Integer, String> getPortsEndpoint() {
        return portsEndpoint;
    }

    public Map<Integer, Integer> getPortsExposed() {
        return portsExposed;
    }

    public List<IPApplicationDefinitionPortRedirect> getPortsRedirect() {
        return portsRedirect;
    }

    public Integer getRunAs() {
        return runAs;
    }

    public List<IPApplicationDefinitionService> getServices() {
        return services;
    }

    public Map<Integer, Integer> getUdpPortsExposed() {
        return udpPortsExposed;
    }

    public List<IPApplicationDefinitionVolume> getVolumes() {
        return volumes;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setAssetsBundles(List<IPApplicationDefinitionAssetsBundle> assetsBundles) {
        this.assetsBundles = assetsBundles;
    }

    public void setAssetsPathAndContent(List<Tuple2<String, String>> assetsPathAndContent) {
        this.assetsPathAndContent = assetsPathAndContent;
    }

    public void setBuildSteps(List<IPApplicationDefinitionBuildStep> buildSteps) {
        this.buildSteps = buildSteps;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setContainerUsersToChangeId(List<Tuple2<String, Integer>> containerUsersToChangeId) {
        this.containerUsersToChangeId = containerUsersToChangeId;
    }

    public void setCopyWhenStartedPathAndContentFiles(List<Tuple2<String, String>> copyWhenStartedPathAndContentFiles) {
        this.copyWhenStartedPathAndContentFiles = copyWhenStartedPathAndContentFiles;
    }

    public void setEntrypoint(List<String> entrypoint) {
        this.entrypoint = entrypoint;
    }

    public void setEnvironments(Map<String, String> environments) {
        this.environments = environments;
    }

    public void setExecuteWhenStartedCommands(List<String> executeWhenStartedCommands) {
        this.executeWhenStartedCommands = executeWhenStartedCommands;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setHostToIpMapping(List<Tuple2<String, String>> hostToIpMapping) {
        this.hostToIpMapping = hostToIpMapping;
    }

    public void setNextAssetId(long nextAssetId) {
        this.nextAssetId = nextAssetId;
    }

    public void setPortsEndpoint(Map<Integer, String> portsEndpoint) {
        this.portsEndpoint = portsEndpoint;
    }

    public void setPortsExposed(Map<Integer, Integer> portsExposed) {
        this.portsExposed = portsExposed;
    }

    public void setPortsRedirect(List<IPApplicationDefinitionPortRedirect> portsRedirect) {
        this.portsRedirect = portsRedirect;
    }

    public void setRunAs(Integer runAs) {
        this.runAs = runAs;
    }

    public void setServices(List<IPApplicationDefinitionService> services) {
        this.services = services;
    }

    public void setUdpPortsExposed(Map<Integer, Integer> udpPortsExposed) {
        this.udpPortsExposed = udpPortsExposed;
    }

    public void setVolumes(List<IPApplicationDefinitionVolume> volumes) {
        this.volumes = volumes;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * Gives a unique ID depending on the run command fields.
     *
     * @return the unique id
     */
    public String toContainerRunUniqueId() {
        StringBuilder concat = new StringBuilder();
        concat.append(hostToIpMapping);
        return HashSha256.hashString(concat.toString());
    }

    /**
     * Gives a unique ID depending on the commands to execute once started fields.
     *
     * @return the unique id
     */
    public String toContainerStartUniqueId() {
        StringBuilder concat = new StringBuilder();
        concat.append(copyWhenStartedPathAndContentFiles);
        concat.append(executeWhenStartedCommands);
        return HashSha256.hashString(concat.toString());
    }

    /**
     * Gives a unique ID depending on the image fields.
     *
     * @return the unique id
     */
    public String toImageUniqueId() {
        StringBuilder concat = new StringBuilder();
        concat.append(from);
        concat.append(buildSteps);
        concat.append(portsExposed);
        concat.append(udpPortsExposed);
        concat.append(portsEndpoint);
        concat.append(volumes);
        concat.append(environments);
        concat.append(runAs);
        concat.append(workingDirectory);
        concat.append(entrypoint);
        concat.append(command);
        concat.append(services);
        concat.append(containerUsersToChangeId);
        concat.append(assetsPathAndContent);
        concat.append(assetsBundles);
        concat.append(portsRedirect);
        return HashSha256.hashString(concat.toString());
    }

}
