/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.foilen.infra.plugin.v1.core.base.resources.Domain;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.plugin.v1.example.resource.EmployeeResource;
import com.foilen.infra.plugin.v1.example.resource.Ex1Resource;
import com.foilen.smalltools.tools.DateTools;

@SpringBootApplication
public class LaunchWebApp {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(LaunchWebApp.class, args);

        // Fake data
        InternalChangeService internalChangeService = ctx.getBean(InternalChangeService.class);
        ChangesContext changes = new ChangesContext();
        changes.getResourcesToAdd().add(new Domain("example.com", "com.example"));
        changes.getResourcesToAdd().add(new Domain("test1.example.com", "com.example.test1"));
        changes.getResourcesToAdd().add(new Domain("test2.example.com", "com.example.test2"));
        changes.getResourcesToAdd().add(new Domain("test3.example.com", "com.example.test3"));
        changes.getResourcesToAdd().add(new Domain("node.example.com", "com.example.node"));
        changes.getResourcesToAdd().add(new Machine("test1.node.example.com", "192.168.0.11"));
        changes.getResourcesToAdd().add(new Machine("test2.node.example.com", null));
        changes.getResourcesToAdd().add(new Ex1Resource("Something", 2001));
        changes.getResourcesToAdd().add(new Ex1Resource("Another thing", 2005));
        changes.getResourcesToAdd().add(new Ex1Resource("Why not", 1986));
        changes.getResourcesToAdd().add(new EmployeeResource("Éric", "Lépine", DateTools.parseDateOnly("1980-03-17")));
        changes.getResourcesToAdd().add(new EmployeeResource("Samantha", "Smith", DateTools.parseDateOnly("1991-06-14")));
        internalChangeService.changesExecute(changes);
    }

}
