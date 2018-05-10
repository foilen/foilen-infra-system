/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UnixUserDetail {

    private Integer id;
    private String name;
    private String homeFolder;
    private String shell;
    private String hashedPassword;
    private Set<String> sudos = new HashSet<>();

    public UnixUserDetail() {
    }

    public UnixUserDetail(Integer id, String name, String homeFolder, String shell) {
        this.id = id;
        this.name = name;
        this.homeFolder = homeFolder;
        this.shell = shell;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public String getHomeFolder() {
        return homeFolder;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getShell() {
        return shell;
    }

    public Set<String> getSudos() {
        return sudos;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public void setHomeFolder(String homeFolder) {
        this.homeFolder = homeFolder;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setShell(String shell) {
        this.shell = shell;
    }

    public void setSudos(Set<String> sudos) {
        this.sudos = sudos;
    }

    /**
     * Get the line in /etc/passwd file.
     *
     * @return the line
     */
    public String toPasswd() {
        return name + ":x:" + id + ":" + id + "::" + homeFolder + ":" + shell;
    }

    /**
     * Get the line in /etc/shadow file.
     *
     * @return the line
     */
    public String toShadow() {
        return name + ":" + hashedPassword + ":0:0:99999:7:::";
    }

    /**
     * Complete sudo file.
     *
     * @return all the lines
     */
    public String toSudoFile() {
        StringBuilder builder = new StringBuilder();
        for (String command : sudos) {
            builder.append(name);
            builder.append("  ALL = NOPASSWD: ");
            builder.append(command.replaceAll("\\:", "\\\\:"));
            builder.append("\n");
        }
        return builder.toString();
    }

}
