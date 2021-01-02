/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.upgrader;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tuple.Tuple2;
import com.foilen.smalltools.upgrader.tasks.UpgradeTask;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Component
public class V2020040102_Indexes extends AbstractBasics implements UpgradeTask {

    @Autowired
    private MongoClient mongoClient;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @SafeVarargs
    private void addIndex(String collectionName, Tuple2<String, Object>... keys) {
        MongoDatabase mongoDatabase = mongoClient.getDatabase(databaseName);

        logger.info("Create index for collection {} , with keys {}", collectionName, keys);
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        Document keysDocument = new Document();
        for (Tuple2<String, Object> key : keys) {
            keysDocument.put(key.getA(), key.getB());
        }
        collection.createIndex(keysDocument);
    }

    @Override
    public void execute() {

        addIndex("message", new Tuple2<>("sentOn", 1));
        addIndex("message", new Tuple2<>("acknowledged", 1));
        addIndex("message", new Tuple2<>("acknowledged", 1), new Tuple2<>("sentOn", 1));
        addIndex("message", new Tuple2<>("acknowledged", 1), new Tuple2<>("acknowledgedBatch", 1));
        addIndex("message", new Tuple2<>("acknowledgedBatch", 1));

        addIndex("pluginResource", new Tuple2<>("editorName", 1));
        addIndex("pluginResource", new Tuple2<>("type", 1));
        addIndex("pluginResource", new Tuple2<>("resource.$**", 1));
        addIndex("pluginResource", new Tuple2<>("tags", 1));

        addIndex("pluginResourceLink", new Tuple2<>("fromResourceId", 1));
        addIndex("pluginResourceLink", new Tuple2<>("fromResourceId", 1), new Tuple2<>("linkType", 1), new Tuple2<>("toResourceId", 1));
        addIndex("pluginResourceLink", new Tuple2<>("fromResourceId", 1), new Tuple2<>("toResourceId", 1));
        addIndex("pluginResourceLink", new Tuple2<>("fromResourceId", 1), new Tuple2<>("linkType", 1));
        addIndex("pluginResourceLink", new Tuple2<>("fromResourceId", 1), new Tuple2<>("linkType", 1), new Tuple2<>("toResourceType", 1));
        addIndex("pluginResourceLink", new Tuple2<>("toResourceId", 1));
        addIndex("pluginResourceLink", new Tuple2<>("fromResourceType", 1), new Tuple2<>("linkType", 1), new Tuple2<>("toResourceId", 1));
        addIndex("pluginResourceLink", new Tuple2<>("linkType", 1), new Tuple2<>("toResourceId", 1));

    }

    @Override
    public String useTracker() {
        return MongoDbUpgraderConstants.TRACKER;
    }

}
