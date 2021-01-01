/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.repositories;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.data.mongodb.core.query.Query;

import com.foilen.infra.plugin.v1.core.resource.IPResourceQuery;
import com.foilen.infra.plugin.v1.model.resource.IPResource;

public interface PluginResourceCustomRepository {

    void addTagById(String resourceId, String tagName);

    <T extends IPResource> List<T> findAll(IPResourceQuery<T> query);

    <T extends IPResource> List<T> findAll(IPResourceQuery<T> query, Consumer<Query> queryHook);

    boolean removeTagById(String resourceId, String tagName);

}
