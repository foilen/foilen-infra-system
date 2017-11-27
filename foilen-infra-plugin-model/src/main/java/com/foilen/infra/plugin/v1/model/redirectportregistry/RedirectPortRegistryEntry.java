/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.redirectportregistry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.ComparisonChain;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedirectPortRegistryEntry implements Comparable<RedirectPortRegistryEntry> {

    private int entryRawPort;
    private String remoteBridgeHost;
    private String remoteServiceName;
    private String remoteServiceEndpoint;

    public RedirectPortRegistryEntry() {
    }

    public RedirectPortRegistryEntry(int entryRawPort, String remoteBridgeHost, String remoteServiceName, String remoteServiceEndpoint) {
        this.entryRawPort = entryRawPort;
        this.remoteBridgeHost = remoteBridgeHost;
        this.remoteServiceName = remoteServiceName;
        this.remoteServiceEndpoint = remoteServiceEndpoint;
    }

    @Override
    public int compareTo(RedirectPortRegistryEntry o) {
        ComparisonChain cc = ComparisonChain.start();
        cc = cc.compare(entryRawPort, o.entryRawPort);
        cc = cc.compare(remoteBridgeHost, o.remoteBridgeHost);
        cc = cc.compare(remoteServiceName, o.remoteServiceName);
        cc = cc.compare(remoteServiceEndpoint, o.remoteServiceEndpoint);
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
        RedirectPortRegistryEntry other = (RedirectPortRegistryEntry) obj;
        if (entryRawPort != other.entryRawPort) {
            return false;
        }
        if (remoteBridgeHost == null) {
            if (other.remoteBridgeHost != null) {
                return false;
            }
        } else if (!remoteBridgeHost.equals(other.remoteBridgeHost)) {
            return false;
        }
        if (remoteServiceEndpoint == null) {
            if (other.remoteServiceEndpoint != null) {
                return false;
            }
        } else if (!remoteServiceEndpoint.equals(other.remoteServiceEndpoint)) {
            return false;
        }
        if (remoteServiceName == null) {
            if (other.remoteServiceName != null) {
                return false;
            }
        } else if (!remoteServiceName.equals(other.remoteServiceName)) {
            return false;
        }
        return true;
    }

    public int getEntryRawPort() {
        return entryRawPort;
    }

    public String getRemoteBridgeHost() {
        return remoteBridgeHost;
    }

    public String getRemoteServiceEndpoint() {
        return remoteServiceEndpoint;
    }

    public String getRemoteServiceName() {
        return remoteServiceName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + entryRawPort;
        result = prime * result + ((remoteBridgeHost == null) ? 0 : remoteBridgeHost.hashCode());
        result = prime * result + ((remoteServiceEndpoint == null) ? 0 : remoteServiceEndpoint.hashCode());
        result = prime * result + ((remoteServiceName == null) ? 0 : remoteServiceName.hashCode());
        return result;
    }

    public void setEntryRawPort(int entryRawPort) {
        this.entryRawPort = entryRawPort;
    }

    public void setRemoteBridgeHost(String remoteBridgeHost) {
        this.remoteBridgeHost = remoteBridgeHost;
    }

    public void setRemoteServiceEndpoint(String remoteServiceEndpoint) {
        this.remoteServiceEndpoint = remoteServiceEndpoint;
    }

    public void setRemoteServiceName(String remoteServiceName) {
        this.remoteServiceName = remoteServiceName;
    }

}
