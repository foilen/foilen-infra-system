/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.app.test.docker.webapp.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.foilen.infra.plugin.core.system.junits.JunitsHelper;
import com.foilen.infra.plugin.core.system.junits.ResourcesDump;
import com.foilen.infra.plugin.core.system.memory.service.ResourceServicesInMemoryImpl;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.DirectoryTools;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.JsonTools;
import com.google.common.base.Joiner;

public class ResourcesControllerExtra extends AbstractBasics {

    @Autowired
    private CommonServicesContext commonServicesContext;
    @Autowired
    private InternalServicesContext internalServicesContext;
    @Autowired
    private ResourceServicesInMemoryImpl resourceService;

    @ResponseBody
    @GetMapping("exportFile")
    public ResourcesDump exportFile() {
        return JunitsHelper.dumpExport(commonServicesContext, internalServicesContext);
    }

    @GetMapping("exportFolder")
    public String exportFolder(@RequestParam String folder, HttpServletRequest httpServletRequest) {
        File exportFolder = new File(folder);
        String path = exportFolder.getAbsolutePath();
        if (exportFolder.exists()) {
            logger.error("The folder {} already exist", exportFolder.getAbsolutePath());
            return "redirect:list";
        }

        logger.info("Exporting in the folder {}", exportFolder.getAbsolutePath());
        if (!DirectoryTools.createPath(exportFolder)) {
            logger.error("Could not create the folder {}", exportFolder.getAbsolutePath());
            return "redirect:list";
        }

        // Export
        List<String> links = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        for (IPResource resource : resourceService.resourceFindAll()) {
            // Export resources
            String resourceType = resourceService.getResourceDefinition(resource).getResourceType();
            String resourceFilename = path + "/" + resourceType + "/" + resource.getResourceName().replaceAll("/", "_") + ".json";
            DirectoryTools.createPathToFile(resourceFilename);
            JsonTools.writeToFile(resourceFilename, resource);

            // Export tags
            resourceService.tagFindAllByResource(resource).stream() //
                    .map(it -> {
                        return resourceType + "/" + resource.getResourceName() + //
                        ";" + it;
                    }) //
                    .forEach(it -> tags.add(it));

            // Export links
            resourceService.linkFindAllByFromResource(resource).stream() //
                    .map(it -> {
                        String toResourceType = resourceService.getResourceDefinition(it.getB()).getResourceType();
                        return resourceType + "/" + resource.getResourceName() + //
                        ";" + it.getA() + ";" //
                                + toResourceType + "/" + it.getB().getResourceName();
                    }) //
                    .forEach(it -> links.add(it));

        }

        // Save tags and links
        Collections.sort(links);
        Collections.sort(tags);
        FileTools.writeFile(Joiner.on('\n').join(tags), path + "/tags.txt");
        FileTools.writeFile(Joiner.on('\n').join(links), path + "/links.txt");

        return "redirect:list";
    }

}
