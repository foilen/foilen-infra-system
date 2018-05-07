/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.app.test.docker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.foilen.infra.plugin.core.system.common.service.IPPluginServiceImpl;
import com.foilen.infra.plugin.core.system.fake.CommonServicesContextBean;
import com.foilen.infra.plugin.core.system.fake.InitSystemBean;
import com.foilen.infra.plugin.core.system.fake.InternalServicesContextBean;
import com.foilen.infra.plugin.core.system.junits.JunitsHelper;
import com.foilen.infra.plugin.core.system.junits.ResourcesDump;
import com.foilen.infra.plugin.system.utils.DockerUtils;
import com.foilen.infra.plugin.system.utils.UnixUsersAndGroupsUtils;
import com.foilen.infra.plugin.system.utils.impl.DockerUtilsImpl;
import com.foilen.infra.plugin.system.utils.impl.UnixUsersAndGroupsUtilsImpl;
import com.foilen.infra.plugin.system.utils.model.ApplicationBuildDetails;
import com.foilen.infra.plugin.system.utils.model.ContainersManageContext;
import com.foilen.infra.plugin.system.utils.model.DockerState;
import com.foilen.infra.plugin.v1.core.base.resources.Application;
import com.foilen.infra.plugin.v1.core.base.resources.UnixUser;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.plugin.v1.model.outputter.docker.DockerContainerOutputContext;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.LogbackTools;
import com.google.common.base.Strings;
import com.google.common.io.Files;

public class StartResourcesApp {

    protected static void importFromDirectory(File inputDirectory, AnnotationConfigApplicationContext applicationContext) {
        // Import all the resources
        Map<String, IPResource> resourcesByFullName = new HashMap<>();
        IPResourceService resourceService = applicationContext.getBean(IPResourceService.class);
        InternalChangeService internalChangeService = applicationContext.getBean(InternalChangeService.class);
        File[] subDirectories = inputDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        ChangesContext changes = new ChangesContext(resourceService);
        System.out.println("---[ Importing Resources ]---");
        for (File subDirectory : subDirectories) {
            String resourceType = subDirectory.getName();
            System.out.println("Type: [" + resourceType + "]");
            Class<? extends IPResource> resourceClass = resourceService.getResourceDefinition(resourceType).getResourceClass();

            for (File resourceFile : subDirectory.listFiles()) {
                String resourceName = resourceFile.getName();
                resourceName = resourceName.substring(0, resourceName.lastIndexOf('.'));

                System.out.println("\t" + resourceName);
                IPResource resource = JsonTools.readFromFile(resourceFile, resourceClass);
                resourcesByFullName.put(resourceType + "/" + resourceName, resource);

                changes.resourceAdd(resource);
            }
        }

        // Import all the tags
        System.out.println("---[ Importing Tags ]---");
        String tagsFilePath = inputDirectory + "/tags.txt";
        File tagsFile = new File(tagsFilePath);
        if (tagsFile.exists() && tagsFile.isFile()) {
            for (String line : FileTools.readFileLinesIteration(tagsFilePath)) {
                System.out.println("\t" + line);
                String[] lineParts = line.split(";");
                if (lineParts.length != 2) {
                    System.out.println("\tThe tag line [" + line + "] is invalid");
                    System.exit(1);
                }

                String resourceName = lineParts[0];
                String tagName = lineParts[1];

                IPResource resource = resourcesByFullName.get(resourceName);
                if (resource == null) {
                    System.out.println("\t\tThe resource [" + resourceName + "] is invalid");
                    System.exit(1);
                }
                if (Strings.isNullOrEmpty(tagName)) {
                    System.out.println("\t\tThe tag name cannot be empty");
                    System.exit(1);
                }

                changes.tagAdd(resource, tagName);

            }
        }

        // Import all the links
        System.out.println("---[ Importing Links ]---");
        String linksFilePath = inputDirectory + "/links.txt";
        File linksFile = new File(linksFilePath);
        if (linksFile.exists() && linksFile.isFile()) {
            for (String line : FileTools.readFileLinesIteration(linksFilePath)) {
                System.out.println("\t" + line);
                String[] lineParts = line.split(";");
                if (lineParts.length != 3) {
                    System.out.println("\tThe link line [" + line + "] is invalid");
                    System.exit(1);
                }

                String fromResourceName = lineParts[0];
                String linkType = lineParts[1];
                String toResourceName = lineParts[2];

                IPResource fromResource = resourcesByFullName.get(fromResourceName);
                if (fromResource == null) {
                    System.out.println("\t\tThe resource [" + fromResourceName + "] is invalid");
                    System.exit(1);
                }
                IPResource toResource = resourcesByFullName.get(toResourceName);
                if (toResource == null) {
                    System.out.println("\t\tThe resource [" + toResourceName + "] is invalid");
                    System.exit(1);
                }

                changes.linkAdd(fromResource, linkType, toResource);

            }
        }

        // Execute the changes
        System.out.println("\n---[ Execute the changes ]---");
        internalChangeService.changesExecute(changes);
        changes.clear();
    }

    public static void main(String[] args) {

        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        // Check if debug mode
        boolean isDebug = false;
        boolean isInfo = false;
        if (arguments.remove("--debug")) {
            isDebug = true;
        }
        if (arguments.remove("--info")) {
            isInfo = true;
        }

        if (arguments.size() != 1) {
            System.out.println("You need to provide a directory or file where to read all the resources from");
            System.exit(1);
        }

        // Check if directory or file mode
        boolean isFile = false;
        String inputName = arguments.get(0);
        File inputFileOrDirectory = new File(inputName);
        if (inputFileOrDirectory.exists()) {
            isFile = !inputFileOrDirectory.isDirectory();
            if (isFile) {
                System.out.println("Reading a file");
            } else {
                System.out.println("Reading a directory");
            }
        } else {
            System.out.println("The file or directory " + inputName + " does not exist");
            System.exit(1);
        }

        // Start the plugin service
        if (isDebug) {
            LogbackTools.changeConfig("/logback-debug.xml");
        } else if (isInfo) {
            LogbackTools.changeConfig("/logback-info.xml");
        } else {
            LogbackTools.changeConfig("/logback-quiet.xml");
        }
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.register(CreateSampleResourcesApp.class);
        applicationContext.register(CommonServicesContextBean.class);
        applicationContext.register(InitSystemBean.class);
        applicationContext.register(InternalServicesContextBean.class);
        applicationContext.register(IPPluginServiceImpl.class);
        applicationContext.scan("com.foilen.infra.plugin.core.system.fake.service");
        applicationContext.refresh();

        // Import from directory or file
        IPResourceService resourceService = applicationContext.getBean(IPResourceService.class);
        if (isFile) {
            CommonServicesContext commonServicesContext = applicationContext.getBean(CommonServicesContext.class);
            InternalServicesContext internalServicesContext = applicationContext.getBean(InternalServicesContext.class);
            ResourcesDump resourcesDump = JsonTools.readFromFile(inputFileOrDirectory, ResourcesDump.class);
            JunitsHelper.dumpImport(commonServicesContext, internalServicesContext, resourcesDump);
        } else {
            importFromDirectory(inputFileOrDirectory, applicationContext);
        }

        // Install unix users
        System.out.println("\n---[ Install unix users ]---");
        UnixUsersAndGroupsUtils unixUsersAndGroupsUtils = new UnixUsersAndGroupsUtilsImpl();
        resourceService.resourceFindAll(resourceService.createResourceQuery(UnixUser.class)).forEach(unixUser -> {
            System.out.println("\t" + unixUser.getName() + " (" + unixUser.getId() + ")");
            unixUsersAndGroupsUtils.userCreate(unixUser.getName(), unixUser.getId(), unixUser.getHomeFolder(), null, null);
        });

        // Install applications (docker)
        DockerUtils dockerUtils = new DockerUtilsImpl();
        File tmpDirectory = Files.createTempDir();
        System.out.println("\n---[ Install application (docker) ]---");
        List<Application> applications = resourceService.resourceFindAll(resourceService.createResourceQuery(Application.class));
        DockerState dockerState = stateGetOrCreate();
        List<ApplicationBuildDetails> alwaysRunningApplications = applications.stream() //
                .map(application -> {
                    String applicationName = application.getName();
                    String buildDirectory = tmpDirectory.getAbsolutePath() + "/" + applicationName + "/";
                    return new ApplicationBuildDetails().setApplicationDefinition(application.getApplicationDefinition()) //
                            .setOutputContext(new DockerContainerOutputContext(applicationName, applicationName, applicationName, buildDirectory));
                }).collect(Collectors.toList());
        dockerUtils.containersManage(new ContainersManageContext() //
                .setDockerState(dockerState) //
                .setAlwaysRunningApplications(alwaysRunningApplications));
        stateSave(dockerState);
        List<String> applicationStatuses = new ArrayList<>();
        dockerState.getFailedContainersByName().keySet().forEach(it -> {
            applicationStatuses.add(it + " [FAILED]");
        });
        dockerState.getRunningContainersByName().keySet().forEach(it -> {
            String ip = dockerState.getIpByName().get(it);
            applicationStatuses.add(it + " [OK:" + ip + "]");
        });
        applicationStatuses.stream().sorted().forEach(it -> {
            System.out.println("\t" + it);
        });

        // Wait, stop and clean
        System.out.println("\nPress enter to stop and remove all the Docker containers or press ctrl-c to quit");
        BufferedReader stdInReader = new BufferedReader(new InputStreamReader(System.in));
        try {
            stdInReader.readLine();
        } catch (IOException e) {
        }

        System.out.println("\n---[ Stop and remove all application (docker) ]---");
        dockerState.getRunningContainersByName().keySet().forEach(applicationName -> {
            System.out.println("\t" + applicationName);
            dockerUtils.containerStopAndRemove(applicationName);
        });

        // End
        applicationContext.close();

    }

    private static DockerState stateGetOrCreate() {
        try {
            return JsonTools.readFromFile("_dockerState.json", DockerState.class);
        } catch (Exception e) {
        }
        return new DockerState();
    }

    private static void stateSave(DockerState dockerState) {
        JsonTools.writeToFile("_dockerState.json", dockerState);
    }

}
