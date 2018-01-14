/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.eventhandler;

import java.util.List;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tuple.Tuple3;

/**
 * Has a "delete" that removes all the managed resources and a common method for create and update.
 */
public abstract class AbstractCommonMethodUpdateEventHandler<R extends IPResource> extends AbstractUpdateEventHandler<R> {

    @Override
    public final void addHandler(CommonServicesContext services, ChangesContext changes, R resource) {
        CommonMethodUpdateEventHandlerContext<R> context = new CommonMethodUpdateEventHandlerContext<>();
        context.setResource(resource);
        commonHandler(services, changes, context);
    }

    @Override
    public final void checkAndFix(CommonServicesContext services, ChangesContext changes, R resource) {
        CommonMethodUpdateEventHandlerContext<R> context = new CommonMethodUpdateEventHandlerContext<>();
        context.setResource(resource);
        commonHandler(services, changes, context);
    }

    private void commonHandler(CommonServicesContext services, ChangesContext changes, CommonMethodUpdateEventHandlerContext<R> context) {

        commonHandlerExecute(services, changes, context);

        if (context.isManagedResourcesUpdateContentIfExists()) {
            manageNeededResourcesWithContentUpdates(services, changes, context.getResource(), context.getManagedResources(), context.getManagedResourceTypes());
        } else {
            manageNeededResourcesNoUpdates(services, changes, context.getResource(), context.getManagedResources(), context.getManagedResourceTypes());
        }
    }

    /**
     * Set if the managed resources must be updated if they exists, their classes and the resources themselves.
     *
     * @param services
     *            the services you can use
     * @param changes
     *            any changes you want to do
     * @param context
     *            the context of the current update
     */
    protected abstract void commonHandlerExecute(CommonServicesContext services, ChangesContext changes, CommonMethodUpdateEventHandlerContext<R> context);

    @Override
    public final void deleteHandler(CommonServicesContext services, ChangesContext changes, R resource, List<Tuple3<IPResource, String, IPResource>> previousLinks) {
        detachManagedResources(services, changes, resource, previousLinks);
    }

    @Override
    public final void updateHandler(CommonServicesContext services, ChangesContext changes, R previousResource, R newResource) {
        CommonMethodUpdateEventHandlerContext<R> context = new CommonMethodUpdateEventHandlerContext<>();
        context.setOldResource(previousResource);
        context.setResource(newResource);
        commonHandler(services, changes, context);
    }

}
