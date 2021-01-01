/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.repositories;

import java.util.List;

import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.Message;

public interface MessageCustomRepository {

    List<Message> findAllNotAcknowledgedAndAcknowledgedThem();

}
