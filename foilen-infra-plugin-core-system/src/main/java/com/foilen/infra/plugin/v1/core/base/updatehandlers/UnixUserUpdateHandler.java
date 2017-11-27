/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.updatehandlers;

import java.util.List;

import com.foilen.infra.plugin.v1.core.base.resources.UnixUser;
import com.foilen.infra.plugin.v1.core.base.resources.helper.UnixUserAvailableIdHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.exception.IllegalUpdateException;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tools.StringTools;
import com.foilen.smalltools.tuple.Tuple3;

public class UnixUserUpdateHandler extends AbstractUpdateEventHandler<UnixUser> {

    @Override
    public void addHandler(CommonServicesContext services, ChangesContext changes, UnixUser resource) {
        // Unique user name
        checkUniqueName(services, resource.getName());

        // Choose the next id
        if (resource.getId() == null) {
            resource.setId(UnixUserAvailableIdHelper.getNextAvailableId());
            changes.resourceUpdate(resource.getInternalId(), resource);
        }

    }

    @Override
    public void checkAndFix(CommonServicesContext services, ChangesContext changes, UnixUser resource) {
    }

    private void checkUniqueName(CommonServicesContext services, String name) {
        IPResourceService resourceService = services.getResourceService();
        List<UnixUser> unixUsers = resourceService.resourceFindAll(resourceService.createResourceQuery(UnixUser.class) //
                .propertyEquals(UnixUser.PROPERTY_NAME, name));
        if (unixUsers.size() > 1) {
            throw new IllegalUpdateException("Unix User name " + name + " is already used");
        }
    }

    @Override
    public void deleteHandler(CommonServicesContext services, ChangesContext changes, UnixUser resource, List<Tuple3<IPResource, String, IPResource>> previousLinks) {
    }

    @Override
    public Class<UnixUser> supportedClass() {
        return UnixUser.class;
    }

    @Override
    public void updateHandler(CommonServicesContext services, ChangesContext changes, UnixUser previousResource, UnixUser newResource) {
        // Unique user name
        if (!StringTools.safeEquals(previousResource.getName(), newResource.getName())) {
            checkUniqueName(services, newResource.getName());
        }
    }

}
