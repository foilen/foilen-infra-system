/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.junits;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.TimerEventContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.TimerEventHandler;
import com.foilen.infra.plugin.v1.core.exception.InfiniteUpdateLoop;
import com.foilen.infra.plugin.v1.core.exception.ResourcePrimaryKeyCollisionException;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionProvider;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionV1;
import com.foilen.infra.plugin.v1.core.resource.IPResourceQuery;
import com.foilen.infra.plugin.v1.core.service.IPPluginService;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.foilen.infra.resource.example.AbstractParent;
import com.foilen.infra.resource.example.ConcreteLevel1;
import com.foilen.infra.resource.example.ConcreteLevel2;
import com.foilen.infra.resource.example.EmployeeResource;
import com.foilen.infra.resource.example.JunitResource;
import com.foilen.infra.resource.example.JunitResourceEnum;
import com.foilen.infra.resource.example.failing.CrashingTimerEventHandler;
import com.foilen.infra.resource.testing.controller.TestingControllerPluginDefinitionProvider;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.DateTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.ThreadTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.foilen.smalltools.tuple.Tuple3;
import com.google.common.collect.Sets;

/**
 * This is to test that the implementation of the real system is working as expected.
 */
public abstract class AbstractIPResourceServiceTest extends AbstractBasics {

    // TODO Add new tests

    static private interface ApplyQuery<R extends IPResource> {
        void apply(IPResourceQuery<R> resourceQuery);
    }

    private static class CounterTimerEventHandler implements TimerEventHandler {

        private AtomicInteger count;

        public CounterTimerEventHandler(AtomicInteger count) {
            this.count = count;
        }

        @Override
        public void timerHandler(CommonServicesContext services, ChangesContext changes, TimerEventContext event) {
            count.incrementAndGet();
        }

    }

    public static final String RESOURCE_ID_FIELD = "_resourceId";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Execute the editor with the form.
     *
     * @param internalId
     *            (optional) the internal id of the existing resource
     * @param resourceEditor
     *            the resource editor
     * @param formValues
     *            the form
     * @param <T>
     *            resource type
     */
    @SuppressWarnings("unchecked")
    protected <T extends IPResource> void assertEditorNoErrors(Long internalId, ResourceEditor<T> resourceEditor, Map<String, String> formValues) {
        try {
            // Format, validate
            if (internalId != null) {
                formValues.put(RESOURCE_ID_FIELD, String.valueOf(internalId));
            }
            resourceEditor.formatForm(getCommonServicesContext(), formValues);
            List<Tuple2<String, String>> errors = resourceEditor.validateForm(getCommonServicesContext(), formValues);
            if (!errors.isEmpty()) {
                System.out.println(JsonTools.prettyPrint(errors));
            }
            Assert.assertTrue(errors.isEmpty());

            // Create or get
            T resource;
            if (internalId == null) {
                resource = resourceEditor.getForResourceType().getConstructor().newInstance();
            } else {
                resource = (T) getCommonServicesContext().getResourceService().resourceFind(internalId).get();
            }

            // Fill
            ChangesContext changesContext = new ChangesContext(getCommonServicesContext().getResourceService());
            resourceEditor.fillResource(getCommonServicesContext(), changesContext, formValues, resource);

            // Add or update
            if (internalId == null) {
                changesContext.resourceAdd(resource);
            } else {
                changesContext.resourceUpdate(internalId, resource);
            }

            // Execute the change
            getInternalServicesContext().getInternalChangeService().changesExecute(changesContext);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Got an exception");
        }
    }

    /**
     * Execute the editor with the form and fails.
     *
     * @param internalId
     *            (optional) the internal id of the existing resource
     * @param resourceEditor
     *            the resource editor
     * @param formValues
     *            the form
     * @param expectedErrors
     *            check for these errors
     * @param <T>
     *            the resource type
     */
    @SafeVarargs
    protected final <T extends IPResource> void assertEditorWithErrors(Long internalId, ResourceEditor<T> resourceEditor, Map<String, String> formValues, Tuple2<String, String>... expectedErrors) {

        // Format, validate
        if (internalId != null) {
            formValues.put(RESOURCE_ID_FIELD, String.valueOf(internalId));
        }
        resourceEditor.formatForm(getCommonServicesContext(), formValues);
        List<Tuple2<String, String>> errors = resourceEditor.validateForm(getCommonServicesContext(), formValues);

        AssertTools.assertJsonComparison(Arrays.asList(expectedErrors), errors);

    }

    private <R extends IPResource> void assertLinkFromAllNames(Class<R> resourceClass, String linkType, IPResource resourceTo, String... expectedNames) {
        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<String> actualNames = resourceService.linkFindAllByFromResourceClassAndLinkTypeAndToResource(resourceClass, linkType, resourceTo).stream() //
                .map(it -> it.getResourceName()) //
                .sorted() //
                .collect(Collectors.toList());
        List<String> eNames = Arrays.asList(expectedNames).stream() //
                .sorted() //
                .collect(Collectors.toList());

        Assert.assertEquals(eNames, actualNames);
    }

    private <R extends IPResource> void assertLinkToAllNames(Class<R> resourceClass, String linkType, IPResource resourceFrom, String... expectedNames) {
        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<String> actualNames = resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resourceFrom, linkType, resourceClass).stream() //
                .map(it -> it.getResourceName()) //
                .sorted() //
                .collect(Collectors.toList());
        List<String> eNames = Arrays.asList(expectedNames).stream() //
                .sorted() //
                .collect(Collectors.toList());

        Assert.assertEquals(eNames, actualNames);
    }

    public void assertResourceCount(int expectedCount, Class<? extends IPResource> resourceType) {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<? extends IPResource> actualResources = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(resourceType) //
        );
        logger.info("Found {} resources of type {}; expecting {}", actualResources.size(), resourceType, expectedCount);
        for (IPResource actualResource : actualResources) {
            logger.debug("Actual: {}", actualResource);
        }

        Assert.assertEquals(expectedCount, actualResources.size());
    }

    public <T extends IPResource> T assertResourceExists(boolean expectedExists, T resourcePk, Class<T> resourceClass) {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        Optional<T> resourceFindOne = resourceService.resourceFindByPk(resourcePk);
        Assert.assertEquals(expectedExists, resourceFindOne.isPresent());

        if (resourceFindOne.isPresent()) {
            return resourceFindOne.get();
        } else {
            return null;
        }
    }

    private <R extends IPResource> void assertResourceFindAllNames(Class<R> resourceClass, ApplyQuery<R> applyQuery, String... expectedNames) {
        IPResourceService resourceService = getCommonServicesContext().getResourceService();
        IPResourceQuery<R> resourceQuery = resourceService.createResourceQuery(resourceClass);
        applyQuery.apply(resourceQuery);

        List<String> actualNames = resourceService.resourceFindAll(resourceQuery).stream() //
                .map(it -> it.getResourceName()) //
                .sorted() //
                .collect(Collectors.toList());
        List<String> eNames = Arrays.asList(expectedNames).stream() //
                .sorted() //
                .collect(Collectors.toList());

        Assert.assertEquals(eNames, actualNames);
    }

    private void assertSet(Set<String> actualTags, String... expectedTags) {
        Assert.assertEquals(expectedTags.length, actualTags.size());
        Assert.assertTrue(actualTags.containsAll(Arrays.asList(expectedTags)));
    }

    @Before
    public void beforeEach() {

        List<? extends IPResource> all = getInternalServicesContext().getInternalIPResourceService().resourceFindAll();
        AssertTools.assertJsonComparison(Collections.emptyList(), all);

        TestingControllerPluginDefinitionProvider.getInstance().getTestingControllerInfiniteLoopChangesEventHandler().setAlwaysUpdate(false);

        JunitsHelper.createFakeData(getCommonServicesContext(), getInternalServicesContext());
    }

    private void deleteAllResources() {
        ChangesContext changes = new ChangesContext(getCommonServicesContext().getResourceService());
        for (IPResource resource : getInternalServicesContext().getInternalIPResourceService().resourceFindAll()) {
            changes.resourceDelete(resource);
        }
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);
    }

    protected abstract CommonServicesContext getCommonServicesContext();

    protected abstract InternalServicesContext getInternalServicesContext();

    @Test
    public void testBrokenPlugin() {
        IPPluginService ipPluginService = getCommonServicesContext().getPluginService();

        List<Tuple3<Class<? extends IPPluginDefinitionProvider>, IPPluginDefinitionV1, String>> broken = ipPluginService.getBrokenPlugins();
        Assert.assertEquals(1, broken.size());
        Assert.assertEquals("com.foilen.infra.resource.example.failing.FoilenExampleFailingPluginDefinitionProvider", broken.get(0).getA().getName());
    }

    @Test
    public void testBrokenTimerEvent() {

        AtomicInteger okCounter = new AtomicInteger();

        getCommonServicesContext().getTimerService().timerAdd(new TimerEventContext(new CrashingTimerEventHandler(), "Crashing", Calendar.MILLISECOND, 500, false, true));
        getCommonServicesContext().getTimerService().timerAdd(new TimerEventContext( //
                (services, changes, event) -> okCounter.incrementAndGet(), //
                "OK", //
                Calendar.MILLISECOND, //
                700, //
                false, //
                true));

        ThreadTools.sleep(3000);

        Assert.assertTrue(okCounter.get() >= 2);

    }

    @Test
    public void testChanges_links() {

        // Common
        List<JunitResource> entries;
        JunitResource resource;
        ChangesContext changes = new ChangesContext(getCommonServicesContext().getResourceService());
        InternalChangeService internalChangeService = getInternalServicesContext().getInternalChangeService();
        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Delete all
        entries = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class));
        for (JunitResource entry : entries) {
            changes.resourceDelete(entry.getInternalId());
        }
        internalChangeService.changesExecute(changes);

        entries = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class));
        Assert.assertTrue(entries.isEmpty());

        // Create some linked together
        JunitResource masterResource = new JunitResource("theMaster");
        changes.resourceAdd(masterResource);

        JunitResource slaveResource = new JunitResource("slave1");
        changes.resourceAdd(slaveResource);
        changes.linkAdd(masterResource, "COMMANDS", slaveResource);

        slaveResource = new JunitResource("slave2");
        changes.resourceAdd(slaveResource);
        changes.linkAdd(masterResource, "COMMANDS", slaveResource);

        changes.linkAdd(slaveResource, "LIKES", masterResource);

        internalChangeService.changesExecute(changes);

        // Get the master and check
        resource = resourceService.resourceFindByPk(new JunitResource("theMaster")).get();
        masterResource = resourceService.resourceFindByPk(masterResource).get();
        List<Tuple2<String, ? extends IPResource>> links = resourceService.linkFindAllByFromResource(masterResource);

        Assert.assertEquals("theMaster", resource.getText());
        Assert.assertEquals(2, links.size());
        for (Tuple2<String, ? extends IPResource> usingResource : links) {
            Assert.assertEquals("COMMANDS", usingResource.getA());
        }
        Assert.assertEquals(Arrays.asList("slave1", "slave2"), links.stream().map(it -> ((JunitResource) it.getB()).getText()).sorted().collect(Collectors.toList()));

        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkType(resource, "LIKES").size());
        Assert.assertEquals(2, resourceService.linkFindAllByFromResourceAndLinkType(resource, "COMMANDS").size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "LIKES", JunitResource.class).size());
        Assert.assertEquals(2, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "COMMANDS", JunitResource.class).size());

        // Get the slave1 and check
        resource = resourceService.resourceFindByPk(new JunitResource("slave1")).get();

        Assert.assertEquals("slave1", resource.getText());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResource(resource).size());

        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkType(resource, "LIKES").size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkType(resource, "COMMANDS").size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "LIKES", JunitResource.class).size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "COMMANDS", JunitResource.class).size());

        // Get the slave2 and check
        resource = resourceService.resourceFindByPk(new JunitResource("slave2")).get();

        Assert.assertEquals("slave2", resource.getText());
        Assert.assertEquals(1, resourceService.linkFindAllByFromResource(resource).size());
        for (Tuple2<String, ? extends IPResource> usingResource : resourceService.linkFindAllByFromResource(resource)) {
            Assert.assertEquals("LIKES", usingResource.getA());
        }
        Assert.assertEquals(Arrays.asList("theMaster"),
                resourceService.linkFindAllByFromResource(resource).stream().map(it -> ((JunitResource) it.getB()).getText()).sorted().collect(Collectors.toList()));

        Assert.assertEquals(1, resourceService.linkFindAllByFromResourceAndLinkType(resource, "LIKES").size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkType(resource, "LOVES").size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkType(resource, "COMMANDS").size());
        Assert.assertEquals(1, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "LIKES", JunitResource.class).size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "LOVES", JunitResource.class).size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "COMMANDS", JunitResource.class).size());

        // Update the slave2 links (append)
        masterResource = resourceService.resourceFindByPk(new JunitResource("theMaster")).get();

        slaveResource = new JunitResource("slave2");
        changes.linkAdd(slaveResource, "LOVES", masterResource);

        internalChangeService.changesExecute(changes);

        // Get the slave2 and check
        resource = resourceService.resourceFindByPk(new JunitResource("slave2")).get();

        Assert.assertEquals("slave2", resource.getText());
        Assert.assertEquals(2, resourceService.linkFindAllByFromResource(resource).size());
        Assert.assertEquals(Arrays.asList("LIKES", "LOVES"), resourceService.linkFindAllByFromResource(resource).stream().map(it -> it.getA()).sorted().collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList("theMaster", "theMaster"),
                resourceService.linkFindAllByFromResource(resource).stream().map(it -> ((JunitResource) it.getB()).getText()).sorted().collect(Collectors.toList()));

        Assert.assertEquals(1, resourceService.linkFindAllByFromResourceAndLinkType(resource, "LIKES").size());
        Assert.assertEquals(1, resourceService.linkFindAllByFromResourceAndLinkType(resource, "LOVES").size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkType(resource, "COMMANDS").size());
        Assert.assertEquals(1, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "LIKES", JunitResource.class).size());
        Assert.assertEquals(1, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "LOVES", JunitResource.class).size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "COMMANDS", JunitResource.class).size());

        // Update the slave2 links (overwrite)
        slaveResource = resourceService.resourceFindByPk(new JunitResource("slave2")).get();
        for (Tuple2<String, ? extends IPResource> link : resourceService.linkFindAllByFromResource(slaveResource)) {
            changes.linkDelete(slaveResource, link.getA(), link.getB());
        }
        changes.linkAdd(slaveResource, "DISLIKE", masterResource);

        internalChangeService.changesExecute(changes);

        // Get the slave2 and check
        resource = resourceService.resourceFindByPk(new JunitResource("slave2")).get();

        Assert.assertEquals("slave2", resource.getText());
        Assert.assertEquals(1, resourceService.linkFindAllByFromResource(resource).size());
        Assert.assertEquals(Arrays.asList("DISLIKE"), resourceService.linkFindAllByFromResource(resource).stream().map(it -> it.getA()).sorted().collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList("theMaster"),
                resourceService.linkFindAllByFromResource(resource).stream().map(it -> ((JunitResource) it.getB()).getText()).sorted().collect(Collectors.toList()));

        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkType(resource, "LIKES").size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkType(resource, "LOVES").size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkType(resource, "COMMANDS").size());
        Assert.assertEquals(1, resourceService.linkFindAllByFromResourceAndLinkType(resource, "DISLIKE").size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "LIKES", JunitResource.class).size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "LOVES", JunitResource.class).size());
        Assert.assertEquals(0, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "COMMANDS", JunitResource.class).size());
        Assert.assertEquals(1, resourceService.linkFindAllByFromResourceAndLinkTypeAndToResourceClass(resource, "DISLIKE", JunitResource.class).size());

    }

    @Test
    public void testChanges_linksAndTagsAreNotKeptWhenDeleted() {

        // Create
        ChangesContext changes = new ChangesContext(getCommonServicesContext().getResourceService());
        JunitResource r1 = new JunitResource("1");
        JunitResource r2 = new JunitResource("2");
        changes.resourceAdd(r1);
        changes.resourceAdd(r2);
        changes.linkAdd(r1, "link1", r2);
        changes.tagAdd(r1, "tag1");
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        IPResourceService resourceService = getCommonServicesContext().getResourceService();
        r1 = resourceService.resourceFindByPk(r1).get();
        r2 = resourceService.resourceFindByPk(r2).get();
        Assert.assertEquals(Arrays.asList("link1"), resourceService.linkFindAllByFromResource(r1).stream().map(it -> it.getA()).sorted().collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList("tag1"), resourceService.tagFindAllByResource(r1).stream().sorted().collect(Collectors.toList()));

        // Delete it
        changes.resourceDelete(resourceService.resourceFindByPk(r1).get().getInternalId());
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        Assert.assertEquals(0, resourceService.linkFindAllByFromResource(r1).size());
        Assert.assertEquals(0, resourceService.tagFindAllByResource(r1).size());

        // Recreate it
        changes.resourceAdd(r1);
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        // Check no links and tags
        Assert.assertEquals(0, resourceService.linkFindAllByFromResource(r1).size());
        Assert.assertEquals(0, resourceService.tagFindAllByResource(r1).size());

    }

    @Test
    public void testChanges_reduntantLinksAndTags() {

        ChangesContext changes = new ChangesContext(getCommonServicesContext().getResourceService());
        JunitResource r1 = new JunitResource("reduntant_1");
        JunitResource r2 = new JunitResource("reduntant_2");
        changes.resourceAdd(r1);
        changes.resourceAdd(r2);
        changes.linkAdd(r1, "link1", r2);
        changes.linkAdd(r1, "link1", r2);
        changes.linkAdd(r1, "link2", r2);
        changes.tagAdd(r1, "tag1");
        changes.tagAdd(r1, "tag1");
        changes.tagAdd(r1, "tag2");
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        // Check
        IPResourceService resourceService = getCommonServicesContext().getResourceService();
        r1 = resourceService.resourceFindByPk(r1).get();
        r2 = resourceService.resourceFindByPk(r2).get();
        Assert.assertEquals(Arrays.asList("link1", "link2"), resourceService.linkFindAllByFromResource(r1).stream().map(it -> it.getA()).sorted().collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList("tag1", "tag2"), resourceService.tagFindAllByResource(r1).stream().sorted().collect(Collectors.toList()));

    }

    @Test
    public void testChanges_rollback() {
        // TOOD + testChanges_rollback
    }

    @Test
    public void testChanges_tags() {
        // Common
        List<JunitResource> entries;
        JunitResource resource;
        List<JunitResource> resources;
        Set<String> tags;
        ChangesContext changes = new ChangesContext(getCommonServicesContext().getResourceService());
        InternalChangeService internalChangeService = getInternalServicesContext().getInternalChangeService();
        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Must have some
        entries = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class));
        Assert.assertFalse(entries.isEmpty());

        // Delete all
        for (JunitResource entry : entries) {
            changes.resourceDelete(entry.getInternalId());
        }
        internalChangeService.changesExecute(changes);

        entries = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class));
        Assert.assertTrue(entries.isEmpty());

        // Create 2
        resource = new JunitResource("example.com", JunitResourceEnum.A, 1);
        changes.resourceAdd(resource);
        changes.tagAdd(resource, "tag1");
        changes.tagAdd(resource, "asite");

        resource = new JunitResource("www.example.com", JunitResourceEnum.A, 1);
        changes.resourceAdd(resource);
        changes.tagAdd(resource, "asite");
        internalChangeService.changesExecute(changes);

        entries = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class));
        Assert.assertEquals(2, entries.size());

        // Update add tags
        resource = resourceService.resourceFindByPk(new JunitResource("example.com", JunitResourceEnum.A, 1)).get();
        changes.tagAdd(resource, "changed");
        changes.resourceUpdate(resource.getInternalId(), new JunitResource("example2.com", JunitResourceEnum.A, 2));
        internalChangeService.changesExecute(changes);

        entries = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class));
        Assert.assertEquals(2, entries.size());
        resources = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class).tagAddAnd("changed"));
        Assert.assertEquals(1, resources.size());
        resource = resources.get(0);
        tags = resourceService.tagFindAllByResource(resource);
        assertSet(tags, "tag1", "asite", "changed");

        Assert.assertEquals("example2.com", resource.getText());
        Assert.assertEquals((Integer) 2, resource.getIntegerNumber());

        // Update remove tags
        resource = resourceService.resourceFindByPk(new JunitResource("example2.com", JunitResourceEnum.A, 2)).get();
        changes.tagDelete(resource, "tag1");
        internalChangeService.changesExecute(changes);

        entries = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class));
        Assert.assertEquals(2, entries.size());
        resources = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class).tagAddAnd("changed"));
        Assert.assertEquals(1, resources.size());
        resource = resources.get(0);
        tags = resourceService.tagFindAllByResource(resource);
        assertSet(tags, "asite", "changed");

        Assert.assertEquals("example2.com", resource.getText());

        // Add existing items (fail)
        try {
            changes.resourceAdd(new JunitResource("example2.com", JunitResourceEnum.A, 2));
            internalChangeService.changesExecute(changes);
            Assert.fail("Didn't get an exception");
        } catch (ResourcePrimaryKeyCollisionException e) {

        }

        // Update non existing items (fail)
        try {
            resource = resourceService.resourceFindByPk(new JunitResource("not.existing.com", JunitResourceEnum.A, 1)).get();
            changes.resourceUpdate(resource.getInternalId(), new JunitResource("example2.com", JunitResourceEnum.A, 5));
            internalChangeService.changesExecute(changes);
            Assert.fail("Didn't get an exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testDuplicatePkSameResource_create() {
        // Common
        JunitResource resource;
        ChangesContext changes = new ChangesContext(getCommonServicesContext().getResourceService());
        InternalChangeService internalChangeService = getInternalServicesContext().getInternalChangeService();

        // Create 1 item
        resource = new JunitResource("t1", JunitResourceEnum.A, 1);
        resource.setLongNumber(10L);
        changes.resourceAdd(resource);
        internalChangeService.changesExecute(changes);

        // Create same. Not fine
        thrown.expect(ResourcePrimaryKeyCollisionException.class);
        resource = new JunitResource("t1", JunitResourceEnum.A, 1);
        resource.setLongNumber(30L);
        changes.resourceAdd(resource);
        internalChangeService.changesExecute(changes);

    }

    @Test
    public void testDuplicatePkSameResource_update() {
        // Common
        JunitResource resource;
        ChangesContext changes = new ChangesContext(getCommonServicesContext().getResourceService());
        InternalChangeService internalChangeService = getInternalServicesContext().getInternalChangeService();
        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Create 2 items
        resource = new JunitResource("t1", JunitResourceEnum.A, 1);
        resource.setLongNumber(10L);
        changes.resourceAdd(resource);
        resource = new JunitResource("t2", JunitResourceEnum.A, 2);
        resource.setLongNumber(10L);
        changes.resourceAdd(resource);
        internalChangeService.changesExecute(changes);

        // Rename second item to same pk as first
        thrown.expect(ResourcePrimaryKeyCollisionException.class);
        resource = new JunitResource("t1", JunitResourceEnum.A, 1);
        resource.setLongNumber(20L);
        changes.resourceUpdate(resourceService.resourceFindByPk(new JunitResource("t2", JunitResourceEnum.A, 2)).get().getInternalId(), resource);
        internalChangeService.changesExecute(changes);

    }

    @Test(timeout = 30000)
    public void testInfiniteLoop() {

        thrown.expect(InfiniteUpdateLoop.class);

        TestingControllerPluginDefinitionProvider.getInstance().getTestingControllerInfiniteLoopChangesEventHandler().setAlwaysUpdate(true);

        ChangesContext changes = new ChangesContext(getCommonServicesContext().getResourceService());
        JunitResource resource = new JunitResource("OneToGetStarted");
        changes.resourceAdd(resource);

        getInternalServicesContext().getInternalChangeService().changesExecute(changes);
    }

    @Test
    public void testMultiLevelResources() {

        ChangesContext changes = new ChangesContext(getCommonServicesContext().getResourceService());
        InternalChangeService internalChangeService = getInternalServicesContext().getInternalChangeService();
        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Create some resources of level 1 and 2
        ConcreteLevel1 cl11 = new ConcreteLevel1("L1-1", "PA", "AA");
        ConcreteLevel1 cl12 = new ConcreteLevel1("L1-2", "PB", "AB");
        ConcreteLevel2 cl21 = new ConcreteLevel2("L2-1", "PA", "AA", "AAA");
        ConcreteLevel2 cl22 = new ConcreteLevel2("L2-2", "PB", "AB", "BBB");
        JunitResource j = new JunitResource("one");
        changes.resourceAdd(cl11);
        changes.resourceAdd(cl12);
        changes.resourceAdd(cl21);
        changes.resourceAdd(cl22);
        changes.resourceAdd(j);

        changes.linkAdd(j, LinkTypeConstants.USES, cl11);
        changes.linkAdd(j, LinkTypeConstants.USES, cl22);
        changes.linkAdd(cl12, LinkTypeConstants.USES, j);
        changes.linkAdd(cl21, LinkTypeConstants.USES, j);

        internalChangeService.changesExecute(changes);

        // Search resource for parent
        assertResourceFindAllNames(AbstractParent.class, resourceQuery -> resourceQuery.propertyEquals(AbstractParent.PROPERTY_ON_PARENT, "PA"), "L1-1", "L2-1");

        // Search resource for level 1
        assertResourceFindAllNames(ConcreteLevel1.class, resourceQuery -> resourceQuery.propertyEquals(AbstractParent.PROPERTY_ON_PARENT, "PA"), "L1-1", "L2-1");
        assertResourceFindAllNames(ConcreteLevel1.class, resourceQuery -> resourceQuery.propertyEquals(ConcreteLevel1.PROPERTY_ON_LEVEL_1, "AB"), "L1-2", "L2-2");

        // Search resource for level 2
        assertResourceFindAllNames(ConcreteLevel2.class, resourceQuery -> resourceQuery.propertyEquals(AbstractParent.PROPERTY_ON_PARENT, "PA"), "L2-1");
        assertResourceFindAllNames(ConcreteLevel2.class, resourceQuery -> resourceQuery.propertyEquals(ConcreteLevel1.PROPERTY_ON_LEVEL_1, "AB"), "L2-2");
        assertResourceFindAllNames(ConcreteLevel2.class, resourceQuery -> resourceQuery.propertyEquals(ConcreteLevel2.PROPERTY_ON_LEVEL_2, "AAA"), "L2-1");

        // Search link from parent
        j = resourceService.resourceFindByPk(j).get();
        assertLinkFromAllNames(AbstractParent.class, LinkTypeConstants.USES, j, "L1-2", "L2-1");

        // Search link from level 1
        assertLinkFromAllNames(ConcreteLevel1.class, LinkTypeConstants.USES, j, "L1-2", "L2-1");

        // Search link from level 2
        assertLinkFromAllNames(ConcreteLevel2.class, LinkTypeConstants.USES, j, "L2-1");

        // Search link to parent
        assertLinkToAllNames(AbstractParent.class, LinkTypeConstants.USES, j, "L1-1", "L2-2");

        // Search link to level 1
        assertLinkToAllNames(ConcreteLevel1.class, LinkTypeConstants.USES, j, "L1-1", "L2-2");

        // Search link to level 2
        assertLinkToAllNames(ConcreteLevel2.class, LinkTypeConstants.USES, j, "L2-2");

    }

    @Test
    public void testQuery_SetPropertyTwice() {

        thrown.expectMessage("Property [text] already has a value to check for equals");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .primaryKeyEquals(new JunitResource("www.example.com", JunitResourceEnum.A, 1)) //
                .propertyEquals(JunitResource.PROPERTY_TEXT, "random");

    }

    @Test
    public void testQuery_tagsBoth_1() {

        thrown.expectMessage("There can be only tags check as AND or OR, but not both at the same time");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .tagAddAnd("a", "b") //
                .tagAddOr("c");

    }

    @Test
    public void testQuery_tagsBoth_2() {

        thrown.expectMessage("There can be only tags check as AND or OR, but not both at the same time");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .tagAddOr("c") //
                .tagAddAnd("a", "b");

    }

    @Test
    public void testQuery_UnexistingProperty() {

        thrown.expectMessage("Property [nope] does not exists");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyEquals("nope", "random");

    }

    @Test
    public void testQueryBoolean_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // false
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_BOOL, false) //
        );
        Assert.assertEquals(4, items.size());
        Assert.assertEquals("www.example.com", items.get(0).getText());
        Assert.assertEquals("www.example.com", items.get(1).getText());
        Assert.assertEquals("example.com", items.get(2).getText());
        Assert.assertEquals("t2_aaa", items.get(3).getText());

        // true
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_BOOL, true) //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("t1_aaa", items.get(0).getText());
        Assert.assertEquals("zz", items.get(1).getText());

    }

    @Test
    public void testQueryBoolean_greater_equal_no() {

        thrown.expectMessage("Property [bool] does not support querying greater or equal");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyGreaterAndEquals(JunitResource.PROPERTY_BOOL, true);

    }

    @Test
    public void testQueryBoolean_greater_no() {

        thrown.expectMessage("Property [bool] does not support querying greater");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyGreater(JunitResource.PROPERTY_BOOL, true);

    }

    @Test
    public void testQueryBoolean_less_equal_no() {

        thrown.expectMessage("Property [bool] does not support querying lesser or equal");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLesserAndEquals(JunitResource.PROPERTY_BOOL, true);

    }

    @Test
    public void testQueryBoolean_less_no() {

        thrown.expectMessage("Property [bool] does not support querying lesser");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLesser(JunitResource.PROPERTY_BOOL, true);

    }

    @Test
    public void testQueryBoolean_like_no() {

        thrown.expectMessage("Property [bool] does not support querying like");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLike(JunitResource.PROPERTY_BOOL, "%true");

    }

    @Test
    public void testQueryDate_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_DATE, DateTools.parseFull("2000-04-01 00:00:00")) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("zz", items.get(0).getText());

    }

    @Test
    public void testQueryDate_greater_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyGreaterAndEquals(JunitResource.PROPERTY_DATE, DateTools.parseFull("2000-04-01 00:00:00")) //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("t2_aaa", items.get(0).getText());
        Assert.assertEquals("zz", items.get(1).getText());

    }

    @Test
    public void testQueryDate_greater_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyGreater(JunitResource.PROPERTY_DATE, DateTools.parseFull("2000-04-01 00:00:00")) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("t2_aaa", items.get(0).getText());

    }

    @Test
    public void testQueryDate_less_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLesserAndEquals(JunitResource.PROPERTY_DATE, DateTools.parseFull("2000-04-01 00:00:00")) //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("t1_aaa", items.get(0).getText());
        Assert.assertEquals("zz", items.get(1).getText());

    }

    @Test
    public void testQueryDate_less_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLesser(JunitResource.PROPERTY_DATE, DateTools.parseFull("2000-04-01 00:00:00")) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("t1_aaa", items.get(0).getText());

    }

    @Test
    public void testQueryDate_like_no() {

        thrown.expectMessage("Property [date] does not support querying like");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLike(JunitResource.PROPERTY_DATE, "%");

    }

    @Test
    public void testQueryDouble_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_DOUBLE_NUMBER, 1.5) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("t2_aaa", items.get(0).getText());

    }

    @Test
    public void testQueryDouble_greater_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyGreaterAndEquals(JunitResource.PROPERTY_DOUBLE_NUMBER, 1.5) //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("t2_aaa", items.get(0).getText());
        Assert.assertEquals("zz", items.get(1).getText());

    }

    @Test
    public void testQueryDouble_greater_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyGreater(JunitResource.PROPERTY_DOUBLE_NUMBER, 1.5) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("zz", items.get(0).getText());

    }

    @Test
    public void testQueryDouble_less_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLesserAndEquals(JunitResource.PROPERTY_DOUBLE_NUMBER, 1.5) //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("t1_aaa", items.get(0).getText());
        Assert.assertEquals("t2_aaa", items.get(1).getText());

    }

    @Test
    public void testQueryDouble_less_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLesser(JunitResource.PROPERTY_DOUBLE_NUMBER, 1.5) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("t1_aaa", items.get(0).getText());

    }

    @Test
    public void testQueryDouble_like_no() {

        thrown.expectMessage("Property [doubleNumber] does not support querying like");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLike(JunitResource.PROPERTY_DOUBLE_NUMBER, "%10.0");

    }

    @Test
    public void testQueryEditorName() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();
        InternalChangeService internalChangeService = getInternalServicesContext().getInternalChangeService();

        // Get the initial id
        Optional<JunitResource> junitResourceOptional = resourceService.resourceFind(resourceService.createResourceQuery(JunitResource.class) //
                .propertyContains(JunitResource.PROPERTY_SET_TEXTS, Arrays.asList("two"))//
        );
        Assert.assertTrue(junitResourceOptional.isPresent());
        long expectedId = junitResourceOptional.get().getInternalId();

        // Modify its editor
        JunitResource resource = junitResourceOptional.get();
        ChangesContext changes = new ChangesContext(getCommonServicesContext().getResourceService());
        resource.setResourceEditorName("junit");
        changes.resourceUpdate(expectedId, resource);
        internalChangeService.changesExecute(changes);

        // Get the resource
        junitResourceOptional = resourceService.resourceFind(resourceService.createResourceQuery(JunitResource.class) //
                .addEditorEquals("junit") //
        );
        Assert.assertTrue(junitResourceOptional.isPresent());
        Assert.assertEquals((Long) expectedId, junitResourceOptional.get().getInternalId());

        // Don't get the resource if wrong editor
        junitResourceOptional = resourceService.resourceFind(resourceService.createResourceQuery(JunitResource.class) //
                .addEditorEquals("not junit") //
        );
        Assert.assertFalse(junitResourceOptional.isPresent());

    }

    @Test
    public void testQueryEnum_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_ENUMERATION, JunitResourceEnum.B) //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("example.com", items.get(0).getText());
        Assert.assertEquals("zz", items.get(1).getText());

    }

    @Test
    public void testQueryEnum_greater_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyGreaterAndEquals(JunitResource.PROPERTY_ENUMERATION, JunitResourceEnum.B) //
        );
        Assert.assertEquals(3, items.size());
        Assert.assertEquals("example.com", items.get(0).getText());
        Assert.assertEquals("t2_aaa", items.get(1).getText());
        Assert.assertEquals("zz", items.get(2).getText());

    }

    @Test
    public void testQueryEnum_greater_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyGreater(JunitResource.PROPERTY_ENUMERATION, JunitResourceEnum.B) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("t2_aaa", items.get(0).getText());

    }

    @Test
    public void testQueryEnum_less_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLesserAndEquals(JunitResource.PROPERTY_ENUMERATION, JunitResourceEnum.B) //
        );
        Assert.assertEquals(5, items.size());
        Assert.assertEquals("www.example.com", items.get(0).getText());
        Assert.assertEquals("www.example.com", items.get(1).getText());
        Assert.assertEquals("example.com", items.get(2).getText());
        Assert.assertEquals("t1_aaa", items.get(3).getText());
        Assert.assertEquals("zz", items.get(4).getText());

    }

    @Test
    public void testQueryEnum_less_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLesser(JunitResource.PROPERTY_ENUMERATION, JunitResourceEnum.B) //
        );
        Assert.assertEquals(3, items.size());
        Assert.assertEquals("www.example.com", items.get(0).getText());
        Assert.assertEquals("www.example.com", items.get(1).getText());
        Assert.assertEquals("t1_aaa", items.get(2).getText());

    }

    @Test
    public void testQueryEnum_like_no() {

        thrown.expectMessage("Property [enumeration] does not support querying like");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLike(JunitResource.PROPERTY_ENUMERATION, "%" + JunitResourceEnum.B);

    }

    @Test
    public void testQueryFloat_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_FLOAT_NUMBER, 3.1f) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("zz", items.get(0).getText());

    }

    @Test
    public void testQueryFloat_greater_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyGreaterAndEquals(JunitResource.PROPERTY_FLOAT_NUMBER, 3.1f) //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("t2_aaa", items.get(0).getText());
        Assert.assertEquals("zz", items.get(1).getText());

    }

    @Test
    public void testQueryFloat_greater_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyGreater(JunitResource.PROPERTY_FLOAT_NUMBER, 3.1f) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("t2_aaa", items.get(0).getText());

    }

    @Test
    public void testQueryFloat_less_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLesserAndEquals(JunitResource.PROPERTY_FLOAT_NUMBER, 3.1f) //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("t1_aaa", items.get(0).getText());
        Assert.assertEquals("zz", items.get(1).getText());

    }

    @Test
    public void testQueryFloat_less_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLesser(JunitResource.PROPERTY_FLOAT_NUMBER, 3.1f) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("t1_aaa", items.get(0).getText());

    }

    @Test
    public void testQueryFloat_like_no() {

        thrown.expectMessage("Property [floatNumber] does not support querying like");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLike(JunitResource.PROPERTY_FLOAT_NUMBER, "%10.0");

    }

    @Test
    public void testQueryIds() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Get the initial id
        Optional<JunitResource> junitResourceOptional = resourceService.resourceFind(resourceService.createResourceQuery(JunitResource.class) //
                .propertyContains(JunitResource.PROPERTY_SET_TEXTS, Arrays.asList("two"))//
        );
        Assert.assertTrue(junitResourceOptional.isPresent());
        long expectedId = junitResourceOptional.get().getInternalId();

        // Get the resource
        junitResourceOptional = resourceService.resourceFind(resourceService.createResourceQuery(JunitResource.class) //
                .addIdEquals(expectedId) //
        );
        Assert.assertTrue(junitResourceOptional.isPresent());
        Assert.assertEquals((Long) expectedId, junitResourceOptional.get().getInternalId());

        // Don't get it if not the right type
        Optional<EmployeeResource> employeeResourceOptional = resourceService.resourceFind(resourceService.createResourceQuery(EmployeeResource.class) //
                .addIdEquals(expectedId) //
        );
        Assert.assertFalse(employeeResourceOptional.isPresent());

    }

    @Test
    public void testQueryInteger_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_INTEGER_NUMBER, 5) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("t2_aaa", items.get(0).getText());

    }

    @Test
    public void testQueryInteger_greater_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyGreaterAndEquals(JunitResource.PROPERTY_INTEGER_NUMBER, 5) //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("t2_aaa", items.get(0).getText());
        Assert.assertEquals("zz", items.get(1).getText());

    }

    @Test
    public void testQueryInteger_greater_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyGreater(JunitResource.PROPERTY_INTEGER_NUMBER, 5) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("zz", items.get(0).getText());

    }

    @Test
    public void testQueryInteger_less_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLesserAndEquals(JunitResource.PROPERTY_INTEGER_NUMBER, 5) //
        );
        Assert.assertEquals(5, items.size());
        Assert.assertEquals("www.example.com", items.get(0).getText());
        Assert.assertEquals("www.example.com", items.get(1).getText());
        Assert.assertEquals("example.com", items.get(2).getText());
        Assert.assertEquals("t1_aaa", items.get(3).getText());
        Assert.assertEquals("t2_aaa", items.get(4).getText());

    }

    @Test
    public void testQueryInteger_less_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLesser(JunitResource.PROPERTY_INTEGER_NUMBER, 5) //
        );
        Assert.assertEquals(4, items.size());
        Assert.assertEquals("www.example.com", items.get(0).getText());
        Assert.assertEquals("www.example.com", items.get(1).getText());
        Assert.assertEquals("example.com", items.get(2).getText());
        Assert.assertEquals("t1_aaa", items.get(3).getText());

    }

    @Test
    public void testQueryInteger_like_no() {

        thrown.expectMessage("Property [integerNumber] does not support querying like");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLike(JunitResource.PROPERTY_INTEGER_NUMBER, "%10");

    }

    @Test
    public void testQueryLong_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_LONG_NUMBER, 4L) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("zz", items.get(0).getText());

    }

    @Test
    public void testQueryLong_greater_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyGreaterAndEquals(JunitResource.PROPERTY_LONG_NUMBER, 4L) //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("t2_aaa", items.get(0).getText());
        Assert.assertEquals("zz", items.get(1).getText());

    }

    @Test
    public void testQueryLong_greater_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyGreater(JunitResource.PROPERTY_LONG_NUMBER, 4L) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("t2_aaa", items.get(0).getText());

    }

    @Test
    public void testQueryLong_less_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLesserAndEquals(JunitResource.PROPERTY_LONG_NUMBER, 4L) //
        );
        Assert.assertEquals(5, items.size());
        Assert.assertEquals("www.example.com", items.get(0).getText());
        Assert.assertEquals("www.example.com", items.get(1).getText());
        Assert.assertEquals("example.com", items.get(2).getText());
        Assert.assertEquals("t1_aaa", items.get(3).getText());
        Assert.assertEquals("zz", items.get(4).getText());

    }

    @Test
    public void testQueryLong_less_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLesser(JunitResource.PROPERTY_LONG_NUMBER, 4L) //
        );
        Assert.assertEquals(4, items.size());
        Assert.assertEquals("www.example.com", items.get(0).getText());
        Assert.assertEquals("www.example.com", items.get(1).getText());
        Assert.assertEquals("example.com", items.get(2).getText());
        Assert.assertEquals("t1_aaa", items.get(3).getText());

    }

    @Test
    public void testQueryLong_like_no() {

        thrown.expectMessage("Property [longNumber] does not support querying like");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLike(JunitResource.PROPERTY_LONG_NUMBER, "%10");

    }

    @Test
    public void testQueryOne() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.resourceFindByPk(new JunitResource("www.example.com", JunitResourceEnum.A, 2)).get();

    }

    @Test
    public void testQueryOne_failMoreThanOne() {

        thrown.expectMessage("There are more than one item matching the query");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.resourceFind( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_TEXT, "www.example.com") //
        );

    }

    @Test
    public void testQueryPK_none() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Missing detail
        Optional<JunitResource> one = resourceService.resourceFindByPk(new JunitResource("www.example.com", JunitResourceEnum.A, null));

        Assert.assertFalse(one.isPresent());

        // Wrong details
        one = resourceService.resourceFindByPk(new JunitResource("www.example.com", JunitResourceEnum.A, 6));

        Assert.assertFalse(one.isPresent());

    }

    @Test
    public void testQueryPK_one() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        Optional<JunitResource> one = resourceService.resourceFindByPk(new JunitResource("www.example.com", JunitResourceEnum.A, 2));

        Assert.assertTrue(one.isPresent());
        Set<String> tags = resourceService.tagFindAllByResource(one.get());
        Assert.assertEquals(1, tags.size());
        Assert.assertTrue(tags.contains("asite"));

    }

    @Test
    public void testQuerySetDates_many_equal_0() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_0.0
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_DATES, Sets.newHashSet()) //
        );
        List<String> actualTexts = items.stream().map(it -> it.getText()).sorted().collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("example.com", "sets_0.0", "t1_aaa", "t2_aaa", "www.example.com", "www.example.com", "zz"), actualTexts);

    }

    @Test
    public void testQuerySetDates_many_equal_1() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_1.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_DATES, Sets.newHashSet(DateTools.parseDateOnly("2000-01-01"))) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_1.1", items.get(0).getText());

        // sets_1.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_DATES, Sets.newHashSet(DateTools.parseDateOnly("2000-01-02"))) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_1.2", items.get(0).getText());
    }

    @Test
    public void testQuerySetDates_many_equal_2() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_2.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_DATES, Sets.newHashSet(DateTools.parseDateOnly("2000-01-01"), DateTools.parseDateOnly("2000-02-01"))) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.1", items.get(0).getText());

        // sets_2.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_DATES, Sets.newHashSet(DateTools.parseDateOnly("2000-01-02"), DateTools.parseDateOnly("2000-02-02"))) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.2", items.get(0).getText());
    }

    @Test
    public void testQuerySetDoubles_many_equal_0() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_0.0
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_DOUBLES, Sets.newHashSet()) //
        );
        List<String> actualTexts = items.stream().map(it -> it.getText()).sorted().collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("example.com", "sets_0.0", "t1_aaa", "t2_aaa", "www.example.com", "www.example.com", "zz"), actualTexts);

    }

    @Test
    public void testQuerySetDoubles_many_equal_1() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_1.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_DOUBLES, Sets.newHashSet(1.0d)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_1.1", items.get(0).getText());

        // sets_1.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_DOUBLES, Sets.newHashSet(2.0d)) //
        );
        Assert.assertEquals(1, items.size());
    }

    @Test
    public void testQuerySetDoubles_many_equal_2() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_2.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_DOUBLES, Sets.newHashSet(1.0d, 2.0d)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.1", items.get(0).getText());

        // sets_2.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_DOUBLES, Sets.newHashSet(3.0d, 4.0d)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.2", items.get(0).getText());
    }

    @Test
    public void testQuerySetEnumerations_many_equal_0() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_0.0
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_ENUMERATIONS, Sets.newHashSet()) //
        );
        List<String> actualTexts = items.stream().map(it -> it.getText()).sorted().collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("example.com", "sets_0.0", "t1_aaa", "t2_aaa", "www.example.com", "www.example.com", "zz"), actualTexts);

    }

    @Test
    public void testQuerySetEnumerations_many_equal_1() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_1.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_ENUMERATIONS, Sets.newHashSet(JunitResourceEnum.A)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_1.1", items.get(0).getText());

        // sets_1.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_ENUMERATIONS, Sets.newHashSet(JunitResourceEnum.B)) //
        );
        Assert.assertEquals(1, items.size());
    }

    @Test
    public void testQuerySetEnumerations_many_equal_2() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_2.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_ENUMERATIONS, Sets.newHashSet(JunitResourceEnum.A, JunitResourceEnum.B)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.1", items.get(0).getText());

        // sets_2.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_ENUMERATIONS, Sets.newHashSet(JunitResourceEnum.C, JunitResourceEnum.B)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.2", items.get(0).getText());
    }

    @Test
    public void testQuerySetFloats_many_equal_0() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_0.0
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_FLOATS, Sets.newHashSet()) //
        );
        List<String> actualTexts = items.stream().map(it -> it.getText()).sorted().collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("example.com", "sets_0.0", "t1_aaa", "t2_aaa", "www.example.com", "www.example.com", "zz"), actualTexts);

    }

    @Test
    public void testQuerySetFloats_many_equal_1() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_1.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_FLOATS, Sets.newHashSet(1.0f)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_1.1", items.get(0).getText());

        // sets_1.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_FLOATS, Sets.newHashSet(2.0f)) //
        );
        Assert.assertEquals(1, items.size());
    }

    @Test
    public void testQuerySetFloats_many_equal_2() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_2.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_FLOATS, Sets.newHashSet(1.0f, 2.0f)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.1", items.get(0).getText());

        // sets_2.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_FLOATS, Sets.newHashSet(3.0f, 4.0f)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.2", items.get(0).getText());
    }

    @Test
    public void testQuerySetIntegers_many_equal_0() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_0.0
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_INTEGERS, Sets.newHashSet()) //
        );
        List<String> actualTexts = items.stream().map(it -> it.getText()).sorted().collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("example.com", "sets_0.0", "t1_aaa", "t2_aaa", "www.example.com", "www.example.com", "zz"), actualTexts);

    }

    @Test
    public void testQuerySetIntegers_many_equal_1() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_1.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_INTEGERS, Sets.newHashSet(1)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_1.1", items.get(0).getText());

        // sets_1.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_INTEGERS, Sets.newHashSet(2)) //
        );
        Assert.assertEquals(1, items.size());
    }

    @Test
    public void testQuerySetIntegers_many_equal_2() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_2.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_INTEGERS, Sets.newHashSet(1, 2)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.1", items.get(0).getText());

        // sets_2.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_INTEGERS, Sets.newHashSet(3, 4)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.2", items.get(0).getText());
    }

    @Test
    public void testQuerySetLongs_many_equal_0() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_0.0
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_LONGS, Sets.newHashSet()) //
        );
        List<String> actualTexts = items.stream().map(it -> it.getText()).sorted().collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("example.com", "sets_0.0", "t1_aaa", "t2_aaa", "www.example.com", "www.example.com", "zz"), actualTexts);

    }

    @Test
    public void testQuerySetLongs_many_equal_1() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_1.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_LONGS, Sets.newHashSet(1l)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_1.1", items.get(0).getText());

        // sets_1.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_LONGS, Sets.newHashSet(2l)) //
        );
        Assert.assertEquals(1, items.size());
    }

    @Test
    public void testQuerySetLongs_many_equal_2() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_2.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_LONGS, Sets.newHashSet(1l, 2l)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.1", items.get(0).getText());

        // sets_2.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_LONGS, Sets.newHashSet(3l, 4l)) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.2", items.get(0).getText());
    }

    @Test
    public void testQuerySetTexts_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // one
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_TEXTS, Arrays.asList("one")) //
        );
        Assert.assertEquals(0, items.size());

        // one two
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_TEXTS, Arrays.asList("one", "two")) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("t1_aaa", items.get(0).getText());

        // three
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_TEXTS, Arrays.asList("three")) //
        );
        Assert.assertEquals(0, items.size());

        // one three
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_TEXTS, Arrays.asList("one", "three")) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("t2_aaa", items.get(0).getText());

    }

    @Test
    public void testQuerySetTexts_many_equal_0() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_0.0
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_TEXTS, Sets.newHashSet()) //
        );
        List<String> actualTexts = items.stream().map(it -> it.getText()).sorted().collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("example.com", "sets_0.0", "www.example.com", "www.example.com", "zz"), actualTexts);

    }

    @Test
    public void testQuerySetTexts_many_equal_1() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_1.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_TEXTS, Sets.newHashSet("1")) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_1.1", items.get(0).getText());

        // sets_1.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_TEXTS, Sets.newHashSet("2")) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_1.2", items.get(0).getText());
    }

    @Test
    public void testQuerySetTexts_many_equal_2() {

        JunitsHelper.createFakeDataWithSets(getCommonServicesContext(), getInternalServicesContext());

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // sets_2.1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_TEXTS, Sets.newHashSet("1", "2")) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.1", items.get(0).getText());

        // sets_2.2
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_TEXTS, Sets.newHashSet("3", "4")) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sets_2.2", items.get(0).getText());
    }

    @Test
    public void testQuerySetTexts_single_contains_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // one
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyContains(JunitResource.PROPERTY_SET_TEXTS, Arrays.asList("one")) //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("t1_aaa", items.get(0).getText());
        Assert.assertEquals("t2_aaa", items.get(1).getText());

        // three
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyContains(JunitResource.PROPERTY_SET_TEXTS, Arrays.asList("three")) //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("t2_aaa", items.get(0).getText());

    }

    @Test
    public void testQuerySetTexts_single_greater_equal_no() {

        thrown.expectMessage("Property [setTexts] does not support querying greater or equal");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyGreaterAndEquals(JunitResource.PROPERTY_SET_TEXTS, "a");

    }

    @Test
    public void testQuerySetTexts_single_greater_no() {

        thrown.expectMessage("Property [setTexts] does not support querying greater");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyGreater(JunitResource.PROPERTY_SET_TEXTS, "a");

    }

    @Test
    public void testQuerySetTexts_single_less_equal_no() {

        thrown.expectMessage("Property [setTexts] does not support querying lesser or equal");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLesserAndEquals(JunitResource.PROPERTY_SET_TEXTS, "a");

    }

    @Test
    public void testQuerySetTexts_single_less_no() {

        thrown.expectMessage("Property [setTexts] does not support querying lesser");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLesser(JunitResource.PROPERTY_SET_TEXTS, "a");

    }

    @Test
    public void testQuerySetTexts_single_like_no() {

        thrown.expectMessage("Property [setTexts] does not support querying like");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLike(JunitResource.PROPERTY_SET_TEXTS, "t%"); //
    }

    @Test
    public void testQueryString_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // None
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_TEXT, "not") //
        );
        Assert.assertEquals(0, items.size());

        // www.example.com
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_TEXT, "www.example.com") //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals((Integer) 1, items.get(0).getIntegerNumber());
        Assert.assertEquals((Integer) 2, items.get(1).getIntegerNumber());

    }

    @Test
    public void testQueryString_greater_equal_no() {

        thrown.expectMessage("Property [text] does not support querying greater or equal");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyGreaterAndEquals(JunitResource.PROPERTY_TEXT, "a");

    }

    @Test
    public void testQueryString_greater_no() {

        thrown.expectMessage("Property [text] does not support querying greater");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyGreater(JunitResource.PROPERTY_TEXT, "a");

    }

    @Test
    public void testQueryString_less_equal_no() {

        thrown.expectMessage("Property [text] does not support querying lesser or equal");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLesserAndEquals(JunitResource.PROPERTY_TEXT, "a");

    }

    @Test
    public void testQueryString_less_no() {

        thrown.expectMessage("Property [text] does not support querying lesser");

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        resourceService.createResourceQuery(JunitResource.class) //
                .propertyLesser(JunitResource.PROPERTY_TEXT, "a");

    }

    @Test
    public void testQueryString_like_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // %_
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLike(JunitResource.PROPERTY_TEXT, "%_aaa") //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("t1_aaa", items.get(0).getText());
        Assert.assertEquals("t2_aaa", items.get(1).getText());

    }

    @Test
    public void testQueryTag_and() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // None
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .tagAddAnd("not") //
        );
        Assert.assertEquals(0, items.size());

        // tag1
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .tagAddAnd("tag1") //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals((Integer) 1, items.get(0).getIntegerNumber());

        // asite
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .tagAddAnd("asite") //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals((Integer) 1, items.get(0).getIntegerNumber());
        Assert.assertEquals((Integer) 2, items.get(1).getIntegerNumber());

    }

    @Test
    public void testQueryTag_or() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // None and tag1
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .tagAddOr("not", "tag1") //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals((Integer) 1, items.get(0).getIntegerNumber());

        // None, tag1 and asite
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .tagAddOr("not", "tag1", "asite") //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals((Integer) 1, items.get(0).getIntegerNumber());
        Assert.assertEquals((Integer) 2, items.get(1).getIntegerNumber());

    }

    @Test(timeout = 20000)
    public void testTimer_once() {

        AtomicInteger count = new AtomicInteger();

        getCommonServicesContext().getTimerService().timerAdd( //
                new TimerEventContext(new CounterTimerEventHandler(count), "testTimer_once", Calendar.SECOND, 2, true, false) //
        );

        // Not at start
        ThreadTools.sleep(500);
        Assert.assertEquals(0, count.get());

        // Wait for it
        while (count.get() == 0) {
            ThreadTools.sleep(1000);
        }

        // Make sure only once
        ThreadTools.sleep(2000);
        Assert.assertEquals(1, count.get());

    }

    @Test(timeout = 20000)
    public void testTimer_recurrent_at_start() {

        AtomicInteger count = new AtomicInteger();

        getCommonServicesContext().getTimerService().timerAdd( //
                new TimerEventContext(new CounterTimerEventHandler(count), "testTimer_recurrent_at_start", Calendar.SECOND, 2, false, true) //
        );

        // At start
        ThreadTools.sleep(500);
        Assert.assertEquals(1, count.get());

        // Wait for at least 4
        while (count.get() < 4) {
            ThreadTools.sleep(1000);
        }

    }

    @Test(timeout = 20000)
    public void testTimer_recurrent_no_start() {

        AtomicInteger count = new AtomicInteger();

        getCommonServicesContext().getTimerService().timerAdd( //
                new TimerEventContext(new CounterTimerEventHandler(count), "testTimer_recurrent_no_start", Calendar.SECOND, 2) //
        );

        // Not at start
        ThreadTools.sleep(500);
        Assert.assertEquals(0, count.get());

        // Wait for at least 3
        while (count.get() < 3) {
            ThreadTools.sleep(1000);
        }

    }

}
