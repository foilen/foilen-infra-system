/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.repositories;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.Message;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.SecureRandomTools;

@Component
public class MessageCustomRepositoryImpl extends AbstractBasics implements MessageCustomRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<Message> findAllNotAcknowledgedAndAcknowledgedThem() {

        // Acknowledge
        String acknowledgedBatch = SecureRandomTools.randomHexString(10);

        Query query = new Query();
        query.addCriteria(new Criteria("acknowledged").is(false));
        Update update = new Update();
        update.set("acknowledged", true);
        update.set("acknowledgedBatch", acknowledgedBatch);
        mongoTemplate.updateMulti(query, update, Message.class);

        // Retrieve the ones acknowledged this time
        query = new Query();
        query.addCriteria(new Criteria("acknowledgedBatch").is(acknowledgedBatch));
        List<Message> found = mongoTemplate.find(query, Message.class);

        // Remove the batch id
        update = new Update();
        update.unset("acknowledgedBatch");
        mongoTemplate.updateMulti(query, update, Message.class);

        return found;
    }

}
