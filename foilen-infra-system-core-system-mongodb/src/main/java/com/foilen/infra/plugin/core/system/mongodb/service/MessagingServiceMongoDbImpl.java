/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

import com.foilen.infra.plugin.core.system.mongodb.repositories.MessageRepository;
import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.Message;
import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.models.MessageLevel;
import com.foilen.infra.plugin.v1.core.service.MessagingService;
import com.foilen.smalltools.tools.AbstractBasics;

public class MessagingServiceMongoDbImpl extends AbstractBasics implements MessagingService {

    private static final String SENDER = "SYSTEM";

    @Autowired
    private MessageRepository messageRepository;

    @Override
    public void alertingError(String shortDescription, String longDescription) {
        messageRepository.save(new Message(MessageLevel.ERROR, new Date(), SENDER, shortDescription, longDescription));
    }

    @Override
    public void alertingInfo(String shortDescription, String longDescription) {
        messageRepository.save(new Message(MessageLevel.INFO, new Date(), SENDER, shortDescription, longDescription));
    }

    @Override
    public void alertingWarn(String shortDescription, String longDescription) {
        messageRepository.save(new Message(MessageLevel.WARN, new Date(), SENDER, shortDescription, longDescription));
    }

}
