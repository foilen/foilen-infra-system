/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.transaction.annotation.Transactional;

import com.foilen.infra.plugin.core.system.common.changeexecution.ChangeExecutionLogic;
import com.foilen.infra.plugin.core.system.mongodb.repositories.PluginResourceLinkRepository;
import com.foilen.infra.plugin.core.system.mongodb.repositories.PluginResourceRepository;
import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.PluginResource;
import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.PluginResourceLink;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.changes.ChangeExecutionHook;
import com.foilen.infra.plugin.v1.core.exception.ResourceNotFoundException;
import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.core.resource.IPResourceQuery;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalIPResourceService;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.exception.SmallToolsException;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.AssertTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.foilen.smalltools.tuple.Tuple3;

@Transactional
public class ResourceServicesInMongoDbImpl extends AbstractBasics implements IPResourceService, InternalIPResourceService, InternalChangeService {

    @Autowired
    private CommonServicesContext commonServicesContext;
    @Autowired
    private InternalServicesContext internalServicesContext;
    @Autowired
    private PluginResourceRepository pluginResourceRepository;
    @Autowired
    private PluginResourceLinkRepository pluginResourceLinkRepository;
    @Autowired
    private ResourceDefinitionService resourceDefinitionService;

    private long infiniteLoopTimeoutInMs = 15000;

    private List<ChangeExecutionHook> defaultChangeExecutionHooks = new ArrayList<>();

    @Override
    public void changesExecute(ChangesContext changes) {
        ChangeExecutionLogic changeExecutionLogic = new ChangeExecutionLogic(commonServicesContext, internalServicesContext);
        changeExecutionLogic.setInfiniteLoopTimeoutInMs(infiniteLoopTimeoutInMs);
        defaultChangeExecutionHooks.forEach(hook -> changeExecutionLogic.addHook(hook));
        changeExecutionLogic.execute(changes);
    }

    @Override
    public void changesExecute(ChangesContext changes, List<ChangeExecutionHook> extraChangeExecutionHooks) {
        ChangeExecutionLogic changeExecutionLogic = new ChangeExecutionLogic(commonServicesContext, internalServicesContext);
        changeExecutionLogic.setInfiniteLoopTimeoutInMs(infiniteLoopTimeoutInMs);
        defaultChangeExecutionHooks.forEach(hook -> changeExecutionLogic.addHook(hook));
        extraChangeExecutionHooks.forEach(hook -> changeExecutionLogic.addHook(hook));
        changeExecutionLogic.execute(changes);
    }

    @Override
    public <T extends IPResource> IPResourceQuery<T> createResourceQuery(Class<T> resourceClass) {
        List<IPResourceDefinition> resourceDefinitions = resourceDefinitionService.getResourceDefinitions(resourceClass);

        if (resourceDefinitions.isEmpty()) {
            throw new SmallToolsException("Resource class " + resourceClass.getName() + " is unknown");
        }

        return new IPResourceQuery<>(resourceDefinitions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IPResource> IPResourceQuery<T> createResourceQuery(String resourceType) {
        IPResourceDefinition resourceDefinition = resourceDefinitionService.getResourceDefinition(resourceType);
        if (resourceDefinition == null) {
            throw new SmallToolsException("Resource type " + resourceType + " is unknown");
        }
        return (IPResourceQuery<T>) createResourceQuery(resourceDefinition.getResourceClass());
    }

    @Override
    public List<ChangeExecutionHook> getDefaultChangeExecutionHooks() {
        return defaultChangeExecutionHooks;
    }

    public long getInfiniteLoopTimeoutInMs() {
        return infiniteLoopTimeoutInMs;
    }

    @Override
    public IPResourceDefinition getResourceDefinition(Class<? extends IPResource> resourceClass) {
        return resourceDefinitionService.getResourceDefinition(resourceClass);
    }

    @Override
    public IPResourceDefinition getResourceDefinition(IPResource resource) {
        return resourceDefinitionService.getResourceDefinition(resource.getClass());
    }

    @Override
    public IPResourceDefinition getResourceDefinition(String resourceType) {
        return resourceDefinitionService.getResourceDefinition(resourceType);
    }

    @Override
    public List<IPResourceDefinition> getResourceDefinitions() {
        return resourceDefinitionService.getResourceDefinitions();
    }

    @Override
    public void linkAdd(String fromResourceId, String linkType, String toResourceId) {
        String fromResourceType = pluginResourceRepository.findById(fromResourceId).get().getType();
        String toResourceType = pluginResourceRepository.findById(toResourceId).get().getType();
        pluginResourceLinkRepository.save(new PluginResourceLink(fromResourceId, fromResourceType, linkType, toResourceId, toResourceType));
    }

    @Override
    public boolean linkDelete(String fromResourceId, String linkType, String toResourceId) {
        return pluginResourceLinkRepository.deleteAllByFromResourceIdAndLinkTypeAndToResourceId(fromResourceId, linkType, toResourceId) > 0;
    }

    @Override
    public boolean linkExists(String fromResourceId, String linkType, String toResourceId) {
        return pluginResourceLinkRepository.existsByFromResourceIdAndLinkTypeAndToResourceId(fromResourceId, linkType, toResourceId);
    }

    @Override
    public boolean linkExistsByFromResourceAndLinkTypeAndToResource(IPResource fromResource, String linkType, IPResource toResource) {
        String fromInternalId = resourceFindIdByPk(fromResource);
        if (fromInternalId == null) {
            return false;
        }
        String toInternalId = resourceFindIdByPk(toResource);
        if (toInternalId == null) {
            return false;
        }
        return pluginResourceLinkRepository.existsByFromResourceIdAndLinkTypeAndToResourceId(fromInternalId, linkType, toInternalId);
    }

    @Override
    public List<Tuple2<String, ? extends IPResource>> linkFindAllByFromResource(IPResource fromResource) {
        String fromInternalId = resourceFindIdByPk(fromResource);
        if (fromInternalId == null) {
            return Collections.emptyList();
        }
        return linkFindAllByFromResource(fromInternalId);
    }

    @Override
    public List<Tuple2<String, ? extends IPResource>> linkFindAllByFromResource(String fromResourceId) {
        List<PluginResourceLink> resourceLinks = pluginResourceLinkRepository.findAllByFromResourceId(fromResourceId);
        Iterable<PluginResource> toResources = pluginResourceRepository.findAllById(resourceLinks.stream().map(PluginResourceLink::getToResourceId).sorted().distinct().collect(Collectors.toList()));
        Map<String, IPResource> resourceByInternalId = StreamSupport.stream(toResources.spliterator(), false) //
                .collect(Collectors.toMap(PluginResource::getId, PluginResource::getResource));
        return resourceLinks.stream() //
                .map(it -> new Tuple2<>(it.getLinkType(), resourceByInternalId.get(it.getToResourceId()))) //
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends IPResource> linkFindAllByFromResourceAndLinkType(IPResource fromResource, String linkType) {
        String fromResourceId = resourceFindIdByPk(fromResource);
        if (fromResourceId == null) {
            return Collections.emptyList();
        }

        List<PluginResourceLink> resourceLinks = pluginResourceLinkRepository.findAllByFromResourceIdAndLinkType(fromResourceId, linkType);
        Iterable<PluginResource> toResources = pluginResourceRepository.findAllById(resourceLinks.stream().map(PluginResourceLink::getToResourceId).sorted().distinct().collect(Collectors.toList()));
        Map<String, IPResource> resourceByInternalId = StreamSupport.stream(toResources.spliterator(), false) //
                .collect(Collectors.toMap(PluginResource::getId, PluginResource::getResource));
        return resourceLinks.stream() //
                .map(it -> resourceByInternalId.get(it.getToResourceId())) //
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends IPResource> List<R> linkFindAllByFromResourceAndLinkTypeAndToResourceClass(IPResource fromResource, String linkType, Class<R> toResourceClass) {
        String fromInternalId = resourceFindIdByPk(fromResource);
        if (fromInternalId == null) {
            return Collections.emptyList();
        }
        List<IPResourceDefinition> ipResourceDefinitions = resourceDefinitionService.getResourceDefinitions(toResourceClass);
        List<String> toResourceTypes = ipResourceDefinitions.stream().map(IPResourceDefinition::getResourceType).collect(Collectors.toList());
        List<PluginResourceLink> resourceLinks = pluginResourceLinkRepository.findAllByFromResourceIdAndLinkTypeAndToResourceTypeIn(fromInternalId, linkType, toResourceTypes);
        Iterable<PluginResource> toResources = pluginResourceRepository.findAllById(resourceLinks.stream().map(PluginResourceLink::getToResourceId).sorted().distinct().collect(Collectors.toList()));
        Map<String, IPResource> resourceByInternalId = StreamSupport.stream(toResources.spliterator(), false) //
                .collect(Collectors.toMap(PluginResource::getId, PluginResource::getResource));
        return resourceLinks.stream() //
                .map(it -> (R) resourceByInternalId.get(it.getToResourceId())) //
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends IPResource> List<R> linkFindAllByFromResourceClassAndLinkTypeAndToResource(Class<R> fromResourceClass, String linkType, IPResource toResource) {
        String toInternalId = resourceFindIdByPk(toResource);
        if (toInternalId == null) {
            return Collections.emptyList();
        }
        List<IPResourceDefinition> ipResourceDefinitions = resourceDefinitionService.getResourceDefinitions(fromResourceClass);
        List<String> fromResourceTypes = ipResourceDefinitions.stream().map(IPResourceDefinition::getResourceType).collect(Collectors.toList());
        List<PluginResourceLink> resourceLinks = pluginResourceLinkRepository.findAllByFromResourceTypeInAndLinkTypeAndToResourceId(fromResourceTypes, linkType, toInternalId);
        Iterable<PluginResource> fromResources = pluginResourceRepository
                .findAllById(resourceLinks.stream().map(PluginResourceLink::getFromResourceId).sorted().distinct().collect(Collectors.toList()));
        Map<String, IPResource> resourceByInternalId = StreamSupport.stream(fromResources.spliterator(), false) //
                .collect(Collectors.toMap(PluginResource::getId, PluginResource::getResource));
        return resourceLinks.stream() //
                .map(it -> (R) resourceByInternalId.get(it.getFromResourceId())) //
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends IPResource> linkFindAllByLinkTypeAndToResource(String linkType, IPResource toResource) {
        String toResourceId = resourceFindIdByPk(toResource);
        if (toResourceId == null) {
            return Collections.emptyList();
        }

        List<PluginResourceLink> resourceLinks = pluginResourceLinkRepository.findAllByLinkTypeAndToResourceId(linkType, toResourceId);
        Iterable<PluginResource> fromResources = pluginResourceRepository
                .findAllById(resourceLinks.stream().map(PluginResourceLink::getFromResourceId).sorted().distinct().collect(Collectors.toList()));
        Map<String, IPResource> resourceByInternalId = StreamSupport.stream(fromResources.spliterator(), false) //
                .collect(Collectors.toMap(PluginResource::getId, PluginResource::getResource));
        return resourceLinks.stream() //
                .map(it -> resourceByInternalId.get(it.getFromResourceId())) //
                .collect(Collectors.toList());
    }

    @Override
    public List<Tuple2<? extends IPResource, String>> linkFindAllByToResource(IPResource toResource) {
        String toInternalId = resourceFindIdByPk(toResource);
        if (toInternalId == null) {
            return Collections.emptyList();
        }
        return linkFindAllByToResource(toInternalId);
    }

    @Override
    public List<Tuple2<? extends IPResource, String>> linkFindAllByToResource(String toResourceId) {
        List<PluginResourceLink> resourceLinks = pluginResourceLinkRepository.findAllByToResourceId(toResourceId);
        Iterable<PluginResource> fromResources = pluginResourceRepository
                .findAllById(resourceLinks.stream().map(PluginResourceLink::getFromResourceId).sorted().distinct().collect(Collectors.toList()));
        Map<String, IPResource> resourceByInternalId = StreamSupport.stream(fromResources.spliterator(), false) //
                .collect(Collectors.toMap(PluginResource::getId, PluginResource::getResource));
        return resourceLinks.stream() //
                .map(it -> new Tuple2<>(resourceByInternalId.get(it.getFromResourceId()), it.getLinkType())) //
                .collect(Collectors.toList());
    }

    @Override
    public List<Tuple3<IPResource, String, IPResource>> linkFindAllRelatedByResource(IPResource resource) {
        String internalId = resourceFindIdByPk(resource);
        if (internalId == null) {
            return Collections.emptyList();
        }
        return linkFindAllRelatedByResource(internalId);
    }

    @Override
    public List<Tuple3<IPResource, String, IPResource>> linkFindAllRelatedByResource(String internalResourceId) {
        List<PluginResourceLink> resourceLinks = pluginResourceLinkRepository.findAllByFromResourceIdOrToResourceId(internalResourceId, internalResourceId);

        Iterable<PluginResource> resources = pluginResourceRepository
                .findAllById(resourceLinks.stream().flatMap(it -> Arrays.asList(it.getFromResourceId(), it.getToResourceId()).stream()).sorted().distinct().collect(Collectors.toList()));
        Map<String, IPResource> resourceByInternalId = StreamSupport.stream(resources.spliterator(), false) //
                .collect(Collectors.toMap(PluginResource::getId, PluginResource::getResource));

        return resourceLinks.stream() //
                .map(it -> new Tuple3<>(resourceByInternalId.get(it.getFromResourceId()), it.getLinkType(), resourceByInternalId.get(it.getToResourceId()))) //
                .collect(Collectors.toList());
    }

    @Override
    public IPResource resourceAdd(IPResource resource) {
        String resourceType = resourceDefinitionService.getResourceDefinition(resource).getResourceType();
        PluginResource pluginResource = pluginResourceRepository.save(new PluginResource(resourceType, resource));
        resource.setInternalId(pluginResource.getId());
        return resource;
    }

    @Override
    public void resourceAdd(IPResourceDefinition resourceDefinition) {
        resourceDefinitionService.resourceAdd(resourceDefinition);
    }

    @Override
    public boolean resourceDelete(String resourceId) {
        pluginResourceLinkRepository.deleteAllByFromResourceIdOrToResourceId(resourceId, resourceId);
        return pluginResourceRepository.deleteOneById(resourceId) > 0;
    }

    @Override
    public <R extends IPResource, T extends IPResource> boolean resourceEqualsPk(R a, T b) {
        // nulls
        if (a == null) {
            return b == null;
        }
        if (b == null) {
            return false;
        }

        // Type
        Class<? extends IPResource> aClass = a.getClass();
        Class<? extends IPResource> bClass = b.getClass();
        if (!aClass.equals(bClass)) {
            return false;
        }

        // PK
        IPResourceDefinition resourceDefinition = getResourceDefinition(aClass);
        if (resourceDefinition == null) {
            return false;
        }
        for (String pkName : resourceDefinition.getPrimaryKeyProperties()) {
            Method pkGetter = resourceDefinition.getPropertyGetterMethod(pkName);
            Object aValue;
            Object bValue;
            try {
                aValue = pkGetter.invoke(a);
                bValue = pkGetter.invoke(b);
            } catch (Exception e) {
                return false;
            }

            // Nulls
            if (aValue == null) {
                if (bValue != null) {
                    return false;
                }
            } else {
                if (!aValue.equals(bValue)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public <T extends IPResource> Optional<T> resourceFind(IPResourceQuery<T> query) {
        List<T> results = resourceFindAll(query);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        AssertTools.assertTrue(results.size() <= 1, "There are more than one item matching the query");
        return Optional.of(results.get(0));
    }

    @Override
    public Optional<IPResource> resourceFind(String internalResourceId) {
        Optional<PluginResource> pluginResourceOptional = pluginResourceRepository.findById(internalResourceId);
        if (pluginResourceOptional.isEmpty()) {
            return Optional.empty();
        }
        PluginResource pluginResource = pluginResourceOptional.get();
        IPResource resource = pluginResource.getResource();
        return Optional.of(resource);
    }

    @Override
    public List<? extends IPResource> resourceFindAll() {
        return pluginResourceRepository.findAll().stream() //
                .map(it -> it.getResource()) //
                .collect(Collectors.toList());
    }

    @Override
    public <T extends IPResource> List<T> resourceFindAll(IPResourceQuery<T> query) {
        return pluginResourceRepository.findAll(query);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends IPResource> Optional<R> resourceFindByPk(R resource) {
        return resourceFind(createResourceQuery((Class<R>) resource.getClass()) //
                .primaryKeyEquals(resource));
    }

    private String resourceFindIdByPk(IPResource resource) {
        // Id already there
        if (resource.getInternalId() != null) {
            return resource.getInternalId();
        }

        // Search by PK
        Optional<IPResource> o = resourceFindByPk(resource);
        if (o.isPresent()) {
            return o.get().getInternalId();
        }

        // Does not exist
        return null;
    }

    @Override
    public void resourceUpdate(IPResource previousResource, IPResource updatedResource) {
        Optional<PluginResource> pluginResourceOptional = pluginResourceRepository.findById(previousResource.getInternalId());
        if (pluginResourceOptional.isEmpty()) {
            throw new ResourceNotFoundException(previousResource);
        }

        PluginResource pluginResource = pluginResourceOptional.get();
        String resourceType = getResourceDefinition(updatedResource).getResourceType();
        pluginResource.store(resourceType, updatedResource);
        pluginResourceRepository.save(pluginResource);
    }

    @Override
    public void setDefaultChangeExecutionHooks(List<ChangeExecutionHook> defaultChangeExecutionHooks) {
        this.defaultChangeExecutionHooks = defaultChangeExecutionHooks;
    }

    @Override
    public void setInfiniteLoopTimeoutInMs(long infiniteLoopTimeoutInMs) {
        this.infiniteLoopTimeoutInMs = infiniteLoopTimeoutInMs;
    }

    @Override
    public void tagAdd(String resourceId, String tagName) {
        pluginResourceRepository.addTagById(resourceId, tagName);
    }

    @Override
    public boolean tagDelete(String resourceId, String tagName) {
        return pluginResourceRepository.removeTagById(resourceId, tagName);
    }

    @Override
    public boolean tagExists(String resourceId, String tagName) {
        PluginResource pluginResource = new PluginResource() //
                .setId(resourceId) //
                .addTag(tagName);
        Example<PluginResource> example = Example.of(pluginResource);
        return pluginResourceRepository.exists(example);
    }

    @Override
    public Set<String> tagFindAllByResource(IPResource resource) {
        String internalId = resourceFindIdByPk(resource);
        if (internalId == null) {
            return Collections.emptySet();
        }

        Optional<PluginResource> pluginResource = pluginResourceRepository.findById(internalId);
        if (pluginResource.isPresent()) {
            return pluginResource.get().getTags();
        } else {
            return Collections.emptySet();
        }
    }

}
