/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.junits;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.exception.IllegalUpdateException;
import com.foilen.infra.plugin.v1.core.exception.ResourceNotFoundException;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.infra.resource.example.JunitResource;
import com.foilen.infra.resource.example.JunitResourceEnum;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.DateTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.ResourceTools;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

public class JunitsHelper {

    private static final Logger logger = LoggerFactory.getLogger(JunitsHelper.class);

    public static void assertState(CommonServicesContext commonServicesContext, InternalServicesContext internalServicesContext, String resourceName, Class<?> resourceContext) {
        assertState(commonServicesContext, internalServicesContext, resourceName, resourceContext, false);
    }

    public static void assertState(CommonServicesContext commonServicesContext, InternalServicesContext internalServicesContext, String resourceName, Class<?> resourceContext, boolean withContent) {
        ResourcesState resourcesState = new ResourcesState();
        resourcesState.setResources(internalServicesContext.getInternalIPResourceService().resourceFindAll().stream() //
                .map(resource -> {
                    ResourceState resourceState = new ResourceState(getResourceDetails(resource));

                    // With content
                    if (withContent) {
                        // Remove some values
                        IPResource cloned = JsonTools.clone(resource);
                        cloned.setInternalId(null);
                        resourceState.setContent(cloned);
                    }

                    // Links
                    List<ResourcesStateLink> links = commonServicesContext.getResourceService().linkFindAllByFromResource(resource).stream() //
                            .map(link -> new ResourcesStateLink(link.getA(), getResourceDetails(link.getB()))) //
                            .collect(Collectors.toList());
                    resourceState.setLinks(links);

                    // Tags
                    resourceState.setTags(commonServicesContext.getResourceService().tagFindAllByResource(resource).stream().sorted().collect(Collectors.toList()));

                    return resourceState;
                }) //
                .collect(Collectors.toList()));

        resourcesState.sort();

        String actualJson = JsonTools.prettyPrintWithoutNulls(resourcesState);
        actualJson = actualJson.replaceAll("\\\\n", "\n");
        actualJson = actualJson.replaceAll("\\\\t", "\t");
        String expectedJson = ResourceTools.getResourceAsString(resourceName, resourceContext);
        AssertTools.assertIgnoreLineFeed(expectedJson, actualJson);
    }

    public static void createFakeData(CommonServicesContext commonCtx, InternalServicesContext internalCtx) {

        // JunitResource
        ChangesContext changes = new ChangesContext(commonCtx.getResourceService());
        JunitResource junitResource = new JunitResource("www.example.com", JunitResourceEnum.A, 1);
        changes.resourceAdd(junitResource);
        changes.tagAdd(junitResource, "tag1");
        changes.tagAdd(junitResource, "asite");
        junitResource = new JunitResource("www.example.com", JunitResourceEnum.A, 2);
        changes.resourceAdd(junitResource);
        changes.tagAdd(junitResource, "asite");
        changes.resourceAdd(new JunitResource("example.com", JunitResourceEnum.B, 3));

        changes.resourceAdd(new JunitResource("t1_aaa", JunitResourceEnum.A, DateTools.parseFull("2000-01-01 00:00:00"), 1, 1L, 1.0, 1.0f, true, "one", "two"));
        changes.resourceAdd(new JunitResource("t2_aaa", JunitResourceEnum.C, DateTools.parseFull("2000-06-01 00:00:00"), 5, 8L, 1.5, 7.3f, false, "one", "three"));
        changes.resourceAdd(new JunitResource("zz", JunitResourceEnum.B, DateTools.parseFull("2000-04-01 00:00:00"), 80, 4L, 77.6, 3.1f, true));

        internalCtx.getInternalChangeService().changesExecute(changes);

    }

    public static void createFakeDataWithSets(CommonServicesContext commonCtx, InternalServicesContext internalCtx) {

        // JunitResource
        ChangesContext changes = new ChangesContext(commonCtx.getResourceService());

        changes.resourceAdd(createWithSets( //
                "sets_0.0", //
                Sets.newHashSet(), //
                Sets.newHashSet(), //
                Sets.newHashSet(), //
                Sets.newHashSet(), //
                Sets.newHashSet(), //
                Sets.newHashSet(), //
                Sets.newHashSet() //
        ));
        changes.resourceAdd(createWithSets( //
                "sets_1.1", //
                Sets.newHashSet(DateTools.parseDateOnly("2000-01-01")), //
                Sets.newHashSet(1.0d), //
                Sets.newHashSet(JunitResourceEnum.A), //
                Sets.newHashSet(1.0f), //
                Sets.newHashSet(1l), //
                Sets.newHashSet(1), //
                Sets.newHashSet("1") //
        ));
        changes.resourceAdd(createWithSets( //
                "sets_1.2", //
                Sets.newHashSet(DateTools.parseDateOnly("2000-01-02")), //
                Sets.newHashSet(2.0d), //
                Sets.newHashSet(JunitResourceEnum.B), //
                Sets.newHashSet(2.0f), //
                Sets.newHashSet(2l), //
                Sets.newHashSet(2), //
                Sets.newHashSet("2") //
        ));
        changes.resourceAdd(createWithSets( //
                "sets_2.1", //
                Sets.newHashSet(DateTools.parseDateOnly("2000-01-01"), DateTools.parseDateOnly("2000-02-01")), //
                Sets.newHashSet(1.0d, 2.0d), //
                Sets.newHashSet(JunitResourceEnum.A, JunitResourceEnum.B), //
                Sets.newHashSet(1.0f, 2.0f), //
                Sets.newHashSet(1l, 2l), //
                Sets.newHashSet(1, 2), //
                Sets.newHashSet("1", "2") //
        ));
        changes.resourceAdd(createWithSets( //
                "sets_2.2", //
                Sets.newHashSet(DateTools.parseDateOnly("2000-01-02"), DateTools.parseDateOnly("2000-02-02")), //
                Sets.newHashSet(3.0d, 4.0d), //
                Sets.newHashSet(JunitResourceEnum.B, JunitResourceEnum.C), //
                Sets.newHashSet(3.0f, 4.0f), //
                Sets.newHashSet(3l, 4l), //
                Sets.newHashSet(3, 4), //
                Sets.newHashSet("3", "4") //
        ));

        internalCtx.getInternalChangeService().changesExecute(changes);

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

    public static ResourcesDump dumpExport(CommonServicesContext commonServicesContext, InternalServicesContext internalServicesContext) {
        ResourcesDump dump = new ResourcesDump();

        IPResourceService resourceService = commonServicesContext.getResourceService();
        for (IPResource resource : internalServicesContext.getInternalIPResourceService().resourceFindAll()) {
            String resourceType = resourceService.getResourceDefinition(resource).getResourceType();
            String resourceTypeAndName = resourceType + "/" + resource.getResourceName();

            // Export resource
            dump.getResources().add(new ResourcesDumpResource(resourceType, resource));

            // Export tags
            resourceService.tagFindAllByResource(resource).stream() //
                    .forEach(it -> dump.getTags().add(new ResourcesDumpTag(resourceTypeAndName, it)));

            // Export links
            resourceService.linkFindAllByFromResource(resource).stream() //
                    .map(it -> {
                        String toResourceType = resourceService.getResourceDefinition(it.getB()).getResourceType();
                        String toResourceTypeAndName = toResourceType + "/" + it.getB().getResourceName();
                        return new ResourcesDumpLink(resourceTypeAndName, it.getA(), toResourceTypeAndName);
                    }) //
                    .forEach(it -> dump.getLinks().add(it));

        }

        dump.sort();
        return dump;
    }

    public static void dumpImport(CommonServicesContext commonServicesContext, InternalServicesContext internalServicesContext, ResourcesDump resourcesDump) {
        dumpImport(commonServicesContext.getResourceService(), internalServicesContext.getInternalChangeService(), resourcesDump);
    }

    public static void dumpImport(IPResourceService resourceService, InternalChangeService internalChangeService, ResourcesDump resourcesDump) {
        // Import all the resources
        Map<String, IPResource> resourcesByTypeAndName = new HashMap<>();
        ChangesContext changes = new ChangesContext(resourceService);
        logger.info("Importing Resources");
        for (ResourcesDumpResource dumpResource : resourcesDump.getResources()) {
            String resourceType = dumpResource.getResourceType();
            logger.info("Type: {}", resourceType);
            Class<? extends IPResource> resourceClass = resourceService.getResourceDefinition(resourceType).getResourceClass();

            String resourceName = dumpResource.getResourceName();

            IPResource resource = JsonTools.readFromString(dumpResource.getResourceJson(), resourceClass);
            resourcesByTypeAndName.put(resourceType + "/" + resourceName, resource);

            changes.resourceAdd(resource);
        }

        // Import all the tags
        logger.info("Importing Tags");
        for (ResourcesDumpTag dumpTag : resourcesDump.getTags()) {

            String resourceTypeAndName = dumpTag.getResourceTypeAndName();
            String tagName = dumpTag.getTag();

            IPResource resource = resourcesByTypeAndName.get(resourceTypeAndName);
            if (resource == null) {
                logger.error("The resource {} does not exit", resourceTypeAndName);
                throw new ResourceNotFoundException(resourceTypeAndName);
            }
            if (Strings.isNullOrEmpty(tagName)) {
                logger.error("The tag name cannot be empty for resource {}", resourceTypeAndName);
                throw new IllegalUpdateException("The tag name cannot be empty for resource " + resourceTypeAndName);
            }

            changes.tagAdd(resource, tagName);

        }

        // Import all the links
        logger.info("Importing Links");
        for (ResourcesDumpLink dumpLink : resourcesDump.getLinks()) {

            String fromResourceTypeAndName = dumpLink.getFromResourceTypeAndName();
            String linkType = dumpLink.getLinkType();
            String toResourceTypeAndName = dumpLink.getToResourceTypeAndName();

            IPResource fromResource = resourcesByTypeAndName.get(fromResourceTypeAndName);
            if (fromResource == null) {
                logger.error("The resource {} does not exit", fromResourceTypeAndName);
                throw new ResourceNotFoundException(fromResourceTypeAndName);
            }
            if (Strings.isNullOrEmpty(linkType)) {
                logger.error("The link type cannot be empty");
                throw new IllegalUpdateException("The link type cannot be em");
            }
            IPResource toResource = resourcesByTypeAndName.get(toResourceTypeAndName);
            if (toResource == null) {
                logger.error("The resource {} does not exit", toResourceTypeAndName);
                throw new ResourceNotFoundException(toResourceTypeAndName);
            }

            changes.linkAdd(fromResource, linkType, toResource);

        }

        // Execute the changes
        logger.info("Execute the changes");
        internalChangeService.changesExecute(changes);
    }

    protected static String getResourceDetails(IPResource resource) {
        return resource.getClass().getSimpleName() + " | " + resource.getResourceName() + " | " + resource.getResourceDescription();
    }

}
