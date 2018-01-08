/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
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
            Map<Long, List<Tuple3<IPResource, String, IPResource>>> deletedResourcePreviousLinksByResourceId, Set<Long> removedResourcesInThisTransaction) {

        logger.debug("State before applying changes. Has {} updates, {} deletions, {} addition", updatedResourcesPrevious.size(), deletedResources.size(), addedResources.size());
        logger.debug("[APPLY] Resources: has {} updates, {} deletions, {} addition ; Links: has {} deletions, {} addition ; Tags: has {} deletions, {} addition", //
                changes.getResourcesToUpdate().size(), changes.getResourcesToDelete().size(), changes.getResourcesToAdd().size(), //
                changes.getLinksToDelete().size(), changes.getLinksToAdd().size(), //
                changes.getTagsToDelete().size(), changes.getTagsToAdd().size() //
        );

        // Delete
        Set<Long> updatedIds = new HashSet<>();
        for (Long id : changes.getResourcesToDelete()) {

            if (removedResourcesInThisTransaction.add(id)) {
                logger.debug("[APPLY] Delete resource {}", id);
            } else {
                logger.debug("[APPLY] Delete resource {}. Already deleted in this transaction. Skipping", id);

                continue;
            }

            List<Tuple3<IPResource, String, IPResource>> deletedResourcePreviousLinks = new ArrayList<>();
            deletedResourcePreviousLinksByResourceId.put(id, deletedResourcePreviousLinks);

            IPResource resource = resourceFind(id).get();
            deletedResources.add(resource);
            deletedResourcePreviousLinks.addAll(linkFindAllRelatedByResource(id));
            Set<Long> idsToUpdate = new HashSet<>();
            idsToUpdate.addAll(linkFindAllByFromResource(resource).stream().map(it -> it.getB().getInternalId()).collect(Collectors.toList()));
            idsToUpdate.addAll(linkFindAllByToResource(resource).stream().map(it -> it.getA().getInternalId()).collect(Collectors.toList()));
            markAllTransientLinkedResourcesToUpdate(updatedIds, idsToUpdate);
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
                    markAllTransientLinkedResourcesToUpdate(updatedIds, Arrays.asList(fromId, toId));
                }
            }
        }
        for (Tuple2<IPResource, String> tag : changes.getTagsToDelete()) {
            logger.debug("[APPLY] Delete tag {}", tag);
            Optional<IPResource> resource = resourceFindByPk(tag.getA());
            if (resource.isPresent()) {
                Long id = resource.get().getInternalId();
                if (tags.removeIf(it -> id.equals(it.getA()) && tag.getB().equals(it.getB()))) {
                    updatedIds.add(id);
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

            // Add the direct links for update notification
            Set<Long> idsToUpdate = new HashSet<>();
            idsToUpdate.addAll(linkFindAllByFromResource(resource).stream().map(it -> it.getB().getInternalId()).collect(Collectors.toList()));
            idsToUpdate.addAll(linkFindAllByToResource(resource).stream().map(it -> it.getA().getInternalId()).collect(Collectors.toList()));
            markAllTransientLinkedResourcesToUpdate(updatedIds, idsToUpdate);

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
            if (!links.stream().filter( //
                    it -> fromId.equals(it.getA()) && //
                            link.getB().equals(it.getB()) && //
                            toId.equals(it.getC()) //
            ).findAny().isPresent()) {
                // Add
                links.add(new Tuple3<>(fromId, link.getB(), toId));
                markAllTransientLinkedResourcesToUpdate(updatedIds, Arrays.asList(fromId, toId));
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
            if (!tags.stream().filter(it -> id.equals(it.getA()) && tag.getB().equals(it.getB())).findAny().isPresent()) {
                // Add
                tags.add(new Tuple2<>(id, tag.getB()));
                updatedIds.add(id);
            }
        }
        updatedIds.removeAll(removedResourcesInThisTransaction);
        updatedResourcesPrevious.addAll(updatedIds.stream().map(it -> resourceFind(it).get()).collect(Collectors.toList()));

        Set<Long> updatedIdSeconds = new HashSet<>();
        // Update
        for (Tuple2<Long, IPResource> update : changes.getResourcesToUpdate()) {

            logger.debug("[APPLY] Update resource {}", update);

            // Get the previous resource
            Optional<IPResource> previousResourceOptional = resourceFind(update.getA());
            if (!previousResourceOptional.isPresent()) {
                throw new ResourceNotFoundException(update.getA());
            }
            IPResource previousResource = previousResourceOptional.get();
            if (updatedIds.add(previousResource.getInternalId())) {
                updatedResourcesPrevious.add(previousResource);
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
            resources.removeIf(it -> update.getA().equals(it.getInternalId()));
            resources.add(clone(updatedResource));

            // Add the direct links for update notification
            updatedResourcesPrevious.addAll(linkFindAllByFromResource(updatedResource).stream().map(it -> it.getB()).collect(Collectors.toList()));
            updatedResourcesPrevious.addAll(linkFindAllByToResource(updatedResource).stream().map(it -> it.getA()).collect(Collectors.toList()));
            // Add all the transient managed resources links for update notification
            markAllTransientLinkedResourcesToUpdate(updatedIdSeconds, Arrays.asList(updatedResource.getInternalId()));
        }

        updatedIdSeconds.removeAll(updatedIds);
        updatedResourcesPrevious.addAll(updatedIdSeconds.stream().map(it -> resourceFind(it).get()).collect(Collectors.toList()));

        // Cleanup lists
        removedResourcesInThisTransaction.addAll(deletedResources.stream().map(IPResource::getInternalId).collect(Collectors.toSet()));
        updatedResourcesPrevious.removeIf(it -> removedResourcesInThisTransaction.contains(it.getInternalId()));

        changes.clear();
        logger.debug("State after applying changes. Has {} updates, {} deletions, {} addition", updatedResourcesPrevious.size(), deletedResources.size(), addedResources.size());
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

        try {

            Queue<IPResource> addedResources = new LinkedBlockingQueue<>();
            Queue<IPResource> deletedResources = new LinkedBlockingQueue<>();
            Map<Long, List<Tuple3<IPResource, String, IPResource>>> deletedResourcePreviousLinksByResourceId = new HashMap<>();
            Queue<IPResource> updatedResourcesPrevious = new LinkedBlockingQueue<>();
            Set<Long> removedResourcesInThisTransaction = new HashSet<>();

            long globalLoopCount = 0;
            Map<Class<?>, List<UpdateEventContext>> updateEventContextsByResourceType = commonServicesContext.getPluginService().getUpdateEvents().stream() //
                    .collect(Collectors.groupingBy(it -> it.getUpdateEventHandler().supportedClass()));

            // Apply the changes
            applyChanges(changes, addedResources, updatedResourcesPrevious, deletedResources, deletedResourcePreviousLinksByResourceId, removedResourcesInThisTransaction);

            while ((!addedResources.isEmpty() || !updatedResourcesPrevious.isEmpty() || !deletedResources.isEmpty()) && globalLoopCount < 100) {
                ++globalLoopCount;

                logger.debug("Update events loop {}. Has {} updates, {} deletions, {} addition", globalLoopCount, updatedResourcesPrevious.size(), deletedResources.size(), addedResources.size());

                // Process all updates
                IPResource itemPrevious;
                while ((itemPrevious = updatedResourcesPrevious.poll()) != null) {
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
                            applyChanges(changes, addedResources, updatedResourcesPrevious, deletedResources, deletedResourcePreviousLinksByResourceId, removedResourcesInThisTransaction);
                        }
                    }
                }

                // Process all deletes
                IPResource item;
                while ((item = deletedResources.poll()) != null) {
                    List<UpdateEventContext> eventContexts = updateEventContextsByResourceType.get(item.getClass());
                    if (eventContexts != null) {
                        logger.debug("[UPDATE EVENT] Processing {} deleted handlers", eventContexts.size());
                        for (UpdateEventContext eventContext : eventContexts) {
                            logger.debug("[UPDATE EVENT] Processing {} deleted handler", eventContext.getUpdateHandlerName());
                            UpdateEventHandler updateEventHandler = eventContext.getUpdateEventHandler();
                            updateEventHandler.deleteHandler(commonServicesContext, changes, item, deletedResourcePreviousLinksByResourceId.get(item.getInternalId()));
                            applyChanges(changes, addedResources, updatedResourcesPrevious, deletedResources, deletedResourcePreviousLinksByResourceId, removedResourcesInThisTransaction);
                        }
                    }
                }

                // Process all adds
                while ((item = addedResources.poll()) != null) {
                    List<UpdateEventContext> eventContexts = updateEventContextsByResourceType.get(item.getClass());
                    if (eventContexts != null) {
                        logger.debug("[UPDATE EVENT] Processing {} added handlers", eventContexts.size());
                        for (UpdateEventContext eventContext : eventContexts) {
                            logger.debug("[UPDATE EVENT] Processing {} added handler", eventContext.getUpdateHandlerName());
                            UpdateEventHandler updateEventHandler = eventContext.getUpdateEventHandler();
                            updateEventHandler.addHandler(commonServicesContext, changes, item);
                            applyChanges(changes, addedResources, updatedResourcesPrevious, deletedResources, deletedResourcePreviousLinksByResourceId, removedResourcesInThisTransaction);
                        }
                    }
                }

                // Apply any pending changes
                applyChanges(changes, addedResources, updatedResourcesPrevious, deletedResources, deletedResourcePreviousLinksByResourceId, removedResourcesInThisTransaction);

            }

            if (!addedResources.isEmpty() || !updatedResourcesPrevious.isEmpty() || !deletedResources.isEmpty()) {
                throw new InfiniteUpdateLoop("Iterated " + globalLoopCount + " times and there are always changes");
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
        IPResourceDefinition resourceDefinition = resourceDefinitionByResourceClass.get(resourceClass);
        if (resourceDefinition == null) {
            throw new SmallToolsException("Resource class " + resourceClass.getName() + " is unknown");
        }
        return new IPResourceQuery<>(resourceDefinition);
    }

    @Override
    public <T extends IPResource> IPResourceQuery<T> createResourceQuery(String resourceType) {
        IPResourceDefinition resourceDefinition = resourceDefinitionByResourceType.get(resourceType);
        if (resourceDefinition == null) {
            throw new SmallToolsException("Resource type " + resourceType + " is unknown");
        }
        return new IPResourceQuery<>(resourceDefinition);
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

    private void markAllTransientLinkedResourcesToUpdate(Set<Long> updatedIds, Collection<Long> ids) {
        Set<Long> transientProcessedIds = new HashSet<>();
        markAllTransientLinkedResourcesToUpdate(updatedIds, transientProcessedIds, ids);
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

                    IPResourceDefinition resourceDefinition = query.getResourceDefinition();

                    // Right type
                    if (!resource.getClass().equals(resourceDefinition.getResourceClass())) {
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
                    for (Entry<String, Object> entry : query.getPropertyEquals().entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        try {
                            Object currentValue = resourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
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
                            Object currentValue = resourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
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
                            Object currentValue = resourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
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
                            Object currentValue = resourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
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
                            Object currentValue = resourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
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
                            Object currentValue = resourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
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
                            Object currentValue = resourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
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
