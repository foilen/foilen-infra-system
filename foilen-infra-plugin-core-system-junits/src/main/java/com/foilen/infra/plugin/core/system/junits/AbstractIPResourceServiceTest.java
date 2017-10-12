/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.junits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.foilen.infra.plugin.v1.core.base.resources.Application;
import com.foilen.infra.plugin.v1.core.base.resources.DnsEntry;
import com.foilen.infra.plugin.v1.core.base.resources.DnsPointer;
import com.foilen.infra.plugin.v1.core.base.resources.Domain;
import com.foilen.infra.plugin.v1.core.base.resources.Machine;
import com.foilen.infra.plugin.v1.core.base.resources.UnixUser;
import com.foilen.infra.plugin.v1.core.base.resources.UrlRedirection;
import com.foilen.infra.plugin.v1.core.base.resources.Website;
import com.foilen.infra.plugin.v1.core.base.resources.WebsiteCertificate;
import com.foilen.infra.plugin.v1.core.base.resources.helper.UnixUserAvailableIdHelper;
import com.foilen.infra.plugin.v1.core.base.resources.model.DnsEntryType;
import com.foilen.infra.plugin.v1.core.common.CertificateHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.TimerEventContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.TimerEventHandler;
import com.foilen.infra.plugin.v1.core.exception.IllegalUpdateException;
import com.foilen.infra.plugin.v1.core.exception.ResourcePrimaryKeyCollisionException;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionProvider;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionV1;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.IPPluginService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.plugin.v1.failingexample.CrashingTimerEventHandler;
import com.foilen.infra.plugin.v1.model.junit.JunitResource;
import com.foilen.infra.plugin.v1.model.junit.JunitResourceEnum;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.foilen.smalltools.crypt.spongycastle.asymmetric.AsymmetricKeys;
import com.foilen.smalltools.crypt.spongycastle.asymmetric.RSACrypt;
import com.foilen.smalltools.crypt.spongycastle.cert.CertificateDetails;
import com.foilen.smalltools.crypt.spongycastle.cert.RSACertificate;
import com.foilen.smalltools.hash.HashSha1;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.DateTools;
import com.foilen.smalltools.tools.ThreadTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.foilen.smalltools.tuple.Tuple3;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

/**
 * This is to test that the implementation of the real system is working as expected.
 */
public abstract class AbstractIPResourceServiceTest extends AbstractBasics {

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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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

    private void assertSet(Set<String> actualTags, String... expectedTags) {
        Assert.assertEquals(expectedTags.length, actualTags.size());
        Assert.assertTrue(actualTags.containsAll(Arrays.asList(expectedTags)));
    }

    protected void assertState(String resourceName) {
        ResourcesState resourcesState = new ResourcesState();

        resourcesState.setResources(getInternalServicesContext().getInternalIPResourceService().resourceFindAll().stream() //
                .map(resource -> {
                    ResourceState resourceState = new ResourceState(getResourceDetails(resource));

                    // Links
                    List<ResourcesStateLink> links = getCommonServicesContext().getResourceService().linkFindAllByFromResource(resource).stream() //
                            .map(link -> new ResourcesStateLink(link.getA(), getResourceDetails(link.getB()))) //
                            .collect(Collectors.toList());
                    resourceState.setLinks(links);

                    // Tags
                    resourceState.setTags(getCommonServicesContext().getResourceService().tagFindAllByResource(resource).stream().sorted().collect(Collectors.toList()));

                    return resourceState;
                }) //
                .collect(Collectors.toList()));

        resourcesState.sort();

        AssertTools.assertJsonComparison(resourceName, AbstractIPResourceServiceTest.class, resourcesState);

    }

    @Before
    public void createFakeData() {
        JunitsHelper.addResourcesDefinition(getInternalServicesContext());
        JunitsHelper.createFakeData(getInternalServicesContext());
    }

    private WebsiteCertificate createWebsiteCertificate(String... domainNames) {

        String commonName = domainNames[0];

        AsymmetricKeys keys = RSACrypt.RSA_CRYPT.generateKeyPair(1024);
        RSACertificate rsaCertificate = new RSACertificate(keys).selfSign( //
                new CertificateDetails().setCommonName(commonName) //
                        .addSanDns(domainNames) //
                        .setStartDate(DateTools.parseDateOnly("2001-07-01")).setEndDate(DateTools.parseDateOnly("2001-08-01")));

        WebsiteCertificate websiteCertificate = new WebsiteCertificate();
        CertificateHelper.toWebsiteCertificate(null, rsaCertificate, websiteCertificate);
        websiteCertificate.setThumbprint(HashSha1.hashString(Joiner.on(',').join(domainNames)));
        return websiteCertificate;
    }

    private void deleteAllResources() {
        ChangesContext changes = new ChangesContext();
        for (IPResource resource : getInternalServicesContext().getInternalIPResourceService().resourceFindAll()) {
            changes.getResourcesToDelete().add(resource.getInternalId());
        }
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);
    }

    protected abstract CommonServicesContext getCommonServicesContext();

    protected abstract InternalServicesContext getInternalServicesContext();

    protected String getResourceDetails(IPResource resource) {
        return resource.getClass().getSimpleName() + " | " + resource.getResourceName() + " | " + resource.getResourceDescription();
    }

    @Test
    public void testBrokenPlugin() {
        IPPluginService ipPluginService = getCommonServicesContext().getPluginService();

        List<Tuple3<Class<? extends IPPluginDefinitionProvider>, IPPluginDefinitionV1, String>> broken = ipPluginService.getBrokenPlugins();
        Assert.assertEquals(1, broken.size());
        Assert.assertEquals("com.foilen.infra.plugin.v1.failingexample.ExampleFailingPluginDefinitionProvider", broken.get(0).getA().getName());
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
        ChangesContext changes = new ChangesContext();
        InternalChangeService internalChangeService = getInternalServicesContext().getInternalChangeService();
        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Delete all
        entries = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class));
        for (JunitResource entry : entries) {
            changes.getResourcesToDelete().add(entry.getInternalId());
        }
        internalChangeService.changesExecute(changes);

        entries = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class));
        Assert.assertTrue(entries.isEmpty());

        // Create some linked together
        JunitResource masterResource = new JunitResource("theMaster");
        changes.getResourcesToAdd().add(masterResource);

        JunitResource slaveResource = new JunitResource("slave1");
        changes.getResourcesToAdd().add(slaveResource);
        changes.getLinksToAdd().add(new Tuple3<>(masterResource, "COMMANDS", slaveResource));

        slaveResource = new JunitResource("slave2");
        changes.getResourcesToAdd().add(slaveResource);
        changes.getLinksToAdd().add(new Tuple3<>(masterResource, "COMMANDS", slaveResource));

        changes.getLinksToAdd().add(new Tuple3<>(slaveResource, "LIKES", masterResource));

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
        changes.getLinksToAdd().add(new Tuple3<>(slaveResource, "LOVES", masterResource));

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
            changes.getLinksToDelete().add(new Tuple3<>(slaveResource, link.getA(), link.getB()));
        }
        changes.getLinksToAdd().add(new Tuple3<>(slaveResource, "DISLIKE", masterResource));

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
        ChangesContext changes = new ChangesContext();
        JunitResource r1 = new JunitResource("1");
        JunitResource r2 = new JunitResource("2");
        changes.getResourcesToAdd().add(r1);
        changes.getResourcesToAdd().add(r2);
        changes.getLinksToAdd().add(new Tuple3<>(r1, "link1", r2));
        changes.getTagsToAdd().add(new Tuple2<>(r1, "tag1"));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        IPResourceService resourceService = getCommonServicesContext().getResourceService();
        r1 = resourceService.resourceFindByPk(r1).get();
        r2 = resourceService.resourceFindByPk(r2).get();
        Assert.assertEquals(Arrays.asList("link1"), resourceService.linkFindAllByFromResource(r1).stream().map(it -> it.getA()).sorted().collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList("tag1"), resourceService.tagFindAllByResource(r1).stream().sorted().collect(Collectors.toList()));

        // Delete it
        changes.getResourcesToDelete().add(resourceService.resourceFindByPk(r1).get().getInternalId());
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        Assert.assertEquals(0, resourceService.linkFindAllByFromResource(r1).size());
        Assert.assertEquals(0, resourceService.tagFindAllByResource(r1).size());

        // Recreate it
        changes.getResourcesToAdd().add(r1);
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        // Check no links and tags
        Assert.assertEquals(0, resourceService.linkFindAllByFromResource(r1).size());
        Assert.assertEquals(0, resourceService.tagFindAllByResource(r1).size());

    }

    @Test
    public void testChanges_reduntantLinksAndTags() {

        ChangesContext changes = new ChangesContext();
        JunitResource r1 = new JunitResource("reduntant_1");
        JunitResource r2 = new JunitResource("reduntant_2");
        changes.getResourcesToAdd().add(r1);
        changes.getResourcesToAdd().add(r2);
        changes.getLinksToAdd().add(new Tuple3<>(r1, "link1", r2));
        changes.getLinksToAdd().add(new Tuple3<>(r1, "link1", r2));
        changes.getLinksToAdd().add(new Tuple3<>(r1, "link2", r2));
        changes.getTagsToAdd().add(new Tuple2<>(r1, "tag1"));
        changes.getTagsToAdd().add(new Tuple2<>(r1, "tag1"));
        changes.getTagsToAdd().add(new Tuple2<>(r1, "tag2"));
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

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        String machineName1 = "m1.node.example.com";
        String machineName2 = "m2.node.example.com";

        String m1Ip1 = "199.141.1.101";
        String m2Ip1 = "199.141.1.201";

        assertResourceCount(0, Machine.class);
        assertResourceExists(false, new Machine(machineName1), Machine.class);
        assertResourceExists(false, new Machine(machineName2), Machine.class);
        assertResourceCount(0, DnsEntry.class);
        assertResourceCount(0, Domain.class);

        // Create both machines
        ChangesContext changes = new ChangesContext();
        changes.getResourcesToAdd().add(new Machine(machineName1, m1Ip1));
        changes.getResourcesToAdd().add(new Machine(machineName2, m2Ip1));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertResourceCount(2, Machine.class);
        Machine m1 = assertResourceExists(true, new Machine(machineName1), Machine.class);
        Machine m2 = assertResourceExists(true, new Machine(machineName2), Machine.class);
        Assert.assertEquals(m1Ip1, m1.getPublicIp());
        Assert.assertEquals(m2Ip1, m2.getPublicIp());
        assertResourceCount(2, DnsEntry.class);
        assertResourceExists(true, new DnsEntry(machineName1, DnsEntryType.A, m1Ip1), DnsEntry.class);
        assertResourceExists(true, new DnsEntry(machineName2, DnsEntryType.A, m2Ip1), DnsEntry.class);
        assertResourceCount(4, Domain.class);
        assertResourceExists(true, new Domain("m1.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("m2.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("example.com", null), Domain.class);

        // Delete the domain "m2.node.example.com" (must be back)
        long domain2Id = resourceService.resourceFindByPk(new Domain("m2.node.example.com", null)).get().getInternalId();
        changes.getResourcesToDelete().add(domain2Id);
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertResourceCount(2, Machine.class);
        m1 = assertResourceExists(true, new Machine(machineName1), Machine.class);
        m2 = assertResourceExists(true, new Machine(machineName2), Machine.class);
        Assert.assertEquals(m1Ip1, m1.getPublicIp());
        Assert.assertEquals(m2Ip1, m2.getPublicIp());
        assertResourceCount(2, DnsEntry.class);
        assertResourceExists(true, new DnsEntry(machineName1, DnsEntryType.A, m1Ip1), DnsEntry.class);
        assertResourceExists(true, new DnsEntry(machineName2, DnsEntryType.A, m2Ip1), DnsEntry.class);
        assertResourceCount(4, Domain.class);
        assertResourceExists(true, new Domain("m1.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("m2.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("example.com", null), Domain.class);

        long domain2Id_2 = resourceService.resourceFindByPk(new Domain("m2.node.example.com", null)).get().getInternalId();
        Assert.assertNotEquals(domain2Id, domain2Id_2);

        // Create extra links and tags
        changes.getTagsToAdd().add(new Tuple2<IPResource, String>(m2, "extraTag"));
        changes.getLinksToAdd().add(new Tuple3<IPResource, String, IPResource>(m1, "extraLink", m2));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        // Get the list of ids
        List<Long> allIds = new ArrayList<>();
        allIds.addAll(resourceService.resourceFindAll(resourceService.createResourceQuery(Machine.class)).stream().map(it -> it.getInternalId()).collect(Collectors.toList()));
        allIds.addAll(resourceService.resourceFindAll(resourceService.createResourceQuery(DnsEntry.class)).stream().map(it -> it.getInternalId()).collect(Collectors.toList()));
        allIds.addAll(resourceService.resourceFindAll(resourceService.createResourceQuery(Domain.class)).stream().map(it -> it.getInternalId()).collect(Collectors.toList()));
        Collections.sort(allIds);

        // Delete the domain "m2.node.example.com", delete machine m1 and rename the machine m2 (to have a rollback)
        changes.getResourcesToDelete().add(m1.getInternalId());
        changes.getResourcesToUpdate().add(new Tuple2<>(m2.getInternalId(), new Machine("m3.node.example.com", m2Ip1)));
        try {
            getInternalServicesContext().getInternalChangeService().changesExecute(changes);
            Assert.fail("Expecting exception");
        } catch (IllegalUpdateException e) {
        }

        assertResourceCount(2, Machine.class);
        m1 = assertResourceExists(true, new Machine(machineName1), Machine.class);
        m2 = assertResourceExists(true, new Machine(machineName2), Machine.class);
        Assert.assertEquals(m1Ip1, m1.getPublicIp());
        Assert.assertEquals(m2Ip1, m2.getPublicIp());
        assertResourceCount(2, DnsEntry.class);
        assertResourceExists(true, new DnsEntry(machineName1, DnsEntryType.A, m1Ip1), DnsEntry.class);
        assertResourceExists(true, new DnsEntry(machineName2, DnsEntryType.A, m2Ip1), DnsEntry.class);
        assertResourceCount(4, Domain.class);
        assertResourceExists(true, new Domain("m1.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("m2.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("example.com", null), Domain.class);

        long domain2Id_3 = resourceService.resourceFindByPk(new Domain("m2.node.example.com", null)).get().getInternalId();
        Assert.assertEquals(domain2Id_2, domain2Id_3);

        Set<String> tags = resourceService.tagFindAllByResource(m2);
        Assert.assertEquals(1, tags.size());
        Assert.assertEquals("extraTag", tags.iterator().next());

        List<? extends IPResource> extraLinks = resourceService.linkFindAllByFromResourceAndLinkType(m1, "extraLink");
        Assert.assertEquals(1, extraLinks.size());
        Assert.assertEquals(m2, extraLinks.get(0));

        // Confirm all the same ids
        List<Long> allActualIds = new ArrayList<>();
        allActualIds.addAll(resourceService.resourceFindAll(resourceService.createResourceQuery(Machine.class)).stream().map(it -> it.getInternalId()).collect(Collectors.toList()));
        allActualIds.addAll(resourceService.resourceFindAll(resourceService.createResourceQuery(DnsEntry.class)).stream().map(it -> it.getInternalId()).collect(Collectors.toList()));
        allActualIds.addAll(resourceService.resourceFindAll(resourceService.createResourceQuery(Domain.class)).stream().map(it -> it.getInternalId()).collect(Collectors.toList()));
        Collections.sort(allActualIds);
        Assert.assertEquals(allIds, allActualIds);
    }

    @Test
    public void testChanges_tags() {
        // Common
        List<JunitResource> entries;
        JunitResource resource;
        List<JunitResource> resources;
        Set<String> tags;
        ChangesContext changes = new ChangesContext();
        InternalChangeService internalChangeService = getInternalServicesContext().getInternalChangeService();
        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Must have some
        entries = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class));
        Assert.assertFalse(entries.isEmpty());

        // Delete all
        for (JunitResource entry : entries) {
            changes.getResourcesToDelete().add(entry.getInternalId());
        }
        internalChangeService.changesExecute(changes);

        entries = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class));
        Assert.assertTrue(entries.isEmpty());

        // Create 2
        resource = new JunitResource("example.com", JunitResourceEnum.A, 1);
        changes.getResourcesToAdd().add(resource);
        changes.getTagsToAdd().addAll(Arrays.asList( //
                new Tuple2<>(resource, "tag1"), //
                new Tuple2<>(resource, "asite")));

        resource = new JunitResource("www.example.com", JunitResourceEnum.A, 1);
        changes.getResourcesToAdd().add(resource);
        changes.getTagsToAdd().add(new Tuple2<>(resource, "asite"));
        internalChangeService.changesExecute(changes);

        entries = resourceService.resourceFindAll(resourceService.createResourceQuery(JunitResource.class));
        Assert.assertEquals(2, entries.size());

        // Update add tags
        resource = resourceService.resourceFindByPk(new JunitResource("example.com", JunitResourceEnum.A, 1)).get();
        changes.getTagsToAdd().add(new Tuple2<>(resource, "changed"));
        changes.getResourcesToUpdate().add(new Tuple2<>(resource.getInternalId(), new JunitResource("example2.com", JunitResourceEnum.A, 2)));
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
        changes.getTagsToDelete().add(new Tuple2<>(resource, "tag1"));
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
            changes.getResourcesToAdd().add(new JunitResource("example2.com", JunitResourceEnum.A, 2));
            internalChangeService.changesExecute(changes);
            Assert.fail("Didn't get an exception");
        } catch (ResourcePrimaryKeyCollisionException e) {

        }

        // Update non existing items (fail)
        try {
            resource = resourceService.resourceFindByPk(new JunitResource("not.existing.com", JunitResourceEnum.A, 1)).get();
            changes.getResourcesToUpdate().add(new Tuple2<>(resource.getInternalId(), new JunitResource("example2.com", JunitResourceEnum.A, 5)));
            internalChangeService.changesExecute(changes);
            Assert.fail("Didn't get an exception");
        } catch (Exception e) {
        }
    }

    @Test
    public void testDuplicatePkSameResource_create() {
        // Common
        JunitResource resource;
        ChangesContext changes = new ChangesContext();
        InternalChangeService internalChangeService = getInternalServicesContext().getInternalChangeService();

        // Create 1 item
        resource = new JunitResource("t1", JunitResourceEnum.A, 1);
        resource.setLongNumber(10L);
        changes.getResourcesToAdd().add(resource);
        internalChangeService.changesExecute(changes);

        // Create same. Not fine
        thrown.expect(ResourcePrimaryKeyCollisionException.class);
        resource = new JunitResource("t1", JunitResourceEnum.A, 1);
        resource.setLongNumber(30L);
        changes.getResourcesToAdd().add(resource);
        internalChangeService.changesExecute(changes);

    }

    @Test
    public void testDuplicatePkSameResource_update() {
        // Common
        JunitResource resource;
        ChangesContext changes = new ChangesContext();
        InternalChangeService internalChangeService = getInternalServicesContext().getInternalChangeService();
        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Create 2 items
        resource = new JunitResource("t1", JunitResourceEnum.A, 1);
        resource.setLongNumber(10L);
        changes.getResourcesToAdd().add(resource);
        resource = new JunitResource("t2", JunitResourceEnum.A, 2);
        resource.setLongNumber(10L);
        changes.getResourcesToAdd().add(resource);
        internalChangeService.changesExecute(changes);

        // Rename second item to same pk as first
        thrown.expect(ResourcePrimaryKeyCollisionException.class);
        resource = new JunitResource("t1", JunitResourceEnum.A, 1);
        resource.setLongNumber(20L);
        changes.getResourcesToUpdate().add(new Tuple2<>(resourceService.resourceFindByPk(new JunitResource("t2", JunitResourceEnum.A, 2)).get().getInternalId(), resource));
        internalChangeService.changesExecute(changes);

    }

    @Test
    public void testMachineUpdateHandler() {

        String machineName1 = "m1.node.example.com";
        String machineName2 = "m2.node.example.com";

        String m1Ip2 = "199.141.1.102";
        String m2Ip1 = "199.141.1.201";
        String m2Ip2 = "199.141.1.202";

        assertResourceCount(0, Machine.class);
        assertResourceExists(false, new Machine(machineName1), Machine.class);
        assertResourceExists(false, new Machine(machineName2), Machine.class);
        assertResourceCount(0, DnsEntry.class);
        assertResourceCount(0, Domain.class);

        // Create both machines: m1 without ip and m2 with ip
        ChangesContext changes = new ChangesContext();
        changes.getResourcesToAdd().add(new Machine(machineName1));
        changes.getResourcesToAdd().add(new Machine(machineName2, m2Ip1));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertResourceCount(2, Machine.class);
        Machine m1 = assertResourceExists(true, new Machine(machineName1), Machine.class);
        Machine m2 = assertResourceExists(true, new Machine(machineName2), Machine.class);
        Assert.assertEquals(null, m1.getPublicIp());
        Assert.assertEquals(m2Ip1, m2.getPublicIp());
        assertResourceCount(1, DnsEntry.class);
        assertResourceExists(true, new DnsEntry(machineName2, DnsEntryType.A, m2Ip1), DnsEntry.class);
        assertResourceCount(4, Domain.class);
        assertResourceExists(true, new Domain("m1.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("m2.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("example.com", null), Domain.class);

        // Update both machines: m1 with ip and m2 with a different ip
        changes.getResourcesToUpdate().add(new Tuple2<>(m1.getInternalId(), new Machine(machineName1, m1Ip2)));
        changes.getResourcesToUpdate().add(new Tuple2<>(m2.getInternalId(), new Machine(machineName2, m2Ip2)));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertResourceCount(2, Machine.class);
        m1 = assertResourceExists(true, new Machine(machineName1), Machine.class);
        m2 = assertResourceExists(true, new Machine(machineName2), Machine.class);
        Assert.assertEquals(m1Ip2, m1.getPublicIp());
        Assert.assertEquals(m2Ip2, m2.getPublicIp());
        assertResourceCount(2, DnsEntry.class);
        assertResourceExists(true, new DnsEntry(machineName1, DnsEntryType.A, m1Ip2), DnsEntry.class);
        assertResourceExists(true, new DnsEntry(machineName2, DnsEntryType.A, m2Ip2), DnsEntry.class);
        assertResourceCount(4, Domain.class);
        assertResourceExists(true, new Domain("m1.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("m2.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("example.com", null), Domain.class);

        // Remove ip of m2
        changes.getResourcesToUpdate().add(new Tuple2<>(m2.getInternalId(), new Machine(machineName2, null)));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertResourceCount(2, Machine.class);
        m1 = assertResourceExists(true, new Machine(machineName1), Machine.class);
        m2 = assertResourceExists(true, new Machine(machineName2), Machine.class);
        Assert.assertEquals(m1Ip2, m1.getPublicIp());
        Assert.assertEquals(null, m2.getPublicIp());
        assertResourceCount(1, DnsEntry.class);
        assertResourceExists(true, new DnsEntry(machineName1, DnsEntryType.A, m1Ip2), DnsEntry.class);
        assertResourceCount(4, Domain.class);
        assertResourceExists(true, new Domain("m1.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("m2.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("example.com", null), Domain.class);

        // Delete m1
        changes.getResourcesToDelete().add(m1.getInternalId());
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertResourceCount(1, Machine.class);
        m2 = assertResourceExists(true, new Machine(machineName2), Machine.class);
        Assert.assertEquals(null, m2.getPublicIp());
        assertResourceCount(0, DnsEntry.class);
        assertResourceCount(3, Domain.class);
        assertResourceExists(true, new Domain("m2.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("example.com", null), Domain.class);

        // Update name (fails)
        try {
            changes.getResourcesToUpdate().add(new Tuple2<>(m2.getInternalId(), new Machine("anotherName.node.example.com")));
            getInternalServicesContext().getInternalChangeService().changesExecute(changes);
            Assert.fail("Expecting an exception");
        } catch (IllegalUpdateException e) {
        }
        changes.clear();

        // Rolled back
        assertResourceCount(1, Machine.class);
        m2 = assertResourceExists(true, new Machine(machineName2), Machine.class);
        Assert.assertEquals(null, m2.getPublicIp());
        assertResourceCount(0, DnsEntry.class);
        assertResourceCount(3, Domain.class);
        assertResourceExists(true, new Domain("m2.node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("node.example.com", null), Domain.class);
        assertResourceExists(true, new Domain("example.com", null), Domain.class);

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
                .propertyEquals(JunitResource.PROPERTY_SET_TEXTS, "two")//
        );
        Assert.assertTrue(junitResourceOptional.isPresent());
        long expectedId = junitResourceOptional.get().getInternalId();

        // Modify its editor
        JunitResource resource = junitResourceOptional.get();
        ChangesContext changes = new ChangesContext();
        resource.setResourceEditorName("junit");
        changes.getResourcesToUpdate().add(new Tuple2<>(expectedId, resource));
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
                .propertyEquals(JunitResource.PROPERTY_SET_TEXTS, "two")//
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
        Optional<DnsEntry> dnsEntryOptional = resourceService.resourceFind(resourceService.createResourceQuery(DnsEntry.class) //
                .addIdEquals(expectedId) //
        );
        Assert.assertFalse(dnsEntryOptional.isPresent());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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
    public void testQuerySetTexts_many_equal_0() {

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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

        JunitsHelper.createFakeDataWithSets(getInternalServicesContext());

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
    public void testQuerySetTexts_single_equal_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // one
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_TEXTS, "one") //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("t1_aaa", items.get(0).getText());
        Assert.assertEquals("t2_aaa", items.get(1).getText());

        // three
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyEquals(JunitResource.PROPERTY_SET_TEXTS, "three") //
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
    public void testQuerySetTexts_single_like_yes() {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // t%
        List<JunitResource> items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLike(JunitResource.PROPERTY_SET_TEXTS, "t%") //
        );
        Assert.assertEquals(2, items.size());
        Assert.assertEquals("t1_aaa", items.get(0).getText());
        Assert.assertEquals("t2_aaa", items.get(1).getText());

        // %w%
        items = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(JunitResource.class) //
                        .propertyLike(JunitResource.PROPERTY_SET_TEXTS, "%w%") //
        );
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("t1_aaa", items.get(0).getText());

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

    @Test
    public void testResourceApplication() {

        deleteAllResources();

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Create initial data
        Machine m1 = new Machine("m1.example.com", "199.141.1.101");
        Machine m2 = new Machine("m2.example.com", "199.141.1.201");
        Machine m3 = new Machine("m3.example.com", "199.141.1.301");
        UnixUser uu1 = new UnixUser(UnixUserAvailableIdHelper.getNextAvailableId(), "user1", "/home/user1", null, null);
        UnixUser uu2 = new UnixUser(UnixUserAvailableIdHelper.getNextAvailableId(), "user2", "/home/user2", null, null);
        ChangesContext changes = new ChangesContext();
        changes.getResourcesToAdd().add(m1);
        changes.getResourcesToAdd().add(m2);
        changes.getResourcesToAdd().add(m3);
        changes.getResourcesToAdd().add(uu1);
        changes.getResourcesToAdd().add(uu2);
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("ApplicationTest-state-0.json");

        // Create an application without links and one with links
        Application a1 = new Application();
        a1.setName("a1");
        a1.getDomainNames().add("a1.example.com");
        a1.getApplicationDefinition().getPortsExposed().put(12, 12);
        Application a2 = new Application();
        a2.setName("a2");
        a2.getDomainNames().add("d1.example.com");
        a2.getDomainNames().add("d2.example.com");
        a2.getApplicationDefinition().getPortsExposed().put(34, 12);
        changes.getResourcesToAdd().add(a1);
        changes.getResourcesToAdd().add(a2);
        changes.getLinksToAdd().add(new Tuple3<>(a2, LinkTypeConstants.RUN_AS, uu2));
        changes.getLinksToAdd().add(new Tuple3<>(a2, LinkTypeConstants.INSTALLED_ON, m2));
        changes.getLinksToAdd().add(new Tuple3<>(a2, LinkTypeConstants.INSTALLED_ON, m3));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("ApplicationTest-state-1.json");
        a1 = resourceService.resourceFindByPk(a1).get();
        a2 = resourceService.resourceFindByPk(a2).get();
        Assert.assertEquals(null, a1.getApplicationDefinition().getRunAs());
        Assert.assertEquals((Integer) 2001, a2.getApplicationDefinition().getRunAs());

        // Update the application without links to have links
        changes.getLinksToAdd().add(new Tuple3<>(a1, LinkTypeConstants.RUN_AS, uu1));
        changes.getLinksToAdd().add(new Tuple3<>(a1, LinkTypeConstants.INSTALLED_ON, m1));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("ApplicationTest-state-2.json");
        a1 = resourceService.resourceFindByPk(a1).get();
        a2 = resourceService.resourceFindByPk(a2).get();
        Assert.assertEquals((Integer) 2000, a1.getApplicationDefinition().getRunAs());
        Assert.assertEquals((Integer) 2001, a2.getApplicationDefinition().getRunAs());

        // Update the application with links to different links
        changes.getLinksToAdd().add(new Tuple3<>(a2, LinkTypeConstants.RUN_AS, uu1));
        changes.getLinksToDelete().add(new Tuple3<>(a2, LinkTypeConstants.RUN_AS, uu2));
        changes.getLinksToAdd().add(new Tuple3<>(a2, LinkTypeConstants.INSTALLED_ON, m1));
        changes.getLinksToDelete().add(new Tuple3<>(a2, LinkTypeConstants.INSTALLED_ON, m2));
        changes.getLinksToDelete().add(new Tuple3<>(a2, LinkTypeConstants.INSTALLED_ON, m3));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("ApplicationTest-state-3.json");
        a1 = resourceService.resourceFindByPk(a1).get();
        a2 = resourceService.resourceFindByPk(a2).get();
        Assert.assertEquals((Integer) 2000, a1.getApplicationDefinition().getRunAs());
        Assert.assertEquals((Integer) 2000, a2.getApplicationDefinition().getRunAs());

        // Fail if they are both on the same machine with same exposed port
        try {
            a1.getApplicationDefinition().getPortsExposed().put(34, 55);
            changes.getResourcesToUpdate().add(new Tuple2<>(a1.getInternalId(), a1));
            getInternalServicesContext().getInternalChangeService().changesExecute(changes);
            Assert.fail("Expecting exception");
        } catch (IllegalUpdateException e) {
            // Expected
        }

        // Fail if there are 2 running users on the same app
        try {
            changes.clear();
            changes.getLinksToAdd().add(new Tuple3<>(a1, LinkTypeConstants.RUN_AS, uu2));
            getInternalServicesContext().getInternalChangeService().changesExecute(changes);
            Assert.fail("Expecting exception");
        } catch (IllegalUpdateException e) {
            // Expected
        }

        // Delete application with links
        changes.clear();
        changes.getResourcesToDelete().add(a2.getInternalId());
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("ApplicationTest-state-4.json");

        // Remove all links on application
        changes.getLinksToDelete().add(new Tuple3<>(a1, LinkTypeConstants.RUN_AS, uu1));
        changes.getLinksToDelete().add(new Tuple3<>(a1, LinkTypeConstants.INSTALLED_ON, m1));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("ApplicationTest-state-5.json");
        a1 = resourceService.resourceFindByPk(a1).get();
        Assert.assertEquals(null, a1.getApplicationDefinition().getRunAs());

        // Delete application (back to initial state)
        changes.getResourcesToDelete().add(a1.getInternalId());
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("ApplicationTest-state-0.json");
    }

    @Test
    public void testResourceDnsPointer() {

        deleteAllResources();

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Create initial data
        Machine m1 = new Machine("m1.example.com", "199.141.1.101");
        Machine m2 = new Machine("m2.example.com", "199.141.1.201");
        ChangesContext changes = new ChangesContext();
        changes.getResourcesToAdd().add(m1);
        changes.getResourcesToAdd().add(m2);
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("DnsPointerTest-state-0.json");

        // Points to no machine
        DnsPointer dp = new DnsPointer("pointer.example.com");
        changes.getResourcesToAdd().add(dp);
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("DnsPointerTest-state-1.json");

        // Points to 1 machine
        changes.getLinksToAdd().add(new Tuple3<>(dp, LinkTypeConstants.POINTS_TO, m1));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("DnsPointerTest-state-2.json");

        // Points to 2 machines
        changes.getLinksToAdd().add(new Tuple3<>(dp, LinkTypeConstants.POINTS_TO, m2));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("DnsPointerTest-state-3.json");

        // Remove ip from m2
        m2 = resourceService.resourceFindByPk(m2).get();
        m2.setPublicIp(null);
        changes.getResourcesToUpdate().add(new Tuple2<>(m2.getInternalId(), m2));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("DnsPointerTest-state-4.json");

        // Put back ip to m2
        m2.setPublicIp("199.141.1.201");
        changes.getResourcesToUpdate().add(new Tuple2<>(m2.getInternalId(), m2));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("DnsPointerTest-state-3.json");

        // Rename
        dp = resourceService.resourceFindByPk(dp).get();
        changes.getResourcesToUpdate().add(new Tuple2<>(dp.getInternalId(), new DnsPointer("pointer2.example.com")));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);
        dp = resourceService.resourceFindByPk(new DnsPointer("pointer2.example.com")).get();

        assertState("DnsPointerTest-state-5.json");

        // Points to 1 machine
        changes.getLinksToDelete().add(new Tuple3<>(dp, LinkTypeConstants.POINTS_TO, m2));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("DnsPointerTest-state-6.json");

        // Point to 2 machines
        changes.getLinksToAdd().add(new Tuple3<>(dp, LinkTypeConstants.POINTS_TO, m2));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("DnsPointerTest-state-5.json");

        // Delete the second machine
        m2 = resourceService.resourceFindByPk(m2).get();
        changes.getResourcesToDelete().add(m2.getInternalId());
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("DnsPointerTest-state-7.json");

        // Delete
        changes.getResourcesToDelete().add(dp.getInternalId());
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("DnsPointerTest-state-8.json");
    }

    @Test
    public void testResourceMachine() {

        deleteAllResources();

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Create one
        ChangesContext changes = new ChangesContext();
        Machine machine = new Machine("m1.node.example.com", "199.141.1.101");
        changes.getResourcesToAdd().add(machine);
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("MachineTest-state-1.json");

        // Change IP
        machine = resourceService.resourceFindByPk(machine).get();
        machine.setPublicIp("199.141.1.102");
        changes.getResourcesToUpdate().add(new Tuple2<>(machine.getInternalId(), machine));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("MachineTest-state-2.json");

        // Remove IP
        machine.setPublicIp(null);
        changes.getResourcesToUpdate().add(new Tuple2<>(machine.getInternalId(), machine));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("MachineTest-state-3.json");

        // Put back IP
        machine.setPublicIp("199.141.1.102");
        changes.getResourcesToUpdate().add(new Tuple2<>(machine.getInternalId(), machine));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("MachineTest-state-2.json");

        // Change name (fail)
        machine.setName("m2.node.example.com");
        changes.getResourcesToUpdate().add(new Tuple2<>(machine.getInternalId(), machine));
        try {
            getInternalServicesContext().getInternalChangeService().changesExecute(changes);
            Assert.fail("Must fail since cannot change machine's name");
        } catch (IllegalUpdateException e) {
        }

        // Delete
        changes.getResourcesToDelete().add(machine.getInternalId());
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("MachineTest-state-4.json");

    }

    @Test
    public void testResourceUrlRedirection() {

        deleteAllResources();

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Create initial data
        Machine m1 = new Machine("m1.example.com", "199.141.1.101");
        Machine m2 = new Machine("m2.example.com", "199.141.1.201");
        ChangesContext changes = new ChangesContext();
        changes.getResourcesToAdd().add(m1);
        changes.getResourcesToAdd().add(m2);
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("UrlRedirectionTest-state-0.json");

        // Create a redirection
        UrlRedirection urlRedirection = new UrlRedirection();
        urlRedirection.setDomainName("redir.example.com");
        urlRedirection.setHttpRedirectToUrl("https://google.com");

        changes.getResourcesToAdd().add(urlRedirection);
        changes.getLinksToAdd().add(new Tuple3<>(urlRedirection, LinkTypeConstants.INSTALLED_ON, m1));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);
        urlRedirection = resourceService.resourceFindByPk(urlRedirection).get();

        assertState("UrlRedirectionTest-state-1.json");

        // Change the list of machines
        changes.getLinksToAdd().add(new Tuple3<>(urlRedirection, LinkTypeConstants.INSTALLED_ON, m2));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("UrlRedirectionTest-state-2.json");

        // Delete
        changes.getResourcesToDelete().add(urlRedirection.getInternalId());
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("UrlRedirectionTest-state-3.json");

    }

    @Test
    public void testResourceWebsite() {

        deleteAllResources();

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Create initial data
        Machine m1 = new Machine("m1.example.com", "199.141.1.101");
        Machine m2 = new Machine("m2.example.com", "199.141.1.201");
        WebsiteCertificate wc1 = createWebsiteCertificate("d1.example.com", "d2.example.com");
        wc1.setResourceEditorName("manual");
        WebsiteCertificate wc2 = createWebsiteCertificate("d3.example.com");
        wc2.setResourceEditorName("manual");
        ChangesContext changes = new ChangesContext();
        changes.getResourcesToAdd().add(m1);
        changes.getResourcesToAdd().add(m2);
        changes.getResourcesToAdd().add(wc1);
        changes.getResourcesToAdd().add(wc2);
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("WebsiteTest-state-0.json");

        // Create one, non-https
        Website website = new Website();
        website.getDomainNames().add("d1.example.com");
        changes.getResourcesToAdd().add(website);
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);
        website = resourceService.resourceFindByPk(website).get();

        assertState("WebsiteTest-state-1.json");

        // Add one application
        Application application = new Application();
        application.setName("my_web_app");
        changes.getResourcesToAdd().add(application);
        changes.getLinksToAdd().add(new Tuple3<>(website, LinkTypeConstants.POINTS_TO, application));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("WebsiteTest-state-2.json");

        // Change the list of machines
        changes.getLinksToAdd().add(new Tuple3<>(application, LinkTypeConstants.INSTALLED_ON, m1));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("WebsiteTest-state-3.json");

        // Change domain names
        website = resourceService.resourceFindByPk(website).get();
        website.getDomainNames().clear();
        website.getDomainNames().add("d2.example.com");
        changes.getResourcesToUpdate().add(new Tuple2<>(website.getInternalId(), website));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("WebsiteTest-state-4.json");

        // Change to https
        website = resourceService.resourceFindByPk(website).get();
        website.setHttps(true);
        changes.getResourcesToUpdate().add(new Tuple2<>(website.getInternalId(), website));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("WebsiteTest-state-5.json");

        // Change to http
        website = resourceService.resourceFindByPk(website).get();
        website.setHttps(false);
        changes.getResourcesToUpdate().add(new Tuple2<>(website.getInternalId(), website));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("WebsiteTest-state-4.json"); // Same as previous

        // Change to https
        website = resourceService.resourceFindByPk(website).get();
        website.setHttps(true);
        changes.getResourcesToUpdate().add(new Tuple2<>(website.getInternalId(), website));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("WebsiteTest-state-5.json");

        // Change to another domain with another cert
        website = resourceService.resourceFindByPk(website).get();
        website.getDomainNames().clear();
        website.getDomainNames().add("d3.example.com");
        changes.getResourcesToUpdate().add(new Tuple2<>(website.getInternalId(), website));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("WebsiteTest-state-6.json");

        // Change to another domain with a cert that does not exists yet
        website = resourceService.resourceFindByPk(website).get();
        website.getDomainNames().clear();
        website.getDomainNames().add("d4.example.com");
        changes.getResourcesToUpdate().add(new Tuple2<>(website.getInternalId(), website));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        Optional<WebsiteCertificate> websiteCertificateOptional = resourceService.resourceFind(resourceService.createResourceQuery(WebsiteCertificate.class) //
                .propertyEquals(WebsiteCertificate.PROPERTY_DOMAIN_NAMES, "d4.example.com"));
        Assert.assertTrue(websiteCertificateOptional.isPresent());
        WebsiteCertificate websiteCertificate = websiteCertificateOptional.get();
        websiteCertificate.setThumbprint("XXXXXXXXd4XXXXXX");
        websiteCertificate.setStart(DateTools.parseDateOnly("2001-07-01"));
        websiteCertificate.setEnd(DateTools.parseDateOnly("2001-08-01"));
        changes.getResourcesToUpdate().add(new Tuple2<>(websiteCertificate.getInternalId(), websiteCertificate));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("WebsiteTest-state-7.json");

        // Delete
        changes.getResourcesToDelete().add(website.getInternalId());
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("WebsiteTest-state-8.json");

    }

    @Test
    public void testResourceWebsiteCertificate() {

        deleteAllResources();

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        // Create a certificate
        AsymmetricKeys rootKeys = RSACrypt.RSA_CRYPT.generateKeyPair(1024);
        RSACertificate rsaCert = new RSACertificate(rootKeys);
        rsaCert.selfSign(new CertificateDetails() //
                .setStartDate(DateTools.parseDateOnly("2001-01-01")) //
                .setEndDate(DateTools.parseDateOnly("2002-01-01")) //
                .addSanDns("m1.example.com", "m2.example.com") //
        );

        WebsiteCertificate c1 = CertificateHelper.toWebsiteCertificate(null, rsaCert);
        c1.setThumbprint("my_thumb");
        ChangesContext changes = new ChangesContext();
        changes.getResourcesToAdd().add(c1);
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);
        c1 = resourceService.resourceFindByPk(c1).get();

        assertState("WebsiteCertificateTest-state-1.json");

        // Change the list of domains
        rsaCert = new RSACertificate(rootKeys);
        rsaCert.selfSign(new CertificateDetails() //
                .setStartDate(DateTools.parseDateOnly("2001-01-01")) //
                .setEndDate(DateTools.parseDateOnly("2002-01-01")) //
                .addSanDns("m3.example.com", "m2.example.com") //
        );

        WebsiteCertificate c2 = CertificateHelper.toWebsiteCertificate(null, rsaCert);
        c2.setThumbprint("my_thumb");
        changes.getResourcesToUpdate().add(new Tuple2<>(c1.getInternalId(), c2));
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("WebsiteCertificateTest-state-2.json");

        // Delete
        changes.getResourcesToDelete().add(c1.getInternalId());
        getInternalServicesContext().getInternalChangeService().changesExecute(changes);

        assertState("WebsiteCertificateTest-state-3.json");

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
