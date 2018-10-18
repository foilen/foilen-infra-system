/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.common.changeexecution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.foilen.infra.plugin.v1.core.eventhandler.UpdateEventHandler;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.CollectionsTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.foilen.smalltools.tuple.Tuple3;

public class ApplyChangesContext extends AbstractBasics {

    private String txId;

    private AuditUserType userType = AuditUserType.SYSTEM;
    private String userName;

    private boolean explicitChange;

    // Processing queues
    private Queue<IPResource> addedResources = new LinkedBlockingQueue<>();
    private Queue<IPResource> updatedResourcesPrevious = new LinkedBlockingQueue<>();
    private Queue<IPResource> deletedResources = new LinkedBlockingQueue<>();
    private Queue<Long> resourcesNeedRefresh = new LinkedBlockingQueue<>();

    // Removed resources
    private Map<Long, List<Tuple3<IPResource, String, IPResource>>> deletedResourcePreviousLinksByResourceId = new HashMap<>();
    private Set<Long> removedResourcesInThisTransaction = new LinkedHashSet<>();

    // Reporting
    private Map<String, AtomicInteger> updateCoundByResourceId = new HashMap<>();
    private Map<String, AtomicLong> executionTimeInMsByUpdateHandler = new HashMap<>();

    public void addExecutionTime(UpdateEventHandler<?> updateEventHandler, long time) {
        CollectionsTools.getOrCreateEmpty(executionTimeInMsByUpdateHandler, updateEventHandler.getClass().getSimpleName(), AtomicLong.class).addAndGet(time);
    }

    public List<String> generateUpdateCountReport() {
        List<String> report = updateCoundByResourceId.entrySet().stream() //
                .map(entry -> new Tuple2<>(entry.getKey(), entry.getValue().get())) //
                .sorted((a, b) -> a.getB().compareTo(b.getB()) * -1) //
                .limit(10) //
                .map(it -> it.getA() + " : " + it.getB()) //
                .collect(Collectors.toCollection(() -> new ArrayList<>()));
        if (updateCoundByResourceId.size() > 10) {
            report.add("...");
        }
        return report;
    }

    public List<String> generateUpdateEventHandlerExecutionTimeReport() {
        List<String> report = executionTimeInMsByUpdateHandler.entrySet().stream() //
                .map(entry -> new Tuple2<>(entry.getKey(), entry.getValue().get())) //
                .sorted((a, b) -> a.getB().compareTo(b.getB()) * -1) //
                .limit(10) //
                .map(it -> it.getA() + " : " + it.getB() + " ms") //
                .collect(Collectors.toCollection(() -> new ArrayList<>()));
        if (updateCoundByResourceId.size() > 10) {
            report.add("...");
        }
        return report;
    }

    public Queue<IPResource> getAddedResources() {
        return addedResources;
    }

    public Map<Long, List<Tuple3<IPResource, String, IPResource>>> getDeletedResourcePreviousLinksByResourceId() {
        return deletedResourcePreviousLinksByResourceId;
    }

    public Queue<IPResource> getDeletedResources() {
        return deletedResources;
    }

    public Map<String, AtomicLong> getExecutionTimeInMsByUpdateHandler() {
        return executionTimeInMsByUpdateHandler;
    }

    public Set<Long> getRemovedResourcesInThisTransaction() {
        return removedResourcesInThisTransaction;
    }

    public Queue<Long> getResourcesNeedRefresh() {
        return resourcesNeedRefresh;
    }

    public String getTxId() {
        return txId;
    }

    public Map<String, AtomicInteger> getUpdateCoundByResourceId() {
        return updateCoundByResourceId;
    }

    public Queue<IPResource> getUpdatedResourcesPrevious() {
        return updatedResourcesPrevious;
    }

    public String getUserName() {
        return userName;
    }

    public AuditUserType getUserType() {
        return userType;
    }

    public boolean hasChangesInQueues() {
        return !addedResources.isEmpty() || !updatedResourcesPrevious.isEmpty() || !deletedResources.isEmpty() || !resourcesNeedRefresh.isEmpty();
    }

    public boolean isExplicitChange() {
        return explicitChange;
    }

    public void setAddedResources(Queue<IPResource> addedResources) {
        this.addedResources = addedResources;
    }

    public void setDeletedResourcePreviousLinksByResourceId(Map<Long, List<Tuple3<IPResource, String, IPResource>>> deletedResourcePreviousLinksByResourceId) {
        this.deletedResourcePreviousLinksByResourceId = deletedResourcePreviousLinksByResourceId;
    }

    public void setDeletedResources(Queue<IPResource> deletedResources) {
        this.deletedResources = deletedResources;
    }

    public void setExecutionTimeInMsByUpdateHandler(Map<String, AtomicLong> executionTimeInMsByUpdateHandler) {
        this.executionTimeInMsByUpdateHandler = executionTimeInMsByUpdateHandler;
    }

    public void setExplicitChange(boolean explicitChange) {
        this.explicitChange = explicitChange;
    }

    public void setRemovedResourcesInThisTransaction(Set<Long> removedResourcesInThisTransaction) {
        this.removedResourcesInThisTransaction = removedResourcesInThisTransaction;
    }

    public void setResourcesNeedRefresh(Queue<Long> resourcesNeedRefresh) {
        this.resourcesNeedRefresh = resourcesNeedRefresh;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public void setUpdateCoundByResourceId(Map<String, AtomicInteger> updateCoundByResourceId) {
        this.updateCoundByResourceId = updateCoundByResourceId;
    }

    public void setUpdatedResourcesPrevious(Queue<IPResource> updatedResourcesPrevious) {
        this.updatedResourcesPrevious = updatedResourcesPrevious;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserType(AuditUserType userType) {
        this.userType = userType;
    }

}
