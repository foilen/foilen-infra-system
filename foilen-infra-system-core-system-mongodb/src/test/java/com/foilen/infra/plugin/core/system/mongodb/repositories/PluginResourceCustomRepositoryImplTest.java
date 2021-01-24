/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.repositories;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;

import com.foilen.infra.plugin.core.system.junits.JunitsHelper;
import com.foilen.infra.plugin.core.system.mongodb.AbstractSpringTest;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.upgrader.tasks.UpgradeTask;

public class PluginResourceCustomRepositoryImplTest extends AbstractSpringTest {

    @Autowired
    private CommonServicesContext commonServicesContext;
    @Autowired
    private InternalServicesContext internalServicesContext;
    @Autowired
    private PluginResourceRepository pluginResourceRepository;
    @Autowired
    private List<UpgradeTask> upgradeTasks;

    @Before
    public void beforeEach() {
        pluginResourceRepository.deleteAll();

        upgradeTasks.forEach(u -> u.execute());
    }

    @Test
    public void testFindAllPageable() {
        JunitsHelper.createFakeData(commonServicesContext, internalServicesContext);

        // All
        Pageable pageable = PageRequest.of(0, 100, Direction.ASC, "resource.resourceName");
        AssertTools.assertJsonComparisonWithoutNulls("PluginResourceCustomRepositoryImplTest-testFindAllPageable-all-1.json", getClass(), cleanup(pluginResourceRepository.findAll(pageable)));
        pageable = PageRequest.of(0, 2, Direction.ASC, "resource.resourceName");
        AssertTools.assertJsonComparisonWithoutNulls("PluginResourceCustomRepositoryImplTest-testFindAllPageable-few-1.json", getClass(), cleanup(pluginResourceRepository.findAll(pageable)));
    }

}
