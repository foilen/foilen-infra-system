/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.resource.domain.Domain;
import com.foilen.infra.resource.example.EmployeeResource;
import com.foilen.infra.resource.example.Ex1Resource;
import com.foilen.infra.resource.machine.Machine;
import com.foilen.infra.resource.unixuser.UnixUser;
import com.foilen.smalltools.tools.DateTools;

@SpringBootApplication
public class LaunchWebApp {

    public static void main(String[] args) {
        main(args, LaunchWebApp.class);
    }

    public static void main(String[] args, Class<? extends LaunchWebApp> launchWebAppClass) {

        ConfigurableApplicationContext ctx = SpringApplication.run(launchWebAppClass, args);

        LaunchWebApp launchWebApp = ctx.getBean(launchWebAppClass);
        IPResourceService resourceService = ctx.getBean(IPResourceService.class);
        InternalChangeService internalChangeService = ctx.getBean(InternalChangeService.class);
        launchWebApp.createFakeData(resourceService, internalChangeService);

    }

    protected void createFakeData(IPResourceService resourceService, InternalChangeService internalChangeService) {
        // Fake data
        ChangesContext changes = new ChangesContext(resourceService);
        changes.resourceAdd(new Domain("example.com", "com.example"));
        changes.resourceAdd(new Domain("test1.example.com", "com.example.test1"));
        changes.resourceAdd(new Domain("test2.example.com", "com.example.test2"));
        changes.resourceAdd(new Domain("test3.example.com", "com.example.test3"));
        changes.resourceAdd(new Domain("node.example.com", "com.example.node"));
        changes.resourceAdd(new Machine("test1.node.example.com", "192.168.0.11"));
        changes.resourceAdd(new Machine("test2.node.example.com", null));
        changes.resourceAdd(new Ex1Resource("Something", 2001));
        changes.resourceAdd(new Ex1Resource("Another thing", 2005));
        changes.resourceAdd(new Ex1Resource("Why not", 1986));
        changes.resourceAdd(new EmployeeResource("Éric", "Lépine", DateTools.parseDateOnly("1980-03-17")));
        changes.resourceAdd(new EmployeeResource("Samantha", "Smith", DateTools.parseDateOnly("1991-06-14")));
        changes.resourceAdd(new UnixUser(null, "user1", "/home/user1", null, null));
        internalChangeService.changesExecute(changes);
    }

}
