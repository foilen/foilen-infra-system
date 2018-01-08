/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.updatehandlers.mariadb;

import java.util.ArrayList;
import java.util.List;

import com.foilen.smalltools.tools.AbstractBasics;

public class MysqlManagerConfigPermission extends AbstractBasics {

    private String name;
    private String host;
    private String password;
    private List<MysqlManagerConfigDatabaseGrants> databaseGrants = new ArrayList<>();

    public MysqlManagerConfigPermission() {
    }

    public MysqlManagerConfigPermission(String name, String host, String password) {
        this.name = name;
        this.host = host;
        this.password = password;
    }

    public List<MysqlManagerConfigDatabaseGrants> getDatabaseGrants() {
        return databaseGrants;
    }

    public String getHost() {
        return host;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public void setDatabaseGrants(List<MysqlManagerConfigDatabaseGrants> databaseGrants) {
        this.databaseGrants = databaseGrants;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
