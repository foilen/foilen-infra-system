/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.upgrader;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.foilen.infra.plugin.core.system.mongodb.repositories.PluginResourceRepository;
import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.PluginResource;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.BufferBatchesTools;
import com.foilen.smalltools.tools.CollectionsTools;
import com.foilen.smalltools.upgrader.tasks.UpgradeTask;

@Component
public class V2021020201_PluginResources_AddNewFields extends AbstractBasics implements UpgradeTask {

    @Autowired
    private PluginResourceRepository pluginResourceRepository;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Override
    public void execute() {

        List<PluginResource> pluginResources = pluginResourceRepository.findAll();
        logger.info("Got {} resources to update", pluginResources.size());

        BufferBatchesTools.<PluginResource> autoClose(100, items -> {
            logger.info("Saving {} in batch", items.size());
            pluginResourceRepository.saveAll(items);
        }, bufferBatchesTools -> {

            pluginResources.forEach(pluginResource -> {
                boolean alreadyCorrect = CollectionsTools.isAllItemNotNull(pluginResource.getResourceDescription(), pluginResource.getResourceName());
                if (alreadyCorrect) {
                    logger.info("{} ({}) already correct. Skip", pluginResource.getId(), pluginResource.getResourceName());
                    return;
                }

                IPResource resource = pluginResource.getResource();
                pluginResource.setResourceDescription(resource.getResourceDescription());
                pluginResource.setResourceName(resource.getResourceName());
                logger.info("{} ({}) needs update", pluginResource.getId(), pluginResource.getResourceName());
                bufferBatchesTools.add(pluginResource);

            });

        });

    }

    @Override
    public String useTracker() {
        return MongoDbUpgraderConstants.TRACKER;
    }
}
