/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.redirectportregistry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ComparisonChain;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RedirectPortRegistryExit implements Comparable<RedirectPortRegistryExit> {

    private String serviceName;
    private String serviceEndpoint;
    private String exitRawHost;
    private int exitRawPort;

    public RedirectPortRegistryExit() {
    }

    public RedirectPortRegistryExit(String serviceName, String serviceEndpoint, String exitRawHost, int exitRawPort) {
        this.serviceName = serviceName;
        this.serviceEndpoint = serviceEndpoint;
        this.exitRawHost = exitRawHost;
        this.exitRawPort = exitRawPort;
    }

    @Override
    public int compareTo(RedirectPortRegistryExit o) {
        ComparisonChain cc = ComparisonChain.start();
        cc = cc.compare(serviceName, o.serviceName);
        cc = cc.compare(serviceEndpoint, o.serviceEndpoint);
        cc = cc.compare(exitRawHost, o.exitRawHost);
        cc = cc.compare(exitRawPort, o.exitRawPort);
        return cc.result();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RedirectPortRegistryExit other = (RedirectPortRegistryExit) obj;
        if (exitRawHost == null) {
            if (other.exitRawHost != null) {
                return false;
            }
        } else if (!exitRawHost.equals(other.exitRawHost)) {
            return false;
        }
        if (exitRawPort != other.exitRawPort) {
            return false;
        }
        if (serviceEndpoint == null) {
            if (other.serviceEndpoint != null) {
                return false;
            }
        } else if (!serviceEndpoint.equals(other.serviceEndpoint)) {
            return false;
        }
        if (serviceName == null) {
            if (other.serviceName != null) {
                return false;
            }
        } else if (!serviceName.equals(other.serviceName)) {
            return false;
        }
        return true;
    }

    public String getExitRawHost() {
        return exitRawHost;
    }

    public int getExitRawPort() {
        return exitRawPort;
    }

    public String getServiceEndpoint() {
        return serviceEndpoint;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((exitRawHost == null) ? 0 : exitRawHost.hashCode());
        result = prime * result + exitRawPort;
        result = prime * result + ((serviceEndpoint == null) ? 0 : serviceEndpoint.hashCode());
        result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
        return result;
    }

    public void setExitRawHost(String exitRawHost) {
        this.exitRawHost = exitRawHost;
    }

    public void setExitRawPort(int exitRawPort) {
        this.exitRawPort = exitRawPort;
    }

    public void setServiceEndpoint(String serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

}
