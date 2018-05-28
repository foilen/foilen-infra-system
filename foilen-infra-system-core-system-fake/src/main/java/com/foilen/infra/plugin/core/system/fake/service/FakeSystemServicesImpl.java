/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.UpdateEventContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.UpdateEventHandler;
import com.foilen.infra.plugin.v1.core.exception.InfiniteUpdateLoop;
import com.foilen.infra.plugin.v1.core.exception.ResourceNotFoundException;
import com.foilen.infra.plugin.v1.core.exception.ResourceNotFromRepositoryException;
import com.foilen.infra.plugin.v1.core.exception.ResourcePrimaryKeyCollisionException;
import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.core.resource.IPResourceQuery;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.MessagingService;
import com.foilen.infra.plugin.v1.core.service.SecurityService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalIPResourceService;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.exception.SmallToolsException;
import com.foilen.smalltools.reflection.ReflectionTools;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.AssertTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.foilen.smalltools.tuple.Tuple3;

@Component
public class FakeSystemServicesImpl extends AbstractBasics implements MessagingService, IPResourceService, InternalIPResourceService, InternalChangeService, SecurityService {

    // Services
    @Autowired
    private CommonServicesContext commonServicesContext;
    @Autowired
    private InternalServicesContext internalServicesContext;

    // In memory data
    private List<IPResource> resources = new ArrayList<>();
    private List<Tuple3<Long, String, Long>> links = new ArrayList<>();
    private List<Tuple2<Long, String>> tags = new ArrayList<>();

    private Map<Class<? extends IPResource>, List<Class<?>>> allClassesByResourceClass = new HashMap<>();
    private Map<Class<? extends IPResource>, IPResourceDefinition> resourceDefinitionByResourceClass = new HashMap<>();
    private Map<String, IPResourceDefinition> resourceDefinitionByResourceType = new HashMap<>();

    // Internal
    private AtomicLong nextId = new AtomicLong(1);
    private AtomicLong nextTxId = new AtomicLong(0);

    @Override
    public void alertingError(String shortDescription, String longDescription) {
        logger.error("ALERT [{}]: [{}]", shortDescription, longDescription);
    }

    @Override
    public void alertingInfo(String shortDescription, String longDescription) {
        logger.info("ALERT [{}]: [{}]", shortDescription, longDescription);
    }

    @Override
    public void alertingWarn(String shortDescription, String longDescription) {
        logger.warn("ALERT [{}]: [{}]", shortDescription, longDescription);
    }

    private void applyChanges(ChangesContext changes, Queue<IPResource> addedResources, Queue<IPResource> updatedResourcesPrevious, Queue<IPResource> deletedResources,
            Map<Long, List<Tuple3<IPResource, String, IPResource>>> deletedResourcePreviousLinksByResourceId, Set<Long> removedResourcesInThisTransaction, Queue<Long> resourcesNeedRefresh) {

        logger.debug("State before applying changes. Has {} updates, {} deletions, {} addition, {} refreshes", updatedResourcesPrevious.size(), deletedResources.size(), addedResources.size(),
                resourcesNeedRefresh.size());
        logger.debug("[APPLY] Resources: has {} updates, {} deletions, {} addition, {} refreshes ; Links: has {} deletions, {} addition ; Tags: has {} deletions, {} addition", //
                changes.getResourcesToUpdate().size(), changes.getResourcesToDelete().size(), changes.getResourcesToAdd().size(), changes.getResourcesToRefresh().size(), //
                changes.getLinksToDelete().size(), changes.getLinksToAdd().size(), //
                changes.getTagsToDelete().size(), changes.getTagsToAdd().size() //
        );

        // Delete
        Set<Long> toRefreshIds = new HashSet<>();
        for (Long id : changes.getResourcesToDelete()) {

            if (removedResourcesInThisTransaction.add(id)) {
                logger.debug("[APPLY] Delete resource {}", id);
            } else {
                logger.debug("[APPLY-SKIP] Delete resource {}. Already deleted in this transaction. Skipping", id);

                continue;
            }

            List<Tuple3<IPResource, String, IPResource>> deletedResourcePreviousLinks = new ArrayList<>();
            deletedResourcePreviousLinksByResourceId.put(id, deletedResourcePreviousLinks);

            IPResource resource = resourceFind(id).get();
            deletedResources.add(resource);
            resourcesNeedRefresh.remove(id);
            deletedResourcePreviousLinks.addAll(linkFindAllRelatedByResource(id));
            Set<Long> idsToUpdate = new HashSet<>();
            idsToUpdate.addAll(linkFindAllByFromResource(resource).stream().map(it -> it.getB().getInternalId()).collect(Collectors.toList()));
            idsToUpdate.addAll(linkFindAllByToResource(resource).stream().map(it -> it.getA().getInternalId()).collect(Collectors.toList()));
            markAllTransientLinkedResourcesToUpdate(toRefreshIds, idsToUpdate);
            resources.removeIf(it -> id.equals(it.getInternalId()));
            links.removeIf(it -> id.equals(it.getA()) || id.equals(it.getC()));
            tags.removeIf(it -> id.equals(it.getA()));
        }
        for (Tuple3<IPResource, String, IPResource> link : changes.getLinksToDelete()) {
            logger.debug("[APPLY] Delete link {}", link);
            Optional<IPResource> fromResource = resourceFindByPk(link.getA());
            Optional<IPResource> toResource = resourceFindByPk(link.getC());
            if (fromResource.isPresent() && toResource.isPresent()) {
                Long fromId = fromResource.get().getInternalId();
                Long toId = toResource.get().getInternalId();
                if (links.removeIf( //
                        it -> fromId.equals(it.getA()) && //
                                link.getB().equals(it.getB()) && //
                                toId.equals(it.getC()) //
                )) {
                    markAllTransientLinkedResourcesToUpdate(toRefreshIds, Arrays.asList(fromId, toId));
                } else {
                    logger.debug("[APPLY-SKIP] Delete link {}. Skipped since does not exists", link);
                }
            }
        }
        for (Tuple2<IPResource, String> tag : changes.getTagsToDelete()) {
            logger.debug("[APPLY] Delete tag {}", tag);
            Optional<IPResource> resource = resourceFindByPk(tag.getA());
            if (resource.isPresent()) {
                Long id = resource.get().getInternalId();
                if (tags.removeIf(it -> id.equals(it.getA()) && tag.getB().equals(it.getB()))) {
                    toRefreshIds.add(id);
                } else {
                    logger.debug("[APPLY-SKIP] Delete tag {}. Skipped since does not exists", tag);
                }
            }
        }

        // Add
        for (IPResource resource : changes.getResourcesToAdd()) {
            logger.debug("[APPLY] Add resource {}", resource);
            // Check if already exists
            if (resourceFindByPk(resource).isPresent()) {
                throw new ResourcePrimaryKeyCollisionException();
            }

            resource = clone(resource);
            resource.setInternalId(nextId.getAndIncrement());
            resources.add(resource);
            addedResources.add(resource);
            resourcesNeedRefresh.remove(resource.getInternalId());

            // Add the direct links for update notification
            Set<Long> idsToUpdate = new HashSet<>();
            idsToUpdate.addAll(linkFindAllByFromResource(resource).stream().map(it -> it.getB().getInternalId()).collect(Collectors.toList()));
            idsToUpdate.addAll(linkFindAllByToResource(resource).stream().map(it -> it.getA().getInternalId()).collect(Collectors.toList()));
            markAllTransientLinkedResourcesToUpdate(toRefreshIds, idsToUpdate);

        }
        for (Tuple3<IPResource, String, IPResource> link : changes.getLinksToAdd()) {
            logger.debug("[APPLY] Add link {}", link);
            Optional<IPResource> fromResource = resourceFindByPk(link.getA());
            if (!fromResource.isPresent()) {
                throw new ResourceNotFoundException(link.getA());
            }
            Optional<IPResource> toResource = resourceFindByPk(link.getC());
            if (!toResource.isPresent()) {
                throw new ResourceNotFoundException(link.getC());
            }

            Long fromId = fromResource.get().getInternalId();
            Long toId = toResource.get().getInternalId();
            // Add if not present
            if (links.stream().filter( //
                    it -> fromId.equals(it.getA()) && //
                            link.getB().equals(it.getB()) && //
                            toId.equals(it.getC()) //
            ).findAny().isPresent()) {
                logger.debug("[APPLY-SKIP] Add link {}. Skipped since does not exists", link);
            } else {
                // Add
                links.add(new Tuple3<>(fromId, link.getB(), toId));
                markAllTransientLinkedResourcesToUpdate(toRefreshIds, Arrays.asList(fromId, toId));
            }

        }
        for (Tuple2<IPResource, String> tag : changes.getTagsToAdd()) {
            logger.debug("[APPLY] Add tag {}", tag);
            Optional<IPResource> resource = resourceFindByPk(tag.getA());
            if (!resource.isPresent()) {
                throw new ResourceNotFoundException(tag.getA());
            }

            Long id = resource.get().getInternalId();
            // Add if not present
            if (tags.stream().filter(it -> id.equals(it.getA()) && tag.getB().equals(it.getB())).findAny().isPresent()) {
                logger.debug("[APPLY-SKIP] Add tag {}. Skipped since does not exists", tag);
            } else {
                // Add
                tags.add(new Tuple2<>(id, tag.getB()));
                toRefreshIds.add(id);
            }
        }
        toRefreshIds.removeAll(removedResourcesInThisTransaction);
        toRefreshIds.forEach(toRefreshId -> {
            if (idNotInAnyQueues(toRefreshId, addedResources, updatedResourcesPrevious, deletedResources, resourcesNeedRefresh)) {
                resourcesNeedRefresh.add(toRefreshId);
            }
        });

        Set<Long> toRefreshIdsSeconds = new HashSet<>();
        // Update
        for (Tuple2<Long, IPResource> update : changes.getResourcesToUpdate()) {

            logger.debug("[APPLY] Update resource {}", update);

            // Get the previous resource
            Optional<IPResource> previousResourceOptional = resourceFind(update.getA());
            if (!previousResourceOptional.isPresent()) {
                throw new ResourceNotFoundException(update.getA());
            }
            IPResource previousResource = previousResourceOptional.get();
            // Add if not already in the list
            if (!updatedResourcesPrevious.stream().filter(it -> previousResource.getInternalId().equals(it.getInternalId())).findAny().isPresent()) {
                updatedResourcesPrevious.add(previousResource);
                resourcesNeedRefresh.remove(previousResource.getInternalId());
            }

            // Get the next resource (might not exists)
            IPResource updatedResource = update.getB();
            Optional<IPResource> nextResourceOptional = resourceFindByPk(updatedResource);
            if (nextResourceOptional.isPresent()) {
                // Check if not the same resource
                IPResource nextResource = nextResourceOptional.get();
                if (!previousResource.getInternalId().equals(nextResource.getInternalId())) {
                    throw new ResourcePrimaryKeyCollisionException();
                }
            }

            // Update the resource
            updatedResource.setInternalId(update.getA());

            // Check if really different
            String jsonPrevious = JsonTools.compactPrint(previousResource);
            String jsonNew = JsonTools.compactPrint(updatedResource);
            if (jsonPrevious.equals(jsonNew)) {
                logger.debug("[APPLY-SKIP] Update resource {}. Skipped since no change", update);
            } else {
                resources.removeIf(it -> update.getA().equals(it.getInternalId()));
                resources.add(clone(updatedResource));

                // Add the direct links for update notification
                updatedResourcesPrevious.addAll(linkFindAllByFromResource(updatedResource).stream().map(it -> it.getB()).collect(Collectors.toList()));
                updatedResourcesPrevious.addAll(linkFindAllByToResource(updatedResource).stream().map(it -> it.getA()).collect(Collectors.toList()));
                // Add all the transient managed resources links for update notification
                markAllTransientLinkedResourcesToUpdate(toRefreshIdsSeconds, Arrays.asList(updatedResource.getInternalId()));
            }
        }

        // Refreshes
        for (Long id : changes.getResourcesToRefresh()) {
            logger.debug("[APPLY] Refresh resource {}", id);
            if (idNotInAnyQueues(id, addedResources, updatedResourcesPrevious, deletedResources, resourcesNeedRefresh)) {
                resourcesNeedRefresh.add(id);
            } else {
                logger.debug("[APPLY-SKIP] Refresh resource {}. Already waiting for a refresh in any category", id);
            }
        }

        toRefreshIdsSeconds.removeAll(toRefreshIds);
        toRefreshIdsSeconds.forEach(toRefreshId -> {
            if (idNotInAnyQueues(toRefreshId, addedResources, updatedResourcesPrevious, deletedResources, resourcesNeedRefresh)) {
                resourcesNeedRefresh.add(toRefreshId);
            }
        });

        // Cleanup lists
        removedResourcesInThisTransaction.addAll(deletedResources.stream().map(IPResource::getInternalId).collect(Collectors.toSet()));
        updatedResourcesPrevious.removeIf(it -> removedResourcesInThisTransaction.contains(it.getInternalId()));

        changes.clear();
        logger.debug("State after applying changes. Has {} updates, {} deletions, {} addition, {} refreshes", updatedResourcesPrevious.size(), deletedResources.size(), addedResources.size(),
                resourcesNeedRefresh.size());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void changesExecute(ChangesContext changes) {

        long txId = nextTxId.incrementAndGet();
        logger.info("----- [Transaction] Begin {} -----", txId);

        // Create a transaction
        List<IPResource> beforeTxResources = resources.stream().map(it -> it.deepClone()).collect(Collectors.toList());
        List<Tuple3<Long, String, Long>> beforeTxLinks = links.stream().map(it -> new Tuple3<>(it.getA(), it.getB(), it.getC())).collect(Collectors.toList());
        List<Tuple2<Long, String>> beforeTxTags = tags.stream().map(it -> new Tuple2<>(it.getA(), it.getB())).collect(Collectors.toList());

        long maxTime = System.currentTimeMillis() + 15000;

        try {

            Queue<IPResource> addedResources = new LinkedBlockingQueue<>();
            Queue<IPResource> deletedResources = new LinkedBlockingQueue<>();
            Map<Long, List<Tuple3<IPResource, String, IPResource>>> deletedResourcePreviousLinksByResourceId = new HashMap<>();
            Queue<IPResource> updatedResourcesPrevious = new LinkedBlockingQueue<>();
            Set<Long> removedResourcesInThisTransaction = new HashSet<>();
            Queue<Long> resourcesNeedRefresh = new LinkedBlockingQueue<>();

            Map<Class<?>, List<UpdateEventContext>> updateEventContextsByResourceType = commonServicesContext.getPluginService().getUpdateEvents().stream() //
                    .collect(Collectors.groupingBy(it -> it.getUpdateEventHandler().supportedClass()));

            // Apply the changes
            applyChanges(changes, addedResources, updatedResourcesPrevious, deletedResources, deletedResourcePreviousLinksByResourceId, removedResourcesInThisTransaction, resourcesNeedRefresh);

            while (System.currentTimeMillis() < maxTime && (!addedResources.isEmpty() || !updatedResourcesPrevious.isEmpty() || !deletedResources.isEmpty() || !resourcesNeedRefresh.isEmpty())) {

                logger.debug("Update events loop. Has {} updates, {} deletions, {} addition, {} refreshes", updatedResourcesPrevious.size(), deletedResources.size(), addedResources.size(),
                        resourcesNeedRefresh.size());

                // Process all updates
                IPResource itemPrevious;
                while (System.currentTimeMillis() < maxTime && (itemPrevious = updatedResourcesPrevious.poll()) != null) {
                    Optional<IPResource> currentResourceOptional = resourceFind(itemPrevious.getInternalId());
                    if (!currentResourceOptional.isPresent()) {
                        throw new ResourceNotFoundException(itemPrevious);
                    }
                    IPResource currentResource = currentResourceOptional.get();
                    List<UpdateEventContext> eventContexts = updateEventContextsByResourceType.get(itemPrevious.getClass());
                    if (eventContexts != null) {
                        logger.debug("[UPDATE EVENT] Processing {} updated handlers", eventContexts.size());
                        for (UpdateEventContext eventContext : eventContexts) {
                            logger.debug("[UPDATE EVENT] Processing {} updated handler", eventContext.getUpdateHandlerName());
                            UpdateEventHandler updateEventHandler = eventContext.getUpdateEventHandler();
                            updateEventHandler.updateHandler(commonServicesContext, changes, itemPrevious, currentResource);
                            applyChanges(changes, addedResources, updatedResourcesPrevious, deletedResources, deletedResourcePreviousLinksByResourceId, removedResourcesInThisTransaction,
                                    resourcesNeedRefresh);
                        }
                    }
                }

                // Process all deletes
                IPResource item;
                while (System.currentTimeMillis() < maxTime && (item = deletedResources.poll()) != null) {
                    List<UpdateEventContext> eventContexts = updateEventContextsByResourceType.get(item.getClass());
                    if (eventContexts != null) {
                        logger.debug("[UPDATE EVENT] Processing {} deleted handlers", eventContexts.size());
                        for (UpdateEventContext eventContext : eventContexts) {
                            logger.debug("[UPDATE EVENT] Processing {} deleted handler", eventContext.getUpdateHandlerName());
                            UpdateEventHandler updateEventHandler = eventContext.getUpdateEventHandler();
                            updateEventHandler.deleteHandler(commonServicesContext, changes, item, deletedResourcePreviousLinksByResourceId.get(item.getInternalId()));
                            applyChanges(changes, addedResources, updatedResourcesPrevious, deletedResources, deletedResourcePreviousLinksByResourceId, removedResourcesInThisTransaction,
                                    resourcesNeedRefresh);
                        }
                    }
                }

                // Process all adds
                while (System.currentTimeMillis() < maxTime && (item = addedResources.poll()) != null) {
                    List<UpdateEventContext> eventContexts = updateEventContextsByResourceType.get(item.getClass());
                    if (eventContexts != null) {
                        logger.debug("[UPDATE EVENT] Processing {} added handlers", eventContexts.size());
                        for (UpdateEventContext eventContext : eventContexts) {
                            logger.debug("[UPDATE EVENT] Processing {} added handler", eventContext.getUpdateHandlerName());
                            UpdateEventHandler updateEventHandler = eventContext.getUpdateEventHandler();
                            updateEventHandler.addHandler(commonServicesContext, changes, item);
                            applyChanges(changes, addedResources, updatedResourcesPrevious, deletedResources, deletedResourcePreviousLinksByResourceId, removedResourcesInThisTransaction,
                                    resourcesNeedRefresh);
                        }
                    }
                }

                // Process all refreshes
                Long id;
                while (System.currentTimeMillis() < maxTime && (id = resourcesNeedRefresh.poll()) != null) {
                    Optional<IPResource> optionalResource = resourceFind(id);
                    if (optionalResource.isPresent()) {
                        item = optionalResource.get();
                        List<UpdateEventContext> eventContexts = updateEventContextsByResourceType.get(item.getClass());
                        if (eventContexts != null) {
                            logger.debug("[UPDATE EVENT] Processing {} refresh handlers", eventContexts.size());
                            for (UpdateEventContext eventContext : eventContexts) {
                                logger.debug("[UPDATE EVENT] Processing {} refresh handler", eventContext.getUpdateHandlerName());
                                UpdateEventHandler updateEventHandler = eventContext.getUpdateEventHandler();
                                updateEventHandler.checkAndFix(commonServicesContext, changes, item);
                                applyChanges(changes, addedResources, updatedResourcesPrevious, deletedResources, deletedResourcePreviousLinksByResourceId, removedResourcesInThisTransaction,
                                        resourcesNeedRefresh);
                            }
                        }
                    }
                }

                // Apply any pending changes
                if (System.currentTimeMillis() < maxTime) {
                    applyChanges(changes, addedResources, updatedResourcesPrevious, deletedResources, deletedResourcePreviousLinksByResourceId, removedResourcesInThisTransaction,
                            resourcesNeedRefresh);
                }
            }

            if (!addedResources.isEmpty() || !updatedResourcesPrevious.isEmpty() || !deletedResources.isEmpty() || !resourcesNeedRefresh.isEmpty()) {
                throw new InfiniteUpdateLoop("Iterated for too long and there are always changes");
            }

            // Complete the transaction

        } catch (RuntimeException e) {
            // Rollback the transaction
            logger.error("===== [Transaction] Problem while executing the changes. Rolling back transaction {} =====", txId, e);
            resources = beforeTxResources;
            links = beforeTxLinks;
            tags = beforeTxTags;
            throw e;
        }

        logger.info("===== [Transaction] Completed {} =====", txId);

    }

    private <R extends IPResource> R clone(R resource) {
        R clonedResource = JsonTools.clone(resource);
        clonedResource.setInternalId(resource.getInternalId());
        return clonedResource;
    }

    @Override
    public <T extends IPResource> IPResourceQuery<T> createResourceQuery(Class<T> resourceClass) {
        List<IPResourceDefinition> resourceDefinitions = allClassesByResourceClass.entrySet().stream() //
                .filter(it -> it.getValue().contains(resourceClass)) //
                .map(it -> resourceDefinitionByResourceClass.get(it.getKey())) //
                .filter(it -> it != null) //
                .collect(Collectors.toList());

        if (resourceDefinitions.isEmpty()) {
            throw new SmallToolsException("Resource class " + resourceClass.getName() + " is unknown");
        }

        return new IPResourceQuery<>(resourceDefinitions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IPResource> IPResourceQuery<T> createResourceQuery(String resourceType) {
        IPResourceDefinition resourceDefinition = resourceDefinitionByResourceType.get(resourceType);
        if (resourceDefinition == null) {
            throw new SmallToolsException("Resource type " + resourceType + " is unknown");
        }
        return (IPResourceQuery<T>) createResourceQuery(resourceDefinition.getResourceClass());
    }

    public CommonServicesContext getCommonServicesContext() {
        return commonServicesContext;
    }

    @Override
    public String getCsrfParameterName() {
        return "_csrf";
    }

    @Override
    public String getCsrfValue(Object request) {
        return "fake";
    }

    public InternalServicesContext getInternalServicesContext() {
        return internalServicesContext;
    }

    public List<Tuple3<Long, String, Long>> getLinks() {
        return links;
    }

    @Override
    public IPResourceDefinition getResourceDefinition(Class<? extends IPResource> resourceClass) {
        return resourceDefinitionByResourceClass.get(resourceClass);
    }

    @Override
    public IPResourceDefinition getResourceDefinition(IPResource resource) {
        return resourceDefinitionByResourceClass.get(resource.getClass());
    }

    @Override
    public IPResourceDefinition getResourceDefinition(String resourceType) {
        return resourceDefinitionByResourceType.get(resourceType);
    }

    @Override
    public List<IPResourceDefinition> getResourceDefinitions() {
        return Collections.unmodifiableList(resourceDefinitionByResourceClass.values().stream().collect(Collectors.toList()));
    }

    public List<IPResource> getResources() {
        return resources;
    }

    public List<Tuple2<Long, String>> getTags() {
        return tags;
    }

    private boolean idNotInAnyQueues(Long id, Queue<IPResource> addedResources, Queue<IPResource> updatedResourcesPrevious, Queue<IPResource> deletedResources, Queue<Long> resourcesNeedRefresh) {
        return !resourcesNeedRefresh.contains(id) //
                && !addedResources.stream().filter(it -> id.equals(it.getInternalId())).findAny().isPresent() //
                && !updatedResourcesPrevious.stream().filter(it -> id.equals(it.getInternalId())).findAny().isPresent() //
                && !deletedResources.stream().filter(it -> id.equals(it.getInternalId())).findAny().isPresent();
    }

    @Override
    public boolean linkExistsByFromResourceAndLinkTypeAndToResource(IPResource fromResource, String linkType, IPResource toResource) {
        if (fromResource.getInternalId() == null) {
            throw new ResourceNotFromRepositoryException(fromResource);
        }
        if (toResource.getInternalId() == null) {
            throw new ResourceNotFromRepositoryException(toResource);
        }
        return links.stream().filter( //
                it -> {
                    return fromResource.getInternalId().equals(it.getA()) && //
                    linkType.equals(it.getB()) && //
                    toResource.getInternalId().equals(it.getC());
                }) //
                .findAny().isPresent();
    }

    @Override
    public List<Tuple2<String, ? extends IPResource>> linkFindAllByFromResource(IPResource fromResource) {
        if (fromResource.getInternalId() == null) {
            throw new ResourceNotFromRepositoryException(fromResource);
        }
        return links.stream().filter( //
                it -> fromResource.getInternalId().equals(it.getA())) //
                .map(it -> new Tuple2<>(it.getB(), resourceFind(it.getC()).get())) //
                .collect(Collectors.toList());
    }

    public List<Tuple2<String, ? extends IPResource>> linkFindAllByFromResource(long fromResourceId) {
        return links.stream().filter( //
                it -> it.getA().equals(fromResourceId)) //
                .map(it -> new Tuple2<>(it.getB(), resourceFind(it.getC()).get())) //
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends IPResource> linkFindAllByFromResourceAndLinkType(IPResource fromResource, String linkType) {
        if (fromResource.getInternalId() == null) {
            throw new ResourceNotFromRepositoryException(fromResource);
        }
        return links.stream().filter( //
                it -> fromResource.getInternalId().equals(it.getA()) && //
                        linkType.equals(it.getB())) //
                .map(it -> resourceFind(it.getC()).get()) //
                .collect(Collectors.toList());
    }

    public List<? extends IPResource> linkFindAllByFromResourceAndLinkType(long fromResourceId, String linkType) {
        return links.stream().filter( //
                it -> it.getA().equals(fromResourceId) && //
                        linkType.equals(it.getB())) //
                .map(it -> resourceFind(it.getC()).get()) //
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends IPResource> List<R> linkFindAllByFromResourceAndLinkTypeAndToResourceClass(IPResource fromResource, String linkType, Class<R> toResourceType) {
        if (fromResource.getInternalId() == null) {
            throw new ResourceNotFromRepositoryException(fromResource);
        }
        return links.stream().filter( //
                it -> {
                    IPResource toResource = resourceFind(it.getC()).get();
                    return fromResource.getInternalId().equals(it.getA()) && //
                    linkType.equals(it.getB()) && //
                    toResourceType.isInstance(toResource);
                }) //
                .map(it -> (R) resourceFind(it.getC()).get()) //
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends IPResource> List<R> linkFindAllByFromResourceClassAndLinkTypeAndToResource(Class<R> fromResourceClass, String linkType, IPResource toResource) {
        if (toResource.getInternalId() == null) {
            throw new ResourceNotFromRepositoryException(toResource);
        }
        return links.stream().filter( //
                it -> {
                    IPResource fromResource = resourceFind(it.getA()).get();
                    return fromResourceClass.isInstance(fromResource) && //
                    linkType.equals(it.getB()) && //
                    toResource.getInternalId().equals(it.getC()); //
                }) //
                .map(it -> (R) resourceFind(it.getA()).get()) //
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends IPResource> linkFindAllByLinkTypeAndToResource(String linkType, IPResource toResource) {
        if (toResource.getInternalId() == null) {
            throw new ResourceNotFromRepositoryException(toResource);
        }
        return links.stream().filter( //
                it -> {
                    return linkType.equals(it.getB()) && //
                    toResource.getInternalId().equals(it.getC()); //
                }) //
                .map(it -> resourceFind(it.getA()).get()) //
                .collect(Collectors.toList());
    }

    public List<? extends IPResource> linkFindAllByLinkTypeAndToResource(String linkType, long toResourceId) {
        return links.stream().filter( //
                it -> {
                    return linkType.equals(it.getB()) && //
                    it.getC().equals(toResourceId); //
                }) //
                .map(it -> resourceFind(it.getA()).get()) //
                .collect(Collectors.toList());
    }

    @Override
    public List<Tuple2<? extends IPResource, String>> linkFindAllByToResource(IPResource toResource) {
        if (toResource.getInternalId() == null) {
            throw new ResourceNotFromRepositoryException(toResource);
        }
        return links.stream().filter( //
                it -> toResource.getInternalId().equals(it.getC())) //
                .map(it -> new Tuple2<>(resourceFind(it.getA()).get(), it.getB())) //
                .collect(Collectors.toList());
    }

    public List<Tuple2<? extends IPResource, String>> linkFindAllByToResource(long toResourceId) {
        return links.stream().filter( //
                it -> it.getC().equals(toResourceId)) //
                .map(it -> new Tuple2<>(resourceFind(it.getA()).get(), it.getB())) //
                .collect(Collectors.toList());
    }

    @Override
    public List<Tuple3<IPResource, String, IPResource>> linkFindAllRelatedByResource(IPResource resource) {
        if (resource.getInternalId() == null) {
            throw new ResourceNotFromRepositoryException(resource);
        }
        return links.stream().filter( //
                it -> resource.getInternalId().equals(it.getA()) || resource.getInternalId().equals(it.getC())) //
                .map(it -> new Tuple3<>(resourceFind(it.getA()).get(), it.getB(), resourceFind(it.getC()).get())) //
                .collect(Collectors.toList());
    }

    @Override
    public List<Tuple3<IPResource, String, IPResource>> linkFindAllRelatedByResource(Long internalResourceId) {
        return links.stream().filter( //
                it -> internalResourceId.equals(it.getA()) || internalResourceId.equals(it.getC())) //
                .map(it -> new Tuple3<>(resourceFind(it.getA()).get(), it.getB(), resourceFind(it.getC()).get())) //
                .collect(Collectors.toList());
    }

    private void markAllTransientLinkedResourcesToUpdate(Set<Long> toRefreshIds, Collection<Long> ids) {
        Set<Long> transientProcessedIds = new HashSet<>();
        markAllTransientLinkedResourcesToUpdate(toRefreshIds, transientProcessedIds, ids);
    }

    private void markAllTransientLinkedResourcesToUpdate(Set<Long> updatedIds, Set<Long> transientProcessedIds, Collection<Long> ids) {

        updatedIds.addAll(ids);

        for (Long id : ids) {
            if (!transientProcessedIds.add(id)) {
                continue;
            }

            List<Tuple2<String, ? extends IPResource>> resourcesTo = linkFindAllByFromResource(id);
            markAllTransientLinkedResourcesToUpdate(updatedIds, transientProcessedIds, resourcesTo.stream().map(it -> it.getB().getInternalId()).collect(Collectors.toSet()));
            List<Tuple2<? extends IPResource, String>> resourcesFrom = linkFindAllByToResource(id);
            markAllTransientLinkedResourcesToUpdate(updatedIds, transientProcessedIds, resourcesFrom.stream().map(it -> it.getA().getInternalId()).collect(Collectors.toSet()));
        }

    }

    protected boolean matchingLike(String likeQuery, String textToCheck) {
        return Pattern.compile(likeQuery.replaceAll("%", ".*")).matcher(textToCheck).matches();
    }

    @Override
    public void resourceAdd(IPResourceDefinition resourceDefinition) {
        resourceDefinitionByResourceClass.put(resourceDefinition.getResourceClass(), resourceDefinition);
        resourceDefinitionByResourceType.put(resourceDefinition.getResourceType(), resourceDefinition);

        allClassesByResourceClass.put(resourceDefinition.getResourceClass(), ReflectionTools.allTypes(resourceDefinition.getResourceClass()));
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
        Class<?> aClass = a.getClass();
        Class<?> bClass = b.getClass();
        if (!aClass.equals(bClass)) {
            return false;
        }

        // PK
        IPResourceDefinition resourceDefinition = resourceDefinitionByResourceClass.get(aClass);
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
    public Optional<IPResource> resourceFind(long internalResourceId) {
        Long internalResourceIdLong = internalResourceId;

        Optional<IPResource> resourceOptional = resources.stream() //
                .filter(it -> internalResourceIdLong.equals(it.getInternalId())) //
                .map(it -> clone(it)) //
                .findAny();

        return resourceOptional;
    }

    @Override
    public List<? extends IPResource> resourceFindAll() {
        return Collections.unmodifiableList(resources);
    }

    @Override
    public <R extends IPResource> List<R> resourceFindAll(IPResourceQuery<R> query) {
        return resourceFindAllNoCloning(query).stream() //
                .map(it -> clone(it)) //
                .collect(Collectors.toList());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <R extends IPResource> List<R> resourceFindAllNoCloning(IPResourceQuery<R> query) {
        List<R> results = resources.stream() //
                .filter(resource -> {

                    List<IPResourceDefinition> resourceDefinitions = query.getResourceDefinitions();

                    // Right type
                    List<Class<? extends IPResource>> resourceClasses = resourceDefinitions.stream().map(IPResourceDefinition::getResourceClass).collect(Collectors.toList());
                    if (!resourceClasses.contains(resource.getClass())) {
                        return false;
                    }

                    // Right ids
                    if (query.getIdsIn() != null && !query.getIdsIn().contains(resource.getInternalId())) {
                        return false;
                    }

                    // Right editor
                    if (query.getEditorsIn() != null && !query.getEditorsIn().contains(resource.getResourceEditorName())) {
                        return false;
                    }

                    // Equals
                    IPResourceDefinition currentResourceDefinition = resourceDefinitionByResourceClass.get(resource.getClass());
                    for (Entry<String, Object> entry : query.getPropertyEquals().entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (propertyValue == null) {
                                if (currentValue == null) {
                                    // Good value
                                    continue;
                                } else {
                                    // Wrong value
                                    return false;
                                }
                            }
                            if (currentValue instanceof Set) {
                                Set currentValueSet = (Set) currentValue;
                                if (propertyValue instanceof Collection) {
                                    Collection<?> propertyValueCollection = (Collection<?>) propertyValue;
                                    if (currentValueSet.size() != propertyValueCollection.size()) {
                                        return false;
                                    }
                                    for (Object it : currentValueSet) {
                                        if (!propertyValueCollection.contains(it)) {
                                            return false;
                                        }
                                    }
                                    for (Object it : propertyValueCollection) {
                                        if (!currentValueSet.contains(it)) {
                                            return false;
                                        }
                                    }
                                    return true;
                                } else {
                                    return false;
                                }
                            } else {
                                if (!propertyValue.equals(currentValue)) {
                                    // Wrong value
                                    return false;
                                }
                            }
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // Contains
                    for (Entry<String, Object> entry : query.getPropertyContains().entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (propertyValue == null) {
                                if (currentValue == null) {
                                    // Good value
                                    continue;
                                } else {
                                    // Wrong value
                                    return false;
                                }
                            }
                            if (currentValue instanceof Set) {
                                Set currentValueSet = (Set) currentValue;
                                if (propertyValue instanceof Collection) {
                                    Collection<?> propertyValueCollection = (Collection<?>) propertyValue;
                                    for (Object it : propertyValueCollection) {
                                        if (!currentValueSet.contains(it)) {
                                            return false;
                                        }
                                    }
                                    return true;
                                } else {
                                    return false;
                                }
                            } else {
                                // Wrong value
                                return false;
                            }
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // Like
                    for (Entry<String, String> entry : query.getPropertyLike().entrySet()) {
                        String propertyName = entry.getKey();
                        String propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (currentValue == null) {
                                // Wrong value
                                return false;
                            }

                            if (currentValue instanceof Collection) {
                                boolean found = false;
                                for (String t : (Collection<String>) currentValue) {
                                    if (matchingLike(propertyValue, t)) {
                                        found = true;
                                    }
                                }
                                if (!found) {
                                    // Wrong value
                                    return false;
                                }
                            } else {
                                if (!matchingLike(propertyValue, (String) currentValue)) {
                                    // Wrong value
                                    return false;
                                }
                            }
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // Greater than
                    for (Entry<String, Object> entry : query.getPropertyGreater().entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (currentValue == null) {
                                // Wrong value
                                return false;
                            }
                            if (propertyValue instanceof Integer) {
                                return ((int) currentValue) > ((int) propertyValue);
                            }
                            if (propertyValue instanceof Long) {
                                return ((long) currentValue) > ((long) propertyValue);
                            }
                            if (propertyValue instanceof Float) {
                                return ((float) currentValue) > ((float) propertyValue);
                            }
                            if (propertyValue instanceof Double) {
                                return ((double) currentValue) > ((double) propertyValue);
                            }
                            if (propertyValue instanceof Date) {
                                return ((Date) currentValue).getTime() > ((Date) propertyValue).getTime();
                            }
                            if (propertyValue instanceof Enum) {
                                return ((Enum) currentValue).ordinal() > ((Enum) propertyValue).ordinal();
                            }
                            return false;
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // Greater and equals
                    for (Entry<String, Object> entry : query.getPropertyGreaterEquals().entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (currentValue == null) {
                                // Wrong value
                                return false;
                            }
                            if (propertyValue instanceof Integer) {
                                return ((int) currentValue) >= ((int) propertyValue);
                            }
                            if (propertyValue instanceof Long) {
                                return ((long) currentValue) >= ((long) propertyValue);
                            }
                            if (propertyValue instanceof Float) {
                                return ((float) currentValue) >= ((float) propertyValue);
                            }
                            if (propertyValue instanceof Double) {
                                return ((double) currentValue) >= ((double) propertyValue);
                            }
                            if (propertyValue instanceof Date) {
                                return ((Date) currentValue).getTime() >= ((Date) propertyValue).getTime();
                            }
                            if (propertyValue instanceof Enum) {
                                return ((Enum) currentValue).ordinal() >= ((Enum) propertyValue).ordinal();
                            }
                            return false;
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // Lesser than
                    for (Entry<String, Object> entry : query.getPropertyLesser().entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (currentValue == null) {
                                // Wrong value
                                return false;
                            }
                            if (propertyValue instanceof Integer) {
                                return ((int) currentValue) < ((int) propertyValue);
                            }
                            if (propertyValue instanceof Long) {
                                return ((long) currentValue) < ((long) propertyValue);
                            }
                            if (propertyValue instanceof Float) {
                                return ((float) currentValue) < ((float) propertyValue);
                            }
                            if (propertyValue instanceof Double) {
                                return ((double) currentValue) < ((double) propertyValue);
                            }
                            if (propertyValue instanceof Date) {
                                return ((Date) currentValue).getTime() < ((Date) propertyValue).getTime();
                            }
                            if (propertyValue instanceof Enum) {
                                return ((Enum) currentValue).ordinal() < ((Enum) propertyValue).ordinal();
                            }
                            return false;
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // Lesser and equals
                    for (Entry<String, Object> entry : query.getPropertyLesserAndEquals().entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (currentValue == null) {
                                // Wrong value
                                return false;
                            }
                            if (propertyValue instanceof Integer) {
                                return ((int) currentValue) <= ((int) propertyValue);
                            }
                            if (propertyValue instanceof Long) {
                                return ((long) currentValue) <= ((long) propertyValue);
                            }
                            if (propertyValue instanceof Float) {
                                return ((float) currentValue) <= ((float) propertyValue);
                            }
                            if (propertyValue instanceof Double) {
                                return ((double) currentValue) <= ((double) propertyValue);
                            }
                            if (propertyValue instanceof Date) {
                                return ((Date) currentValue).getTime() <= ((Date) propertyValue).getTime();
                            }
                            if (propertyValue instanceof Enum) {
                                return ((Enum) currentValue).ordinal() <= ((Enum) propertyValue).ordinal();
                            }
                            return false;
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // All the tags
                    Set<String> tags = tagFindAllByResource(resource);
                    for (String tag : query.getTagsAnd()) {
                        if (!tags.contains(tag)) {
                            // Missing tag
                            return false;
                        }
                    }
                    if (!query.getTagsOr().isEmpty()) {
                        boolean gotOne = false;
                        for (String tag : query.getTagsOr()) {
                            if (tags.contains(tag)) {
                                // Got one
                                gotOne = true;
                                break;
                            }
                        }
                        if (!gotOne) {
                            return false;
                        }
                    }

                    return true;
                }) //
                .map(it -> (R) it) //
                .collect(Collectors.toList());
        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends IPResource> Optional<R> resourceFindByPk(R resource) {
        return resources.stream().filter(it -> resourceEqualsPk(it, resource)).map(it -> clone((R) it)).findAny();
    }

    @Override
    public Set<String> tagFindAllByResource(IPResource resource) {
        if (resource.getInternalId() == null) {
            throw new ResourceNotFromRepositoryException(resource);
        }
        return tags.stream() //
                .filter(it -> resource.getInternalId().equals(it.getA())) //
                .map(it -> it.getB()) //
                .collect(Collectors.toSet());
    }

}
