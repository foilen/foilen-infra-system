/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.PluginResource;

public interface PluginResourceRepository extends MongoRepository<PluginResource, String>, PluginResourceCustomRepository {

    long deleteOneById(String id);

}
