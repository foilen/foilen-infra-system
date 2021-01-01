/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.common.changeexecution;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.ActionHandler;
import com.foilen.infra.plugin.v1.core.eventhandler.changes.ChangeExecutionHook;
import com.foilen.infra.plugin.v1.core.eventhandler.changes.ChangesInTransactionContext;
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
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.SecureRandomTools;
import com.foilen.smalltools.tools.StringTools;
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

    private void applyChanges(ChangesInTransactionContext changesInTransactionContext, ChangesContext changes) {

        logger.debug("[APPLY] Resources: has {} updates, {} deletions, {} additions, {} refreshes; Links: has {} deletions, {} addition ; Tags: has {} deletions, {} addition", //
                changes.getResourcesToUpdate().size(), changes.getResourcesToDelete().size(), changes.getResourcesToAdd().size(), changes.getResourcesToRefresh().size(), //
                changes.getLinksToDelete().size(), changes.getLinksToAdd().size(), //
                changes.getTagsToDelete().size(), changes.getTagsToAdd().size() //
        );

        // Mark refreshed
        for (String id : changes.getResourcesToRefresh()) {
            Optional<IPResource> resourceO = ipResourceService.resourceFind(id);
            if (resourceO.isPresent()) {
                logger.debug("[APPLY] Refresh resource {}", id);
            } else {
                logger.debug("[APPLY-SKIP] Refresh resource {}. Does not exist. Skipping", id);
                continue;
            }

            // Mark it
            changesInTransactionContext.addRefreshedResource(resourceO.get());
        }

        // Delete
        for (String id : changes.getResourcesToDelete()) {

            Optional<IPResource> resourceO = ipResourceService.resourceFind(id);
            if (resourceO.isPresent()) {
                logger.debug("[APPLY] Delete resource {}", id);
            } else {
                logger.debug("[APPLY-SKIP] Delete resource {}. Already deleted in this transaction. Skipping", id);
                continue;
            }

            // The resource
            IPResource resource = resourceO.get();
            CollectionsTools.getOrCreateEmpty(changesInTransactionContext.getUpdateCountByResourceId(), resource.getClass().getSimpleName() + " / " + resource.getResourceName(), AtomicInteger.class)
                    .incrementAndGet();
            hooks.forEach(h -> h.resourceDeleted(changesInTransactionContext, resource));
            changesInTransactionContext.addDeletedResource(resource);

            // The links
            changesInTransactionContext.addDeletedLinks(ipResourceService.linkFindAllRelatedByResource(id));

            // The tags
            changesInTransactionContext.addDeletedTags(resource, ipResourceService.tagFindAllByResource(resource));

            // Execution
            internalChangeService.resourceDelete(id);
        }
        for (Tuple3<IPResource, String, IPResource> link : changes.getLinksToDelete()) {
            logger.debug("[APPLY] Delete link {}", link);
            Optional<IPResource> fromResource = ipResourceService.resourceFindByPk(link.getA());
            Optional<IPResource> toResource = ipResourceService.resourceFindByPk(link.getC());
            if (fromResource.isPresent() && toResource.isPresent()) {
                String fromId = fromResource.get().getInternalId();
                String toId = toResource.get().getInternalId();
                String linkType = link.getB();
                if (internalChangeService.linkDelete(fromId, linkType, toId)) {
                    hooks.forEach(h -> h.linkDeleted(changesInTransactionContext, fromResource.get(), linkType, toResource.get()));
                    changesInTransactionContext.addDeletedLink(new Tuple3<IPResource, String, IPResource>(fromResource.get(), linkType, toResource.get()));
                } else {
                    logger.debug("[APPLY-SKIP] Delete link {}. Skipped since does not exists", link);
                }
            }
        }
        for (Tuple2<IPResource, String> tag : changes.getTagsToDelete()) {
            logger.debug("[APPLY] Delete tag {}", tag);
            Optional<IPResource> resource = ipResourceService.resourceFindByPk(tag.getA());
            if (resource.isPresent()) {
                String internalId = resource.get().getInternalId();
                String tagName = tag.getB();
                if (internalChangeService.tagDelete(internalId, tagName)) {
                    hooks.forEach(h -> h.tagDeleted(changesInTransactionContext, resource.get(), tagName));
                    changesInTransactionContext.addDeletedTag(resource.get(), tagName);
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

            CollectionsTools.getOrCreateEmpty(changesInTransactionContext.getUpdateCountByResourceId(), resource.getClass().getSimpleName() + " / " + resource.getResourceName(), AtomicInteger.class)
                    .incrementAndGet();

            hooks.forEach(h -> h.resourceAdded(changesInTransactionContext, resource));

            IPResource addedResource = internalChangeService.resourceAdd(resource);
            resource.setInternalId(addedResource.getInternalId());

            changesInTransactionContext.addAddedResource(addedResource);
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

            String fromId = fromResource.get().getInternalId();
            String toId = toResource.get().getInternalId();
            // Add if not present
            String linkType = link.getB();
            if (internalChangeService.linkExists(fromId, linkType, toId)) {
                logger.debug("[APPLY-SKIP] Add link {}. Skipped since does not exists", link);
            } else {
                // Add
                internalChangeService.linkAdd(fromId, linkType, toId);
                hooks.forEach(h -> h.linkAdded(changesInTransactionContext, fromResource.get(), linkType, toResource.get()));
                changesInTransactionContext.addAddedLink(link);
            }
        }
        for (Tuple2<IPResource, String> tag : changes.getTagsToAdd()) {
            logger.debug("[APPLY] Add tag {}", tag);
            Optional<IPResource> resource = ipResourceService.resourceFindByPk(tag.getA());
            if (!resource.isPresent()) {
                throw new ResourceNotFoundException(tag.getA());
            }

            String pluginResourceId = resource.get().getInternalId();
            // Add if not present
            String tagName = tag.getB();
            if (internalChangeService.tagExists(pluginResourceId, tagName)) {
                logger.debug("[APPLY-SKIP] Add tag {}. Skipped since does not exists", tag);
            } else {
                // Add
                internalChangeService.tagAdd(pluginResourceId, tagName);
                hooks.forEach(h -> h.tagAdded(changesInTransactionContext, resource.get(), tagName));
                changesInTransactionContext.addAddedTag(resource.get(), tagName);
            }
        }

        // Update
        for (Tuple2<String, IPResource> update : changes.getResourcesToUpdate()) {

            logger.debug("[APPLY] Update resource {}", update);

            // Get the previous resource
            Optional<IPResource> previousResourceOptional = ipResourceService.resourceFind(update.getA());
            if (!previousResourceOptional.isPresent()) {
                throw new ResourceNotFoundException(update.getA());
            }
            IPResource previousResource = previousResourceOptional.get();

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
            CollectionsTools.getOrCreateEmpty(changesInTransactionContext.getUpdateCountByResourceId(), previousResource.getClass().getSimpleName() + " / " + previousResource.getResourceName(),
                    AtomicInteger.class).incrementAndGet();
            updatedResource.setInternalId(update.getA());
            // check if really different
            if (StringTools.safeEquals(JsonTools.compactPrintWithoutNulls(previousResource), JsonTools.compactPrintWithoutNulls(updatedResource))) {
                logger.debug("[APPLY] Updated resource {} didn't really change", update);
            } else {
                internalChangeService.resourceUpdate(previousResource, updatedResource);
                hooks.forEach(h -> h.resourceUpdated(changesInTransactionContext, previousResource, updatedResource));
                changesInTransactionContext.addUpdatedResource(previousResource, updatedResource);
            }

        }

        changes.clear();
    }

    public void execute(ChangesContext changes) {

        // Prepare contexts
        ChangesInTransactionContext changesInTransactionContext = new ChangesInTransactionContext();
        changesInTransactionContext.setTxId(baseTxId + txCounter.getAndIncrement());
        changesInTransactionContext.setExplicitChange(true);
        hooks.forEach(h -> h.fillApplyChangesContext(changesInTransactionContext));

        // Set thread name
        ThreadNameStateTool threadNameStateTool = ThreadTools.nameThread() //
                .appendText(" - txId:") //
                .appendText(changesInTransactionContext.getTxId()) //
                .appendText(" - user:") //
                .appendText(changesInTransactionContext.getUserType().name());

        if (changesInTransactionContext.getUserName() != null) {
            threadNameStateTool.appendText("/") //
                    .appendText(changesInTransactionContext.getUserName());
        }

        threadNameStateTool.change();

        logger.info("----- [changesExecute] Begin -----");

        long maxTime = System.currentTimeMillis() + infiniteLoopTimeoutInMs;

        try {

            // 1. Apply the changes
            applyChanges(changesInTransactionContext, changes);
            changesInTransactionContext.setExplicitChange(false);

            boolean hadChangesInLastLoop = changesInTransactionContext.hasChangesInLastRun();
            while (System.currentTimeMillis() < maxTime) {

                // 2. Call the rules
                List<ActionHandler> actionHandlers = new ArrayList<>();
                ipPluginService.getChangesEvents().forEach(changesEvent -> {
                    actionHandlers.addAll(changesEvent.getChangesEventHandler().computeActionsToExecute(commonServicesContext, changesInTransactionContext));
                });

                // Clear the changes in the last loop
                changesInTransactionContext.clearLast();

                // 3. Run all ActionHandler
                AtomicBoolean hadChanges = new AtomicBoolean();
                logger.info("There are {} actions to handle", actionHandlers.size());
                actionHandlers.forEach(actionHandler -> {
                    long executionTimeInMs = TimeExecutionTools.measureInMs(() -> {
                        logger.info("Begin action {}", actionHandler.getClass().getName());
                        actionHandler.executeAction(commonServicesContext, changes);
                        logger.info("End action {}", actionHandler.getClass().getName());
                    });

                    // 4. If any changes, apply the changes
                    if (changes.hasChanges()) {
                        hadChanges.set(true);
                        applyChanges(changesInTransactionContext, changes);
                    }

                    // Report
                    changesInTransactionContext.addExecutionTime(actionHandler, executionTimeInMs);
                });

                // 4. If any changes, go to #2
                if (!hadChanges.get() && !hadChangesInLastLoop) {
                    if (!changesInTransactionContext.hasChangesInLastRun()) {
                        logger.info("The actions did not provide any changes to apply and there were no changes in the last apply run. Completed");
                        break;
                    }
                }

                hadChangesInLastLoop = hadChanges.get();

            }

            if (changesInTransactionContext.hasChangesInLastRun()) {
                // InfiniteUpdateLoop Display report
                logger.error("Iterated for too long and there are always changes");
                hooks.forEach(h -> h.failureInfinite(changesInTransactionContext));
                logger.info("Report Update count: {}", Joiner.on(", ").join(changesInTransactionContext.generateTop10UpdateCountReport()));
                logger.info("Report Event Handler execution time: {}", Joiner.on(", ").join(changesInTransactionContext.generateTop10UpdateEventHandlerExecutionTimeReport()));
                throw new InfiniteUpdateLoop("Iterated for too long and there are always changes");
            }

            // Show reports
            hooks.forEach(h -> h.success(changesInTransactionContext));
            logger.info("Report Update count: {}", Joiner.on(", ").join(changesInTransactionContext.generateTop10UpdateCountReport()));
            logger.info("Report Event Handler execution time: {}", Joiner.on(", ").join(changesInTransactionContext.generateTop10UpdateEventHandlerExecutionTimeReport()));

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

    public void setInfiniteLoopTimeoutInMs(long infiniteLoopTimeoutInMs) {
        this.infiniteLoopTimeoutInMs = infiniteLoopTimeoutInMs;
    }

}
