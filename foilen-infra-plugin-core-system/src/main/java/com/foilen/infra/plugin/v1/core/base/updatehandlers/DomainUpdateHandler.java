/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.updatehandlers;

import com.foilen.infra.plugin.v1.core.base.resources.Domain;
import com.foilen.infra.plugin.v1.core.common.DomainHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractCommonMethodUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.eventhandler.CommonMethodUpdateEventHandlerContext;
import com.foilen.infra.plugin.v1.core.exception.IllegalUpdateException;
import com.foilen.smalltools.tools.StringTools;

public class DomainUpdateHandler extends AbstractCommonMethodUpdateEventHandler<Domain> {

    @Override
    protected void commonHandlerExecute(CommonServicesContext services, ChangesContext changes, CommonMethodUpdateEventHandlerContext<Domain> context) {

        Domain resource = context.getResource();

        if (context.getOldResource() != null && !StringTools.safeEquals(context.getOldResource().getName(), resource.getName())) {
            throw new IllegalUpdateException("You cannot change a Domain's name");
        }

        // Use a Domain for the parent domain
        String parentDomainName = DomainHelper.parentDomainName(resource.getName());
        if (parentDomainName == null) {
            return;
        }

        context.getManagedResourceTypes().add(Domain.class);
        context.getManagedResources().add(new Domain(parentDomainName, DomainHelper.reverseDomainName(parentDomainName)));

    }

    @Override
    public Class<Domain> supportedClass() {
        return Domain.class;
    }

}
