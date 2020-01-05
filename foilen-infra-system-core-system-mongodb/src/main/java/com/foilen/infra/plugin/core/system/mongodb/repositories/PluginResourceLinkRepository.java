/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.PluginResourceLink;

public interface PluginResourceLinkRepository extends MongoRepository<PluginResourceLink, String> {

    long deleteAllByFromResourceIdAndLinkTypeAndToResourceId(String fromResourceId, String linkType, String toResourceId);

    void deleteAllByFromResourceIdOrToResourceId(String fromResourceId, String toResourceId);

    boolean existsByFromResourceIdAndLinkTypeAndToResourceId(String fromResourceId, String linkType, String toResourceId);

    List<PluginResourceLink> findAllByFromResourceId(String fromResourceId);

    List<PluginResourceLink> findAllByFromResourceIdAndLinkType(String fromResourceId, String linkType);

    List<PluginResourceLink> findAllByFromResourceIdAndLinkTypeAndToResourceTypeIn(String fromResourceId, String linkType, List<String> toResourceTypes);

    List<PluginResourceLink> findAllByFromResourceIdOrToResourceId(String fromResourceId, String toResourceId);

    List<PluginResourceLink> findAllByFromResourceTypeInAndLinkTypeAndToResourceId(List<String> fromResourceTypes, String linkType, String toResourceId);

    List<PluginResourceLink> findAllByLinkTypeAndToResourceId(String linkType, String toResourceId);

    List<PluginResourceLink> findAllByToResourceId(String toResourceId);

}
