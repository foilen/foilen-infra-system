/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.common.changeexecution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.foilen.infra.plugin.core.system.common.changeexecution.hooks.ChangeExecutionHook;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.UpdateEventContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.UpdateEventHandler;
import com.foilen.infra.plugin.v1.core.exception.InfiniteUpdateLoop;
import com.foilen.infra.plugin.v1.core.exception.ResourceNotFoundException;
import com.foilen.infra.plugin.v1.core.exception.ResourcePrimaryKeyCollisionException;
import com.foilen.infra.plugin.v1.core.service.IPPluginService;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.JavaEnvironmentValues;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.CollectionsTools;
import com.foilen.smalltools.tools.SecureRandomTools;
import com.foilen.smalltools.tools.ThreadNameStateTool;
import com.foilen.smalltools.tools.ThreadTools;
import com.foilen.smalltools.tools.TimeExecutionTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.foilen.smalltools.tuple.Tuple3;
import com.google.common.base.Joiner;

public class ChangeExecutionLogic extends AbstractBasics {

    private static final AtomicLong txCounter = new AtomicLong();
    private static final String baseTxId = JavaEnvironmentValues.getHostName() + "/" + SecureRandomTools.randomHexString(5) + "/";

    // Services
    private CommonServicesContext commonServicesContext;
    private IPPluginService ipPluginService;
    private IPResourceService ipResourceService;
    private InternalChangeService internalChangeService;

    // Properties
    private List<ChangeExecutionHook> hooks = new ArrayList<>();
    private long infiniteLoopTimeoutInMs = 15000;

    public ChangeExecutionLogic(CommonServicesContext commonServicesContext, InternalServicesContext internalServicesContext) {
        this.commonServicesContext = commonServicesContext;
        this.ipPluginService = commonServicesContext.getPluginService();
        this.ipResourceService = commonServicesContext.getResourceService();
        this.internalChangeService = internalServicesContext.getInternalChangeService();
    }

    public void addHook(ChangeExecutionHook hook) {
        hooks.add(hook);
    }

    private void applyChanges(ApplyChangesContext applyChangesContext, ChangesContext changes) {

        logger.debug("State before applying changes. Has {}", applyChangesContext.toQueueInformation());
        logger.debug("[APPLY] Resources: has {} updates, {} deletions, {} addition, {} refreshes ; Links: has {} deletions, {} addition ; Tags: has {} deletions, {} addition", //
                changes.getResourcesToUpdate().size(), changes.getResourcesToDelete().size(), changes.getResourcesToAdd().size(), changes.getResourcesToRefresh().size(), //
                changes.getLinksToDelete().size(), changes.getLinksToAdd().size(), //
                changes.getTagsToDelete().size(), changes.getTagsToAdd().size() //
        );

        // Delete
        Set<Long> toRefreshDirectIds = new HashSet<>();
        Set<Long> toRefreshFarIds = new HashSet<>();
        for (Long id : changes.getResourcesToDelete()) {

            if (applyChangesContext.getRemovedResourcesInThisTransaction().add(id)) {
                logger.debug("[APPLY] Delete resource {}", id);
            } else {
                logger.debug("[APPLY-SKIP] Delete resource {}. Already deleted in this transaction. Skipping", id);

                continue;
            }

            List<Tuple3<IPResource, String, IPResource>> deletedResourcePreviousLinks = new ArrayList<>();
            applyChangesContext.getDeletedResourcePreviousLinksByResourceId().put(id, deletedResourcePreviousLinks);

            IPResource resource = ipResourceService.resourceFind(id).get();
            CollectionsTools.getOrCreateEmpty(applyChangesContext.getUpdateCountByResourceId(), resource.getClass().getSimpleName() + " / " + resource.getResourceName(), AtomicInteger.class)
                    .incrementAndGet();
            hooks.forEach(h -> h.resourceDeleted(applyChangesContext, resource));

            applyChangesContext.getDeletedResources().add(resource);
            applyChangesContext.getResourcesNeedRefresh().remove(id);
            applyChangesContext.getUpdatedDirectCheck().remove(id);
            applyChangesContext.getUpdatedFarCheck().remove(id);
            deletedResourcePreviousLinks.addAll(ipResourceService.linkFindAllRelatedByResource(id));
            Set<Long> idsToUpdate = deletedResourcePreviousLinks.stream().map(link -> {
                if (link.getA().getInternalId() != id) {
                    return link.getA().getInternalId();
                } else {
                    return link.getC().getInternalId();
                }
            }).collect(Collectors.toSet());
            toRefreshDirectIds.addAll(idsToUpdate);
            markAllTransientLinkedResourcesToUpdate(toRefreshFarIds, idsToUpdate);
            internalChangeService.resourceDelete(id);
        }
        for (

        Tuple3<IPResource, String, IPResource> link : changes.getLinksToDelete()) {
            logger.debug("[APPLY] Delete link {}", link);
            Optional<IPResource> fromResource = ipResourceService.resourceFindByPk(link.getA());
            Optional<IPResource> toResource = ipResourceService.resourceFindByPk(link.getC());
            if (fromResource.isPresent() && toResource.isPresent()) {
                Long fromId = fromResource.get().getInternalId();
                Long toId = toResource.get().getInternalId();
                String linkType = link.getB();
                if (internalChangeService.linkDelete(fromId, linkType, toId)) {
                    hooks.forEach(h -> h.linkDeleted(applyChangesContext, fromResource.get(), linkType, toResource.get()));
                    toRefreshDirectIds.add(fromId);
                    toRefreshDirectIds.add(toId);
                    markAllTransientLinkedResourcesToUpdate(toRefreshFarIds, Arrays.asList(fromId, toId));
                } else {
                    logger.debug("[APPLY-SKIP] Delete link {}. Skipped since does not exists", link);
                }
            }
        }
        for (Tuple2<IPResource, String> tag : changes.getTagsToDelete()) {
            logger.debug("[APPLY] Delete tag {}", tag);
            Optional<IPResource> resource = ipResourceService.resourceFindByPk(tag.getA());
            if (resource.isPresent()) {
                Long internalId = resource.get().getInternalId();
                String tagName = tag.getB();
                if (internalChangeService.tagDelete(internalId, tagName)) {
                    hooks.forEach(h -> h.tagDeleted(applyChangesContext, resource.get(), tagName));
                    toRefreshDirectIds.add(internalId);
                } else {
                    logger.debug("[APPLY-SKIP] Delete tag {}. Skipped since does not exists", tag);
                }
            }
        }

        // Add
        for (IPResource resource : changes.getResourcesToAdd()) {
            logger.debug("[APPLY] Add resource {}", resource);
            // Check if already exists
            if (ipResourceService.resourceFindByPk(resource).isPresent()) {
                throw new ResourcePrimaryKeyCollisionException(resource);
            }

            CollectionsTools.getOrCreateEmpty(applyChangesContext.getUpdateCountByResourceId(), resource.getClass().getSimpleName() + " / " + resource.getResourceName(), AtomicInteger.class)
                    .incrementAndGet();

            hooks.forEach(h -> h.resourceAdded(applyChangesContext, resource));

            IPResource addedResource = internalChangeService.resourceAdd(resource);
            applyChangesContext.getAddedResources().add(addedResource);
            resource.setInternalId(addedResource.getInternalId());
            applyChangesContext.getResourcesNeedRefresh().remove(resource.getInternalId());
            applyChangesContext.getUpdatedDirectCheck().remove(resource.getInternalId());
            applyChangesContext.getUpdatedFarCheck().remove(resource.getInternalId());

            // Add the direct links for update notification
            Set<Long> idsToUpdate = ipResourceService.linkFindAllRelatedByResource(resource).stream().map(link -> {
                if (link.getA().getInternalId() != resource.getInternalId()) {
                    return link.getA().getInternalId();
                } else {
                    return link.getC().getInternalId();
                }
            }).collect(Collectors.toSet());
            toRefreshDirectIds.addAll(idsToUpdate);
            markAllTransientLinkedResourcesToUpdate(toRefreshFarIds, idsToUpdate);

        }
        for (Tuple3<IPResource, String, IPResource> link : changes.getLinksToAdd()) {
            logger.debug("[APPLY] Add link {}", link);
            Optional<IPResource> fromResource = ipResourceService.resourceFindByPk(link.getA());
            if (!fromResource.isPresent()) {
                throw new ResourceNotFoundException(link.getA());
            }
            Optional<IPResource> toResource = ipResourceService.resourceFindByPk(link.getC());
            if (!toResource.isPresent()) {
                throw new ResourceNotFoundException(link.getC());
            }

            Long fromId = fromResource.get().getInternalId();
            Long toId = toResource.get().getInternalId();
            // Add if not present
            String linkType = link.getB();
            if (!internalChangeService.linkExists(fromId, linkType, toId)) {
                // Add
                internalChangeService.linkAdd(fromId, linkType, toId);
                hooks.forEach(h -> h.linkAdded(applyChangesContext, fromResource.get(), linkType, toResource.get()));
                toRefreshDirectIds.add(fromId);
                toRefreshDirectIds.add(toId);
                markAllTransientLinkedResourcesToUpdate(toRefreshFarIds, Arrays.asList(fromId, toId));
            } else {
                logger.debug("[APPLY-SKIP] Add link {}. Skipped since does not exists", link);
            }

        }
        for (Tuple2<IPResource, String> tag : changes.getTagsToAdd()) {
            logger.debug("[APPLY] Add tag {}", tag);
            Optional<IPResource> resource = ipResourceService.resourceFindByPk(tag.getA());
            if (!resource.isPresent()) {
                throw new ResourceNotFoundException(tag.getA());
            }

            Long pluginResourceId = resource.get().getInternalId();
            // Add if not present
            String tagName = tag.getB();
            if (!internalChangeService.tagExists(pluginResourceId, tagName)) {
                // Add
                internalChangeService.tagAdd(pluginResourceId, tagName);
                hooks.forEach(h -> h.tagAdded(applyChangesContext, resource.get(), tagName));
                toRefreshDirectIds.add(pluginResourceId);
            } else {
                logger.debug("[APPLY-SKIP] Add tag {}. Skipped since does not exists", tag);
            }
        }
        toRefreshDirectIds.removeAll(applyChangesContext.getRemovedResourcesInThisTransaction());
        toRefreshFarIds.removeAll(applyChangesContext.getRemovedResourcesInThisTransaction());
        toRefreshDirectIds.forEach(toRefreshId -> {
            if (idNotInAnyQueues(toRefreshId, applyChangesContext.getAddedResources(), applyChangesContext.getUpdatedResourcesPrevious(), applyChangesContext.getDeletedResources())) {
                applyChangesContext.getUpdatedDirectCheck().add(toRefreshId);
            }
        });
        toRefreshFarIds.forEach(toRefreshId -> {
            if (idNotInAnyQueues(toRefreshId, applyChangesContext.getAddedResources(), applyChangesContext.getUpdatedResourcesPrevious(), applyChangesContext.getDeletedResources())) {
                applyChangesContext.getUpdatedFarCheck().add(toRefreshId);
            }
        });

        Set<Long> toRefreshDirectIdsSeconds = new HashSet<>();
        Set<Long> toRefreshFarIdsSeconds = new HashSet<>();
        // Update
        for (Tuple2<Long, IPResource> update : changes.getResourcesToUpdate()) {

            logger.debug("[APPLY] Update resource {}", update);

            // Get the previous resource
            Optional<IPResource> previousResourceOptional = ipResourceService.resourceFind(update.getA());
            if (!previousResourceOptional.isPresent()) {
                throw new ResourceNotFoundException(update.getA());
            }
            IPResource previousResource = previousResourceOptional.get();
            // Add if not already in the list
            if (!applyChangesContext.getUpdatedResourcesPrevious().stream().filter(it -> previousResource.getInternalId().equals(it.getInternalId())).findAny().isPresent()) {
                applyChangesContext.getUpdatedResourcesPrevious().add(previousResource);
                applyChangesContext.getResourcesNeedRefresh().remove(previousResource.getInternalId());
                applyChangesContext.getUpdatedDirectCheck().remove(previousResource.getInternalId());
                applyChangesContext.getUpdatedFarCheck().remove(previousResource.getInternalId());
            }

            // Get the next resource (might not exists)
            IPResource updatedResource = update.getB();
            Optional<IPResource> nextResourceOptional = ipResourceService.resourceFindByPk(updatedResource);
            if (nextResourceOptional.isPresent()) {
                // Check if not the same resource
                IPResource nextResource = nextResourceOptional.get();
                if (!previousResource.getInternalId().equals(nextResource.getInternalId())) {
                    throw new ResourcePrimaryKeyCollisionException();
                }
            }

            // Update the resource
            CollectionsTools
                    .getOrCreateEmpty(applyChangesContext.getUpdateCountByResourceId(), previousResource.getClass().getSimpleName() + " / " + previousResource.getResourceName(), AtomicInteger.class)
                    .incrementAndGet();
            updatedResource.setInternalId(update.getA());
            internalChangeService.resourceUpdate(previousResource, updatedResource);
            hooks.forEach(h -> h.resourceUpdated(applyChangesContext, previousResource, updatedResource));

            // Add the direct links for update notification
            Set<Long> idsToUpdate = ipResourceService.linkFindAllRelatedByResource(updatedResource).stream().map(link -> {
                if (link.getA().getInternalId() != updatedResource.getInternalId()) {
                    return link.getA().getInternalId();
                } else {
                    return link.getC().getInternalId();
                }
            }).collect(Collectors.toSet());
            toRefreshDirectIds.addAll(idsToUpdate);
            markAllTransientLinkedResourcesToUpdate(toRefreshFarIds, idsToUpdate);

            // Add all the transient managed resources links for update notification
            markAllTransientLinkedResourcesToUpdate(toRefreshFarIdsSeconds, Arrays.asList(updatedResource.getInternalId()));
        }

        // Refreshes
        for (Long id : changes.getResourcesToRefresh()) {
            logger.debug("[APPLY] Refresh resource {}", id);
            if (idNotInAnyQueues(id, applyChangesContext.getAddedResources(), applyChangesContext.getUpdatedResourcesPrevious(), applyChangesContext.getDeletedResources())) {
                applyChangesContext.getResourcesNeedRefresh().add(id);
            } else {
                logger.debug("[APPLY-SKIP] Refresh resource {}. Already waiting for a refresh in any category", id);
            }
        }

        toRefreshDirectIdsSeconds.removeAll(toRefreshDirectIds);
        toRefreshFarIdsSeconds.removeAll(toRefreshFarIds);
        toRefreshDirectIdsSeconds.forEach(toRefreshId -> {
            if (idNotInAnyQueues(toRefreshId, applyChangesContext.getAddedResources(), applyChangesContext.getUpdatedResourcesPrevious(), applyChangesContext.getDeletedResources())) {
                applyChangesContext.getUpdatedDirectCheck().add(toRefreshId);
            }
        });
        toRefreshFarIdsSeconds.forEach(toRefreshId -> {
            if (idNotInAnyQueues(toRefreshId, applyChangesContext.getAddedResources(), applyChangesContext.getUpdatedResourcesPrevious(), applyChangesContext.getDeletedResources())) {
                applyChangesContext.getUpdatedFarCheck().add(toRefreshId);
            }
        });

        // Cleanup lists
        applyChangesContext.getRemovedResourcesInThisTransaction().addAll(applyChangesContext.getDeletedResources().stream().map(IPResource::getInternalId).collect(Collectors.toSet()));
        applyChangesContext.getUpdatedResourcesPrevious().removeIf(it -> applyChangesContext.getRemovedResourcesInThisTransaction().contains(it.getInternalId()));
        applyChangesContext.getUpdatedFarCheck().removeAll(applyChangesContext.getUpdatedDirectCheck());

        changes.clear();
        logger.debug("State after applying changes. Has {} ", applyChangesContext.toQueueInformation());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void execute(ChangesContext changes) {

        // Prepare context
        ApplyChangesContext applyChangesContext = new ApplyChangesContext();
        applyChangesContext.setTxId(baseTxId + txCounter.getAndIncrement());
        applyChangesContext.setExplicitChange(true);
        hooks.forEach(h -> h.fillApplyChangesContext(applyChangesContext));

        // Set thread name
        ThreadNameStateTool threadNameStateTool = ThreadTools.nameThread() //
                .appendText(" - txId:") //
                .appendText(applyChangesContext.getTxId()) //
                .appendText(" - user:") //
                .appendText(applyChangesContext.getUserType().name());

        if (applyChangesContext.getUserName() != null) {
            threadNameStateTool.appendText("/") //
                    .appendText(applyChangesContext.getUserName());
        }

        threadNameStateTool.change();

        logger.info("----- [changesExecute] Begin -----");

        long maxTime = System.currentTimeMillis() + infiniteLoopTimeoutInMs;

        try {

            Map<Class<?>, List<UpdateEventContext>> updateEventContextsByResourceType = ipPluginService.getUpdateEvents().stream() //
                    .collect(Collectors.groupingBy(it -> it.getUpdateEventHandler().supportedClass()));

            // Apply the changes
            applyChanges(applyChangesContext, changes);
            applyChangesContext.setExplicitChange(false);

            while (System.currentTimeMillis() < maxTime && (applyChangesContext.hasChangesInQueues())) {

                logger.debug("Update events loop. Has {}", applyChangesContext.toQueueInformation());

                // Process all updates
                IPResource itemPrevious;
                while (System.currentTimeMillis() < maxTime && (itemPrevious = applyChangesContext.getUpdatedResourcesPrevious().poll()) != null) {
                    Optional<IPResource> currentResourceOptional = ipResourceService.resourceFind(itemPrevious.getInternalId());
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
                            IPResource finalItemPrevious = itemPrevious;
                            long time = TimeExecutionTools.measureInMs(() -> {
                                updateEventHandler.updateHandler(commonServicesContext, changes, finalItemPrevious, currentResource);
                                applyChanges(applyChangesContext, changes);
                            });
                            applyChangesContext.addExecutionTime(updateEventHandler, time);

                        }
                    }
                }

                // Process all deletes
                IPResource item;
                while (System.currentTimeMillis() < maxTime && (item = applyChangesContext.getDeletedResources().poll()) != null) {
                    List<UpdateEventContext> eventContexts = updateEventContextsByResourceType.get(item.getClass());
                    if (eventContexts != null) {
                        logger.debug("[UPDATE EVENT] Processing {} deleted handlers", eventContexts.size());
                        for (UpdateEventContext eventContext : eventContexts) {
                            logger.debug("[UPDATE EVENT] Processing {} deleted handler", eventContext.getUpdateHandlerName());
                            UpdateEventHandler updateEventHandler = eventContext.getUpdateEventHandler();
                            IPResource finalItem = item;
                            long time = TimeExecutionTools.measureInMs(() -> {
                                updateEventHandler.deleteHandler(commonServicesContext, changes, finalItem,
                                        applyChangesContext.getDeletedResourcePreviousLinksByResourceId().get(finalItem.getInternalId()));
                                applyChanges(applyChangesContext, changes);
                            });
                            applyChangesContext.addExecutionTime(updateEventHandler, time);
                        }
                    }
                }

                // Process all adds
                while (System.currentTimeMillis() < maxTime && (item = applyChangesContext.getAddedResources().poll()) != null) {
                    List<UpdateEventContext> eventContexts = updateEventContextsByResourceType.get(item.getClass());
                    if (eventContexts != null) {
                        logger.debug("[UPDATE EVENT] Processing {} added handlers", eventContexts.size());
                        for (UpdateEventContext eventContext : eventContexts) {
                            logger.debug("[UPDATE EVENT] Processing {} added handler", eventContext.getUpdateHandlerName());
                            UpdateEventHandler updateEventHandler = eventContext.getUpdateEventHandler();
                            IPResource finalItem = item;
                            long time = TimeExecutionTools.measureInMs(() -> {
                                updateEventHandler.addHandler(commonServicesContext, changes, finalItem);
                                applyChanges(applyChangesContext, changes);
                            });
                            applyChangesContext.addExecutionTime(updateEventHandler, time);
                        }
                    }
                }

                // Process all refreshes
                Long id;
                while (System.currentTimeMillis() < maxTime && (id = applyChangesContext.getResourcesNeedRefresh().poll()) != null) {
                    Optional<IPResource> optionalResource = ipResourceService.resourceFind(id);
                    if (optionalResource.isPresent()) {
                        item = optionalResource.get();
                        List<UpdateEventContext> eventContexts = updateEventContextsByResourceType.get(item.getClass());
                        if (eventContexts != null) {
                            logger.debug("[UPDATE EVENT] Processing {} refresh handlers", eventContexts.size());
                            for (UpdateEventContext eventContext : eventContexts) {
                                logger.debug("[UPDATE EVENT] Processing {} refresh handler", eventContext.getUpdateHandlerName());
                                UpdateEventHandler updateEventHandler = eventContext.getUpdateEventHandler();
                                IPResource finalItem = item;
                                long time = TimeExecutionTools.measureInMs(() -> {
                                    updateEventHandler.checkAndFix(commonServicesContext, changes, finalItem);
                                    applyChanges(applyChangesContext, changes);
                                });
                                applyChangesContext.addExecutionTime(updateEventHandler, time);
                            }
                        }
                    }
                }

                // Refresh for direct
                while (System.currentTimeMillis() < maxTime && (id = applyChangesContext.getUpdatedDirectCheck().poll()) != null) {
                    Optional<IPResource> optionalResource = ipResourceService.resourceFind(id);
                    if (optionalResource.isPresent()) {
                        item = optionalResource.get();
                        List<UpdateEventContext> eventContexts = updateEventContextsByResourceType.get(item.getClass());
                        if (eventContexts != null) {
                            logger.debug("[UPDATE EVENT] Processing {} updated direct check handlers", eventContexts.size());
                            for (UpdateEventContext eventContext : eventContexts) {
                                logger.debug("[UPDATE EVENT] Processing {} updated direct check handlers", eventContext.getUpdateHandlerName());
                                UpdateEventHandler updateEventHandler = eventContext.getUpdateEventHandler();
                                IPResource finalItem = item;
                                long time = TimeExecutionTools.measureInMs(() -> {
                                    updateEventHandler.checkDirectLinkChanged(commonServicesContext, changes, finalItem);
                                    applyChanges(applyChangesContext, changes);
                                });
                                applyChangesContext.addUpdateDirectCheck(updateEventHandler);
                                applyChangesContext.addExecutionTime(updateEventHandler, time);
                            }
                        }
                    }
                }

                // Refresh for far
                while (System.currentTimeMillis() < maxTime && (id = applyChangesContext.getUpdatedFarCheck().poll()) != null) {
                    Optional<IPResource> optionalResource = ipResourceService.resourceFind(id);
                    if (optionalResource.isPresent()) {
                        item = optionalResource.get();
                        List<UpdateEventContext> eventContexts = updateEventContextsByResourceType.get(item.getClass());
                        if (eventContexts != null) {
                            logger.debug("[UPDATE EVENT] Processing {} updated far check handlers", eventContexts.size());
                            for (UpdateEventContext eventContext : eventContexts) {
                                logger.debug("[UPDATE EVENT] Processing {} updated far check handlers", eventContext.getUpdateHandlerName());
                                UpdateEventHandler updateEventHandler = eventContext.getUpdateEventHandler();
                                IPResource finalItem = item;
                                long time = TimeExecutionTools.measureInMs(() -> {
                                    updateEventHandler.checkFarLinkChanged(commonServicesContext, changes, finalItem);
                                    applyChanges(applyChangesContext, changes);
                                });
                                applyChangesContext.addUpdateFarCheck(updateEventHandler);
                                applyChangesContext.addExecutionTime(updateEventHandler, time);
                            }
                        }
                    }
                }

                // Apply any pending changes
                if (System.currentTimeMillis() < maxTime) {
                    applyChanges(applyChangesContext, changes);
                }

            }

            if (applyChangesContext.hasChangesInQueues()) {
                // InfiniteUpdateLoop Display report
                logger.error("Iterated for too long and there are always changes");
                hooks.forEach(h -> h.failureInfinite(applyChangesContext));
                logger.info("Report Update count: {}", Joiner.on(", ").join(applyChangesContext.generateTop10UpdateCountReport()));
                logger.info("Report Event Handler execution time: {}", Joiner.on(", ").join(applyChangesContext.generateTop10UpdateEventHandlerExecutionTimeReport()));
                logger.info("Report Event Handler update direct count: {}", Joiner.on(", ").join(applyChangesContext.generateTop10UpdateDirectCheckByUpdateHandlerReport()));
                logger.info("Report Event Handler update far count: {}", Joiner.on(", ").join(applyChangesContext.generateTop10UpdateFarCheckByUpdateHandlerReport()));
                throw new InfiniteUpdateLoop("Iterated for too long and there are always changes");
            }

            // Show reports
            hooks.forEach(h -> h.success(applyChangesContext));
            logger.info("Report Update count: {}", Joiner.on(", ").join(applyChangesContext.generateTop10UpdateCountReport()));
            logger.info("Report Event Handler execution time: {}", Joiner.on(", ").join(applyChangesContext.generateTop10UpdateEventHandlerExecutionTimeReport()));
            logger.info("Report Event Handler update direct count: {}", Joiner.on(", ").join(applyChangesContext.generateTop10UpdateDirectCheckByUpdateHandlerReport()));
            logger.info("Report Event Handler update far count: {}", Joiner.on(", ").join(applyChangesContext.generateTop10UpdateFarCheckByUpdateHandlerReport()));

            // Complete the transaction
            logger.info("===== [changesExecute] Completed =====");

        } catch (RuntimeException e) {
            // Rollback the transaction
            logger.error("===== [changesExecute] Problem while executing the changes. Rolling back transaction =====", e);
            throw e;
        } finally {
            threadNameStateTool.revert();
        }

    }

    public long getInfiniteLoopTimeoutInMs() {
        return infiniteLoopTimeoutInMs;
    }

    private boolean idNotInAnyQueues(Long id, Queue<IPResource> addedResources, Queue<IPResource> updatedResourcesPrevious, Queue<IPResource> deletedResources) {
        return !addedResources.stream().filter(it -> id.equals(it.getInternalId())).findAny().isPresent() //
                && !updatedResourcesPrevious.stream().filter(it -> id.equals(it.getInternalId())).findAny().isPresent() //
                && !deletedResources.stream().filter(it -> id.equals(it.getInternalId())).findAny().isPresent();
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

            Set<Long> linkedIds = ipResourceService.linkFindAllRelatedByResource(id).stream().map(link -> {
                if (link.getA().getInternalId() != id) {
                    return link.getA().getInternalId();
                } else {
                    return link.getC().getInternalId();
                }
            }).collect(Collectors.toSet());

            markAllTransientLinkedResourcesToUpdate(updatedIds, transientProcessedIds, linkedIds);
        }

    }

    public void setInfiniteLoopTimeoutInMs(long infiniteLoopTimeoutInMs) {
        this.infiniteLoopTimeoutInMs = infiniteLoopTimeoutInMs;
    }

}
