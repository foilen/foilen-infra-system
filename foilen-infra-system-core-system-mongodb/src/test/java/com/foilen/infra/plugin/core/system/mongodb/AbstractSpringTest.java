/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb;

import org.junit.runner.RunWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.foilen.infra.plugin.core.system.mongodb.spring.MongoDbSpringConfig;
import com.foilen.infra.plugin.core.system.mongodb.spring.ResourceServicesMongoDBSpringConfig;

@RunWith(SpringRunner.class)
@Import({ MongoDbSpringConfig.class, ResourceServicesMongoDBSpringConfig.class })
public abstract class AbstractSpringTest {

    public AbstractSpringTest() {
        System.setProperty("spring.data.mongodb.uri", "mongodb://127.0.0.1:27085/");
        System.setProperty("spring.data.mongodb.database", "junits");
    }

}
