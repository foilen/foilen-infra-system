/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.junits;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.model.junit.JunitResource;
import com.foilen.infra.plugin.v1.model.junit.JunitResourceEnum;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tools.DateTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.google.common.collect.Sets;

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
                        JunitResource.PROPERTY_SET_TEXTS, //
                        JunitResource.PROPERTY_SET_DATES, //
                        JunitResource.PROPERTY_SET_DOUBLES, //
                        JunitResource.PROPERTY_SET_ENUMERATIONS, //
                        JunitResource.PROPERTY_SET_LONGS, //
                        JunitResource.PROPERTY_SET_INTEGERS, //
                        JunitResource.PROPERTY_SET_FLOATS //
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

    public static void createFakeDataWithSets(InternalServicesContext ctx) {

        // JunitResource
        ChangesContext changes = new ChangesContext();

        changes.getResourcesToAdd().add(createWithSets( //
                "sets_0.0", //
                Sets.newHashSet(), //
                Sets.newHashSet(), //
                Sets.newHashSet(), //
                Sets.newHashSet(), //
                Sets.newHashSet(), //
                Sets.newHashSet(), //
                Sets.newHashSet() //
        ));
        changes.getResourcesToAdd().add(createWithSets( //
                "sets_1.1", //
                Sets.newHashSet(DateTools.parseDateOnly("2000-01-01")), //
                Sets.newHashSet(1.0d), //
                Sets.newHashSet(JunitResourceEnum.A), //
                Sets.newHashSet(1.0f), //
                Sets.newHashSet(1l), //
                Sets.newHashSet(1), //
                Sets.newHashSet("1") //
        ));
        changes.getResourcesToAdd().add(createWithSets( //
                "sets_1.2", //
                Sets.newHashSet(DateTools.parseDateOnly("2000-01-02")), //
                Sets.newHashSet(2.0d), //
                Sets.newHashSet(JunitResourceEnum.B), //
                Sets.newHashSet(2.0f), //
                Sets.newHashSet(2l), //
                Sets.newHashSet(2), //
                Sets.newHashSet("2") //
        ));
        changes.getResourcesToAdd().add(createWithSets( //
                "sets_2.1", //
                Sets.newHashSet(DateTools.parseDateOnly("2000-01-01"), DateTools.parseDateOnly("2000-02-01")), //
                Sets.newHashSet(1.0d, 2.0d), //
                Sets.newHashSet(JunitResourceEnum.A, JunitResourceEnum.B), //
                Sets.newHashSet(1.0f, 2.0f), //
                Sets.newHashSet(1l, 2l), //
                Sets.newHashSet(1, 2), //
                Sets.newHashSet("1", "2") //
        ));
        changes.getResourcesToAdd().add(createWithSets( //
                "sets_2.2", //
                Sets.newHashSet(DateTools.parseDateOnly("2000-01-02"), DateTools.parseDateOnly("2000-02-02")), //
                Sets.newHashSet(3.0d, 4.0d), //
                Sets.newHashSet(JunitResourceEnum.B, JunitResourceEnum.C), //
                Sets.newHashSet(3.0f, 4.0f), //
                Sets.newHashSet(3l, 4l), //
                Sets.newHashSet(3, 4), //
                Sets.newHashSet("3", "4") //
        ));

        ctx.getInternalChangeService().changesExecute(changes);

    }

    private static IPResource createWithSets(String text, Set<Date> setDates, Set<Double> setDoubles, Set<JunitResourceEnum> setEnumerations, Set<Float> setFloats, Set<Long> setLongs,
            Set<Integer> setIntegers, Set<String> setTexts) {
        JunitResource junitResource = new JunitResource(text);
        junitResource.setSetDates(setDates);
        junitResource.setSetDoubles(setDoubles);
        junitResource.setSetEnumerations(setEnumerations);
        junitResource.setSetFloats(setFloats);
        junitResource.setSetLongs(setLongs);
        junitResource.setSetIntegers(setIntegers);
        junitResource.setSetTexts(setTexts);
        return junitResource;
    }

}
