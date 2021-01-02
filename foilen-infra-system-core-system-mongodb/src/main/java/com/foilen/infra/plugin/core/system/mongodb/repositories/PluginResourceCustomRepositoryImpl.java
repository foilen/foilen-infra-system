/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.repositories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.PluginResource;
import com.foilen.infra.plugin.v1.core.exception.ProblemException;
import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.core.resource.IPResourceQuery;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.CollectionsTools;
import com.mongodb.client.result.UpdateResult;

@Component
public class PluginResourceCustomRepositoryImpl extends AbstractBasics implements PluginResourceCustomRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void addTagById(String resourceId, String tagName) {
        Query query = new Query();
        query.addCriteria(new Criteria("id").is(resourceId));
        Update update = new Update();
        update.addToSet("tags", tagName);
        mongoTemplate.updateMulti(query, update, PluginResource.class);
    }

    @Override
    public <T extends IPResource> List<T> findAll(IPResourceQuery<T> query) {
        return findAll(query, q -> {
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IPResource> List<T> findAll(IPResourceQuery<T> query, Consumer<Query> queryHook) {
        Query mongoQuery = new Query();

        List<IPResourceDefinition> resourceDefinitions = query.getResourceDefinitions();

        // Right type
        List<String> resourceTypes = resourceDefinitions.stream().map(IPResourceDefinition::getResourceType).collect(Collectors.toList());
        mongoQuery.addCriteria(new Criteria("type").in(resourceTypes));

        // Right ids
        if (query.getIdsIn() != null) {
            mongoQuery.addCriteria(new Criteria("id").in(query.getIdsIn()));
        }

        // Right editor
        if (query.getEditorsIn() != null) {
            mongoQuery.addCriteria(new Criteria("editorName").in(query.getEditorsIn()));
        }

        // Equals
        for (Entry<String, Object> entry : query.getPropertyEquals().entrySet()) {
            String propertyName = entry.getKey();
            String dbPropertyName = getDbPropertyName(propertyName);
            Object propertyValue = entry.getValue();

            // Get the first property type with that name
            IPResourceDefinition mainResourceDefinition = resourceDefinitions.stream() //
                    .filter(it -> it.getPropertyType(propertyName) != null) //
                    .findFirst().get();
            Class<?> propertyType = mainResourceDefinition.getPropertyType(propertyName);

            if (Set.class.isAssignableFrom(propertyType)) {
                // All the values
                if (((Collection<?>) propertyValue).isEmpty()) {
                    mongoQuery.addCriteria(new Criteria().orOperator( //
                            new Criteria("resource." + dbPropertyName).all(propertyValue), //
                            new Criteria("resource." + dbPropertyName).exists(false)));
                } else {
                    mongoQuery.addCriteria(new Criteria("resource." + dbPropertyName).all(propertyValue));
                }
            } else {
                // The value must be equal
                mongoQuery.addCriteria(new Criteria("resource." + dbPropertyName).is(propertyValue));
            }
        }

        // Contains
        for (Entry<String, Object> entry : query.getPropertyContains().entrySet()) {
            String propertyName = entry.getKey();
            String dbPropertyName = getDbPropertyName(propertyName);
            Object propertyValue = entry.getValue();

            // Get the first property type with that name
            IPResourceDefinition mainResourceDefinition = resourceDefinitions.stream() //
                    .filter(it -> it.getPropertyType(propertyName) != null) //
                    .findFirst().get();
            Class<?> propertyType = mainResourceDefinition.getPropertyType(propertyName);

            if (Set.class.isAssignableFrom(propertyType)) {
                // All the values
                List<?> values = ((Collection<?>) propertyValue).stream().collect(Collectors.toList());
                Criteria[] criterias = new Criteria[values.size()];
                for (int i = 0; i < values.size(); ++i) {
                    criterias[i] = new Criteria("resource." + dbPropertyName).is(values.get(0));
                }

                mongoQuery.addCriteria(new Criteria().andOperator(criterias));
            } else {
                return new ArrayList<>();
            }
        }

        // Like
        for (Entry<String, String> entry : query.getPropertyLike().entrySet()) {
            String propertyName = entry.getKey();
            String dbPropertyName = getDbPropertyName(propertyName);
            String propertyValue = entry.getValue();

            // Get the first property type with that name
            IPResourceDefinition mainResourceDefinition = resourceDefinitions.stream() //
                    .filter(it -> it.getPropertyType(propertyName) != null) //
                    .findFirst().get();
            Class<?> propertyType = mainResourceDefinition.getPropertyType(propertyName);
            if (Collection.class.isAssignableFrom(propertyType) || String.class.isAssignableFrom(propertyType)) {
                mongoQuery.addCriteria(new Criteria("resource." + dbPropertyName).regex("^" + propertyValue.replaceAll("\\%", ".*") + "$"));
            } else {
                throw new ProblemException("Property [" + propertyName + "] does not support querying like");
            }

        }

        // Share an "and" criteria for quantities
        List<Criteria> qtyCriterias = new ArrayList<>();

        // Lesser
        for (Entry<String, Object> entry : query.getPropertyLesser().entrySet()) {
            String propertyName = entry.getKey();
            String dbPropertyName = getDbPropertyName(propertyName);
            Object propertyValue = entry.getValue();

            qtyCriterias.add(new Criteria("resource." + dbPropertyName).lt(propertyValue));

        }

        // Lesser and equal
        for (Entry<String, Object> entry : query.getPropertyLesserAndEquals().entrySet()) {
            String propertyName = entry.getKey();
            String dbPropertyName = getDbPropertyName(propertyName);
            Object propertyValue = entry.getValue();

            qtyCriterias.add(new Criteria("resource." + dbPropertyName).lte(propertyValue));
        }

        // Greater
        for (Entry<String, Object> entry : query.getPropertyGreater().entrySet()) {
            String propertyName = entry.getKey();
            String dbPropertyName = getDbPropertyName(propertyName);
            Object propertyValue = entry.getValue();

            qtyCriterias.add(new Criteria("resource." + dbPropertyName).gt(propertyValue));
        }

        // Greater and equal
        for (Entry<String, Object> entry : query.getPropertyGreaterEquals().entrySet()) {
            String propertyName = entry.getKey();
            String dbPropertyName = getDbPropertyName(propertyName);
            Object propertyValue = entry.getValue();

            qtyCriterias.add(new Criteria("resource." + dbPropertyName).gte(propertyValue));

        }

        if (!qtyCriterias.isEmpty()) {
            mongoQuery.addCriteria(new Criteria().andOperator(qtyCriterias.toArray(new Criteria[qtyCriterias.size()])));
        }

        // Tags and
        if (!CollectionsTools.isNullOrEmpty(query.getTagsAnd())) {
            List<String> tagsList = query.getTagsAnd().stream().collect(Collectors.toList());

            Criteria[] criterias = new Criteria[tagsList.size()];
            for (int i = 0; i < tagsList.size(); ++i) {
                criterias[i] = new Criteria("tags").is(tagsList.get(0));
            }

            mongoQuery.addCriteria(new Criteria().andOperator(criterias));
        }

        // Tags or
        if (!CollectionsTools.isNullOrEmpty(query.getTagsOr())) {
            mongoQuery.addCriteria(new Criteria("tags").in(query.getTagsOr()));
        }

        logger.debug("MongoDB Query (before hook): {}", mongoQuery);

        // Call the hook
        queryHook.accept(mongoQuery);

        logger.debug("MongoDB Query (after hook): {}", mongoQuery);

        List<PluginResource> found = mongoTemplate.find(mongoQuery, PluginResource.class);
        return found.stream() //
                .map(it -> {
                    T resource = (T) it.getResource();
                    resource.setInternalId(it.getId());
                    return resource;
                }) //
                .collect(Collectors.toList());
    }

    private String getDbPropertyName(String propertyName) {
        // FIX bug https://jira.spring.io/browse/DATAMONGO-2496
        if ("id".equals(propertyName)) {
            return "_id";
        }
        return propertyName;
    }

    @Override
    public boolean removeTagById(String resourceId, String tagName) {
        Query query = new Query();
        query.addCriteria(new Criteria("id").is(resourceId));
        Update update = new Update();
        update.pull("tags", tagName);
        UpdateResult result = mongoTemplate.updateMulti(query, update, PluginResource.class);
        return result.getModifiedCount() > 0;
    }

}
