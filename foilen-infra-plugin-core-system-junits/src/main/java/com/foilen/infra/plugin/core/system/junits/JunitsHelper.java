/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.junits;

import java.util.Arrays;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.model.junit.JunitResource;
import com.foilen.infra.plugin.v1.model.junit.JunitResourceEnum;
import com.foilen.smalltools.tools.DateTools;
import com.foilen.smalltools.tuple.Tuple2;

public class JunitsHelper {

    public static void addResourcesDefinition(InternalServicesContext ctx) {
        IPResourceDefinition resourceDefinition = new IPResourceDefinition(JunitResource.class, "Junit", //
                Arrays.asList(JunitResource.PROPERTY_TEXT, JunitResource.PROPERTY_ENUMERATION, JunitResource.PROPERTY_INTEGER_NUMBER), //
                Arrays.asList( //
                        JunitResource.PROPERTY_BOOL, //
                        JunitResource.PROPERTY_DATE, //
                        JunitResource.PROPERTY_DOUBLE_NUMBER, //
                        JunitResource.PROPERTY_ENUMERATION, //
                        JunitResource.PROPERTY_FLOAT_NUMBER, //
                        JunitResource.PROPERTY_INTEGER_NUMBER, //
                        JunitResource.PROPERTY_LONG_NUMBER, //
                        JunitResource.PROPERTY_TEXT, //
                        JunitResource.PROPERTY_SET_TEXTS //
                ));
        ctx.getInternalIPResourceService().resourceAdd(resourceDefinition);
    }

    public static void createFakeData(InternalServicesContext ctx) {

        // JunitResource
        ChangesContext changes = new ChangesContext();
        JunitResource junitResource = new JunitResource("www.example.com", JunitResourceEnum.A, 1);
        changes.getResourcesToAdd().add(junitResource);
        changes.getTagsToAdd().addAll(Arrays.asList( //
                new Tuple2<>(junitResource, "tag1"), //
                new Tuple2<>(junitResource, "asite")));
        junitResource = new JunitResource("www.example.com", JunitResourceEnum.A, 2);
        changes.getResourcesToAdd().add(junitResource);
        changes.getTagsToAdd().addAll(Arrays.asList( //
                new Tuple2<>(junitResource, "asite")));
        changes.getResourcesToAdd().add(new JunitResource("example.com", JunitResourceEnum.B, 3));

        changes.getResourcesToAdd().add(new JunitResource("t1_aaa", JunitResourceEnum.A, DateTools.parseFull("2000-01-01 00:00:00"), 1, 1L, 1.0, 1.0f, true, "one", "two"));
        changes.getResourcesToAdd().add(new JunitResource("t2_aaa", JunitResourceEnum.C, DateTools.parseFull("2000-06-01 00:00:00"), 5, 8L, 1.5, 7.3f, false, "one", "three"));
        changes.getResourcesToAdd().add(new JunitResource("zz", JunitResourceEnum.B, DateTools.parseFull("2000-04-01 00:00:00"), 80, 4L, 77.6, 3.1f, true));

        ctx.getInternalChangeService().changesExecute(changes);

    }

}
