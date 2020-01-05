/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.foilen.smalltools.tools.AbstractBasics;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerState extends AbstractBasics {

    // Running state
    private Map<String, DockerStateIds> runningContainersByName = new HashMap<>();
    private Map<String, DockerStateFailed> failedContainersByName = new HashMap<>();
    private Map<String, DockerStateIp> ipStateByName = new HashMap<>();

    // Cron
    private Map<String, DockerStateIds> cronContainersByName = new HashMap<>();
    private Map<String, String> cronTimeByName = new HashMap<>();

    // Redirection details
    private int redirectorBridgePort = 11000;
    private List<String> redirectorCaCerts = new ArrayList<>();
    private String redirectorNodeCert;
    private String redirectorNodeKey;
    private Map<String, Integer> redirectPortByMachineContainerEndpoint = new HashMap<>();
    private Map<String, String> redirectIpByMachineContainerEndpoint = new HashMap<>();

    // Executions
    @JsonIgnore
    private Map<String, Future<Boolean>> executionsFutures = new HashMap<>();

    public Map<String, DockerStateIds> getCronContainersByName() {
        return cronContainersByName;
    }

    public Map<String, String> getCronTimeByName() {
        return cronTimeByName;
    }

    public Map<String, Future<Boolean>> getExecutionsFutures() {
        return executionsFutures;
    }

    public Map<String, DockerStateFailed> getFailedContainersByName() {
        return failedContainersByName;
    }

    public Map<String, DockerStateIp> getIpStateByName() {
        return ipStateByName;
    }

    public Map<String, String> getRedirectIpByMachineContainerEndpoint() {
        return redirectIpByMachineContainerEndpoint;
    }

    public int getRedirectorBridgePort() {
        return redirectorBridgePort;
    }

    public List<String> getRedirectorCaCerts() {
        return redirectorCaCerts;
    }

    public String getRedirectorNodeCert() {
        return redirectorNodeCert;
    }

    public String getRedirectorNodeKey() {
        return redirectorNodeKey;
    }

    public Map<String, Integer> getRedirectPortByMachineContainerEndpoint() {
        return redirectPortByMachineContainerEndpoint;
    }

    public Map<String, DockerStateIds> getRunningContainersByName() {
        return runningContainersByName;
    }

    public void setCronContainersByName(Map<String, DockerStateIds> cronContainersByName) {
        this.cronContainersByName = cronContainersByName;
    }

    public void setCronTimeByName(Map<String, String> cronTimeByName) {
        this.cronTimeByName = cronTimeByName;
    }

    public void setExecutionsFutures(Map<String, Future<Boolean>> executionsFutures) {
        this.executionsFutures = executionsFutures;
    }

    public void setFailedContainersByName(Map<String, DockerStateFailed> failedContainersByName) {
        this.failedContainersByName = failedContainersByName;
    }

    public void setIpStateByName(Map<String, DockerStateIp> ipStateByName) {
        this.ipStateByName = ipStateByName;
    }

    public void setRedirectIpByMachineContainerEndpoint(Map<String, String> redirectIpByMachineContainerEndpoint) {
        this.redirectIpByMachineContainerEndpoint = redirectIpByMachineContainerEndpoint;
    }

    public void setRedirectorBridgePort(int redirectorBridgePort) {
        this.redirectorBridgePort = redirectorBridgePort;
    }

    public void setRedirectorCaCerts(List<String> redirectorCaCerts) {
        this.redirectorCaCerts = redirectorCaCerts;
    }

    public void setRedirectorNodeCert(String redirectorNodeCert) {
        this.redirectorNodeCert = redirectorNodeCert;
    }

    public void setRedirectorNodeKey(String redirectorNodeKey) {
        this.redirectorNodeKey = redirectorNodeKey;
    }

    public void setRedirectPortByMachineContainerEndpoint(Map<String, Integer> redirectPortByMachineContainerEndpoint) {
        this.redirectPortByMachineContainerEndpoint = redirectPortByMachineContainerEndpoint;
    }

    public void setRunningContainersByName(Map<String, DockerStateIds> runningContainersByName) {
        this.runningContainersByName = runningContainersByName;
    }

}
