/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.upgrader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.upgrader.tasks.UpgradeTask;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

@Component
public class V2020040101_Collections extends AbstractBasics implements UpgradeTask {

    @Autowired
    private MongoClient mongoClient;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    private void addCollection(String collectionName) {
        MongoDatabase mongoDatabase = mongoClient.getDatabase(databaseName);

        logger.info("Create collection {}", collectionName);
        try {
            mongoDatabase.createCollection(collectionName);
        } catch (MongoCommandException e) {
            if (e.getErrorCode() != 48) { // Already exists
                throw e;
            }
        }
    }

    @Override
    public void execute() {
        addCollection("message");
        addCollection("pluginResource");
        addCollection("pluginResourceLink");
    }

    @Override
    public String useTracker() {
        return MongoDbUpgraderConstants.TRACKER;
    }

}
