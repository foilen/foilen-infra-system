/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.repositories;

import java.util.Date;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.Message;

public interface MessageRepository extends MongoRepository<Message, String>, MessageCustomRepository {

    long countBySentOnBeforeAndAcknowledgedIsFalse(Date sentOnBefore);

    long deleteBySentOnBeforeAndAcknowledgedIsTrue(Date sentOnBefore);

}
