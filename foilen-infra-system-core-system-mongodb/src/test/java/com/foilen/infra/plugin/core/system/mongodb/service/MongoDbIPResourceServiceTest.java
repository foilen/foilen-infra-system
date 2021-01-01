/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb.service;

import java.util.List;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.foilen.infra.plugin.core.system.junits.AbstractIPResourceServiceTest;
import com.foilen.infra.plugin.core.system.mongodb.repositories.MessageRepository;
import com.foilen.infra.plugin.core.system.mongodb.repositories.PluginResourceLinkRepository;
import com.foilen.infra.plugin.core.system.mongodb.repositories.PluginResourceRepository;
import com.foilen.infra.plugin.core.system.mongodb.spring.MongoDbSpringConfig;
import com.foilen.infra.plugin.core.system.mongodb.spring.ResourceServicesMongoDBSpringConfig;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.smalltools.upgrader.tasks.UpgradeTask;

@RunWith(SpringRunner.class)
@Import({ MongoDbSpringConfig.class, ResourceServicesMongoDBSpringConfig.class })
public class MongoDbIPResourceServiceTest extends AbstractIPResourceServiceTest {

    @Autowired
    private CommonServicesContext commonServicesContext;
    @Autowired
    private InternalServicesContext internalServicesContext;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private PluginResourceRepository pluginResourceRepository;
    @Autowired
    private PluginResourceLinkRepository pluginResourceLinkRepository;
    @Autowired
    private List<UpgradeTask> upgradeTasks;

    public MongoDbIPResourceServiceTest() {
        System.setProperty("spring.data.mongodb.uri", "mongodb://127.0.0.1:27085/");
        System.setProperty("spring.data.mongodb.database", "junits");
    }

    @Override
    @Before
    public void beforeEach() {
        messageRepository.deleteAll();
        pluginResourceLinkRepository.deleteAll();
        pluginResourceRepository.deleteAll();

        upgradeTasks.forEach(u -> u.execute());

        super.beforeEach();
    }

    @Override
    protected CommonServicesContext getCommonServicesContext() {
        return commonServicesContext;
    }

    @Override
    protected InternalServicesContext getInternalServicesContext() {
        return internalServicesContext;
    }

}
