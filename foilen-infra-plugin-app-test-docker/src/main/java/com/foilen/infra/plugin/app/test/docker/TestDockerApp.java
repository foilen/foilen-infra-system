/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.app.test.docker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestDockerApp {

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("You need to provide the component to use");
            System.out.println("\tcreate-sample: to create sample files of each resource type");
            System.out.println("\tstart-resources: to import the resource files and start the applications in Docker");
            System.exit(1);
        }

        String component = args[0];
        List<String> newArgs = new ArrayList<>();
        newArgs.addAll(Arrays.asList(args));
        newArgs.remove(0);
        switch (component) {
        case "create-sample":
            CreateSampleResourcesApp.main(newArgs.toArray(new String[newArgs.size()]));
            break;
        case "start-resources":
            StartResourcesApp.main(newArgs.toArray(new String[newArgs.size()]));
            break;
        default:
            System.out.println("Invalid component: " + component);
            System.exit(1);
        }

    }

}