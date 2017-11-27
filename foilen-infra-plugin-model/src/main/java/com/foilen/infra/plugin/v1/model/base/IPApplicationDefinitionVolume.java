/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.foilen.smalltools.tools.AbstractBasics;

@JsonPropertyOrder(alphabetic = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IPApplicationDefinitionVolume extends AbstractBasics {

    private String hostFolder;
    private String containerFsFolder;

    private Integer ownerId;
    private Integer groupId;

    private String permissions;

    public IPApplicationDefinitionVolume() {
    }

    public IPApplicationDefinitionVolume(String hostFolder, String containerFsFolder, Integer ownerId, Integer groupId, String permissions) {
        this.hostFolder = hostFolder;
        this.containerFsFolder = containerFsFolder;
        this.ownerId = ownerId;
        this.groupId = groupId;
        this.permissions = permissions;
    }

    public String getContainerFsFolder() {
        return containerFsFolder;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public String getHostFolder() {
        return hostFolder;
    }

    public Integer getOwnerId() {
        return ownerId;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setContainerFsFolder(String containerFsFolder) {
        this.containerFsFolder = containerFsFolder;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public void setHostFolder(String hostFolder) {
        this.hostFolder = hostFolder;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

}
