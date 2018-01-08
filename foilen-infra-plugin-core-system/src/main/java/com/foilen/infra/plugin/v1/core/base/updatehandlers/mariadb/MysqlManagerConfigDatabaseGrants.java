/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.updatehandlers.mariadb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.foilen.smalltools.tools.AbstractBasics;

public class MysqlManagerConfigDatabaseGrants extends AbstractBasics {

    private String databaseName;
    private List<MysqlManagerConfigGrant> grants = new ArrayList<>();

    public MysqlManagerConfigDatabaseGrants() {
    }

    public MysqlManagerConfigDatabaseGrants(String databaseName, MysqlManagerConfigGrant... grants) {
        this.databaseName = databaseName;
        this.grants.addAll(Arrays.asList(grants));
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public List<MysqlManagerConfigGrant> getGrants() {
        return grants;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setGrants(List<MysqlManagerConfigGrant> grants) {
        this.grants = grants;
    }

}
