/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

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
import com.foilen.smalltools.tools.ResourceTools;
import com.foilen.smalltools.tuple.Tuple2;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IPApplicationDefinition {

    // Basic DockerFile
    private String from = "ubuntu:16.04";
    private List<IPApplicationDefinitionBuildStep> buildSteps = new ArrayList<>();
    private Map<Integer, Integer> portsExposed = new LinkedHashMap<>();
    private Map<Integer, Integer> udpPortsExposed = new LinkedHashMap<>();
    private Map<String, String> volumes = new LinkedHashMap<>();
    private Integer runAs = null;
    private String ip;
    private String workingDirectory;
    private String command = "/bin/bash";

    // Extra features
    private List<IPApplicationDefinitionService> services = new ArrayList<>();
    private List<Tuple2<String, Integer>> containerUsersToChangeId = new ArrayList<>();
    private List<Tuple2<String, String>> assetsPathAndContent = new ArrayList<>();
    private List<IPApplicationDefinitionAssetsBundle> assetsBundles = new ArrayList<>();
    private List<Tuple2<String, String>> hostToIpMapping = new ArrayList<>();

    private List<IPApplicationDefinitionPortRedirect> portsRedirect = new ArrayList<>();
    private Map<Integer, String> portsEndpoint = new LinkedHashMap<>();

    // Internal
    @JsonProperty("_nextAssetId")
    private long nextAssetId = 1L;

    public void addAssetContent(String content, String destination) {
        long id = nextAssetId++;
        String assetPath = "_assets/" + id;
        assetsPathAndContent.add(new Tuple2<>(assetPath, content));

        addBuildStepCopy(assetPath, destination);
    }

    public void addAssetResource(String sourceResource, String destination) {
        String content = ResourceTools.getResourceAsString(sourceResource);
        addAssetContent(content, destination);
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

    public void addVolume(String hostFolder, String containerFsFolder) {
        volumes.put(hostFolder, containerFsFolder);
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

    public String getFrom() {
        return from;
    }

    public List<Tuple2<String, String>> getHostToIpMapping() {
        return hostToIpMapping;
    }

    public String getIp() {
        return ip;
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

    public Map<String, String> getVolumes() {
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

    public void setFrom(String from) {
        this.from = from;
    }

    public void setHostToIpMapping(List<Tuple2<String, String>> hostToIpMapping) {
        this.hostToIpMapping = hostToIpMapping;
    }

    public void setIp(String ip) {
        this.ip = ip;
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

    public void setVolumes(Map<String, String> volumes) {
        this.volumes = volumes;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

}
