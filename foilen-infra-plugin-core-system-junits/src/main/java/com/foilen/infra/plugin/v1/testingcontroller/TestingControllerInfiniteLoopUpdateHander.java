/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.testingcontroller;

import java.util.Date;
import java.util.List;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractUpdateEventHandler;
import com.foilen.infra.plugin.v1.model.junit.JunitResource;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tools.SecureRandomTools;
import com.foilen.smalltools.tuple.Tuple3;

public class TestingControllerInfiniteLoopUpdateHander extends AbstractUpdateEventHandler<JunitResource> {

    private boolean alwaysUpdate = false;

    @Override
    public void addHandler(CommonServicesContext services, ChangesContext changes, JunitResource resource) {
        common(services, changes, resource);
    }

    @Override
    public void checkAndFix(CommonServicesContext services, ChangesContext changes, JunitResource resource) {
        common(services, changes, resource);
    }

    private void common(CommonServicesContext services, ChangesContext changes, JunitResource resource) {

        if (!alwaysUpdate) {
            return;
        }

        List<JunitResource> junitResources = services.getResourceService().resourceFindAll(services.getResourceService().createResourceQuery(JunitResource.class));
        if (junitResources.size() < 3) {
            for (int i = 0; i < 5; ++i) {
                changes.resourceAdd(new JunitResource(SecureRandomTools.randomHexString(10)));
            }
        } else {
            JunitResource update = junitResources.get(0);
            JunitResource refresh = junitResources.get(1);
            JunitResource delete = junitResources.get(2);

            update.setDate(new Date());
            changes.resourceUpdate(update);

            changes.resourceRefresh(refresh);

            changes.resourceDelete(delete);
        }

    }

    @Override
    public void deleteHandler(CommonServicesContext services, ChangesContext changes, JunitResource resource, List<Tuple3<IPResource, String, IPResource>> previousLinks) {
        common(services, changes, resource);
    }

    public boolean isAlwaysUpdate() {
        return alwaysUpdate;
    }

    public void setAlwaysUpdate(boolean alwaysUpdate) {
        this.alwaysUpdate = alwaysUpdate;
    }

    @Override
    public Class<JunitResource> supportedClass() {
        return JunitResource.class;
    }

    @Override
    public void updateHandler(CommonServicesContext services, ChangesContext changes, JunitResource previousResource, JunitResource newResource) {
        common(services, changes, newResource);
    }

}
