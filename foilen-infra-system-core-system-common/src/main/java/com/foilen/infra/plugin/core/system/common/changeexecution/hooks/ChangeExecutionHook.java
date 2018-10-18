/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.common.changeexecution.hooks;

import com.foilen.infra.plugin.core.system.common.changeexecution.ApplyChangesContext;
import com.foilen.infra.plugin.v1.model.resource.IPResource;

public interface ChangeExecutionHook {

    default void failureInfinite(ApplyChangesContext applyChangesContext) {
    }

    default void fillApplyChangesContext(ApplyChangesContext applyChangesContext) {
    }

    default void linkAdded(ApplyChangesContext applyChangesContext, IPResource fromResource, String linkType, IPResource toResource) {
    }

    default void linkDeleted(ApplyChangesContext applyChangesContext, IPResource fromResource, String linkType, IPResource toResource) {
    }

    default void resourceAdded(ApplyChangesContext applyChangesContext, IPResource resource) {
    }

    default void resourceDeleted(ApplyChangesContext applyChangesContext, IPResource resource) {
    }

    default void resourceUpdated(ApplyChangesContext applyChangesContext, IPResource previousResource, IPResource updatedResource) {
    }

    default void success(ApplyChangesContext applyChangesContext) {
    }

    default void tagAdded(ApplyChangesContext applyChangesContext, IPResource resource, String tagName) {
    }

    default void tagDeleted(ApplyChangesContext applyChangesContext, IPResource resource, String tagName) {
    }

}
