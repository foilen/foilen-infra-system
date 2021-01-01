/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.app.test.docker.model;

import com.foilen.infra.plugin.v1.model.resource.AbstractIPResource;
import com.foilen.infra.plugin.v1.model.resource.InfraPluginResourceCategory;
import com.google.common.collect.ComparisonChain;

/**
 * This is a unix user that is installed on some Machine.<br>
 * Links to:
 * <ul>
 * <li>Machine: (optional / many) INSTALLED_ON - The machines where to install that unix user</li>
 * </ul>
 *
 * Manages:
 * <ul>
 * <li>None</li>
 * </ul>
 */
public class UnixUser extends AbstractIPResource implements Comparable<UnixUser> {

    public static final String RESOURCE_TYPE = "Unix User";

    public static final String PROPERTY_ID = "id";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_HOME_FOLDER = "homeFolder";
    public static final String PROPERTY_SHELL = "shell";
    public static final String PROPERTY_HASHED_PASSWORD = "hashedPassword";
    public static final String PROPERTY_PASSWORD = "password";
    public static final String PROPERTY_KEEP_CLEAR_PASSWORD = "keepClearPassword";

    private Long id;
    private String name;
    private String homeFolder;
    private String shell = "/bin/bash";
    private boolean keepClearPassword;
    private String password;
    private String hashedPassword;

    public UnixUser() {
    }

    /**
     * Primary key.
     *
     * @param id
     *            the id
     */
    public UnixUser(Long id) {
        this.id = id;
    }

    public UnixUser(Long id, String name, String homeFolder, String shell) {
        this.id = id;
        this.name = name;
        this.homeFolder = homeFolder;
        this.shell = shell;
    }

    public UnixUser(Long id, String name, String homeFolder, String shell, String hashedPassword) {
        this.id = id;
        this.name = name;
        this.homeFolder = homeFolder;
        this.shell = shell;
        this.hashedPassword = hashedPassword;
    }

    @Override
    public int compareTo(UnixUser o) {
        return ComparisonChain.start() //
                .compare(this.name, o.name) //
                .result();
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public String getHomeFolder() {
        return homeFolder;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public InfraPluginResourceCategory getResourceCategory() {
        return InfraPluginResourceCategory.INFRASTRUCTURE;
    }

    @Override
    public String getResourceDescription() {
        return homeFolder;
    }

    @Override
    public String getResourceName() {
        return name;
    }

    public String getShell() {
        return shell;
    }

    public boolean isKeepClearPassword() {
        return keepClearPassword;
    }

    public UnixUser setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
        return this;
    }

    public UnixUser setHomeFolder(String homeFolder) {
        this.homeFolder = homeFolder;
        return this;
    }

    public UnixUser setId(Long id) {
        this.id = id;
        return this;
    }

    public UnixUser setKeepClearPassword(boolean keepClearPassword) {
        this.keepClearPassword = keepClearPassword;
        return this;
    }

    public UnixUser setName(String name) {
        this.name = name;
        return this;
    }

    public UnixUser setPassword(String password) {
        this.password = password;
        return this;
    }

    public UnixUser setShell(String shell) {
        this.shell = shell;
        return this;
    }

}
