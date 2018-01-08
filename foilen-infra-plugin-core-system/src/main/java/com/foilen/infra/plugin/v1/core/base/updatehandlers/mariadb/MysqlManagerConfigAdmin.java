/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.updatehandlers.mariadb;

import com.foilen.smalltools.tools.AbstractBasics;

public class MysqlManagerConfigAdmin extends AbstractBasics {

    private String name;
    private String password;

    public MysqlManagerConfigAdmin() {
    }

    public MysqlManagerConfigAdmin(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
