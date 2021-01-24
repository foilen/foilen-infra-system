/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.mongodb;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.runner.RunWith;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringRunner;

import com.foilen.infra.plugin.core.system.mongodb.repositories.documents.PluginResource;
import com.foilen.infra.plugin.core.system.mongodb.spring.MongoDbSpringConfig;
import com.foilen.infra.plugin.core.system.mongodb.spring.ResourceServicesMongoDBSpringConfig;
import com.foilen.smalltools.tools.JsonTools;

@RunWith(SpringRunner.class)
@Import({ MongoDbSpringConfig.class, ResourceServicesMongoDBSpringConfig.class })
public abstract class AbstractSpringTest {

    public AbstractSpringTest() {
        System.setProperty("spring.data.mongodb.uri", "mongodb://127.0.0.1:27085/");
        System.setProperty("spring.data.mongodb.database", "junits");
    }

    protected Object cleanup(Page<PluginResource> page) {
        page.get().forEach(it -> it.setId(null));
        @SuppressWarnings("unchecked")
        SortedMap<String, Object> cloned = JsonTools.clone(page, TreeMap.class);
        mapsToSortedMaps(cloned);
        return cloned;
    }

    protected void mapsToSortedMaps(Map<String, Object> root) {
        for (String key : root.keySet()) {
            Object value = root.get(key);
            if (value instanceof Map && !(value instanceof SortedMap)) {
                @SuppressWarnings("unchecked")
                TreeMap<String, Object> cloned = JsonTools.clone(value, TreeMap.class);
                mapsToSortedMaps(cloned);
                root.put(key, cloned);
            }
        }
    }

}
