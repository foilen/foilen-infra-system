/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.testingcontroller;

import java.util.ArrayList;
import java.util.List;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractUpdateEventHandler;
import com.foilen.infra.plugin.v1.model.junit.JunitResource;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tuple.Tuple3;

public class TestingControllerMockUpdateHander extends AbstractUpdateEventHandler<JunitResource> {

    private List<Long> added = new ArrayList<>();
    private List<Long> updated = new ArrayList<>();
    private List<Long> deleted = new ArrayList<>();
    private List<Long> checked = new ArrayList<>();

    @Override
    public void addHandler(CommonServicesContext services, ChangesContext changes, JunitResource resource) {
        added.add(resource.getInternalId());
    }

    @Override
    public void checkAndFix(CommonServicesContext services, ChangesContext changes, JunitResource resource) {
        checked.add(resource.getInternalId());
    }

    public void clear() {
        added.clear();
        updated.clear();
        deleted.clear();
        checked.clear();
    }

    @Override
    public void deleteHandler(CommonServicesContext services, ChangesContext changes, JunitResource resource, List<Tuple3<IPResource, String, IPResource>> previousLinks) {
        deleted.add(resource.getInternalId());
    }

    public List<Long> getAdded() {
        return added;
    }

    public List<Long> getChecked() {
        return checked;
    }

    public List<Long> getDeleted() {
        return deleted;
    }

    public List<Long> getUpdated() {
        return updated;
    }

    @Override
    public Class<JunitResource> supportedClass() {
        return JunitResource.class;
    }

    @Override
    public void updateHandler(CommonServicesContext services, ChangesContext changes, JunitResource previousResource, JunitResource newResource) {
        updated.add(newResource.getInternalId());
    }

}
