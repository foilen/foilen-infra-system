/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.helper;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.foilen.infra.plugin.core.system.fake.junits.FakeSystemServicesTests;
import com.foilen.infra.plugin.core.system.fake.service.FakeSystemServicesImpl;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.visual.FieldPageItem;
import com.foilen.infra.plugin.v1.core.visual.PageDefinition;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.ResourceFieldPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.ResourcesFieldPageItem;
import com.foilen.infra.plugin.v1.example.resource.EmployeeResource;
import com.foilen.smalltools.tuple.Tuple3;

public class CommonResourceLinkTest {

    private static final String FIELD_NAME_MANAGER = "managerId";

    private FakeSystemServicesImpl fakeSystemServicesImpl;
    private CommonServicesContext commonServicesContext;
    private InternalServicesContext internalServicesContext;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public CommonResourceLinkTest() {
        fakeSystemServicesImpl = FakeSystemServicesTests.init();
        commonServicesContext = fakeSystemServicesImpl.getCommonServicesContext();
        internalServicesContext = fakeSystemServicesImpl.getInternalServicesContext();
    }

    private EmployeeResource getEmployee(String firstName) {
        return fakeSystemServicesImpl.resourceFind( //
                fakeSystemServicesImpl.createResourceQuery(EmployeeResource.class) //
                        .propertyEquals(EmployeeResource.PROPERTY_FIRST_NAME, firstName) //
        ).get();
    }

    @SuppressWarnings("unchecked")
    private EmployeeResource getManagerFromResourceFieldPageItem(PageDefinition pageDefinition) {
        Optional<FieldPageItem> fieldPageItemOptional = pageDefinition.getField(FIELD_NAME_MANAGER);
        ResourceFieldPageItem<EmployeeResource> managerResourceFieldPageItem = (ResourceFieldPageItem<EmployeeResource>) fieldPageItemOptional.get();
        return managerResourceFieldPageItem.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<EmployeeResource> getManagerFromResourcesFieldPageItem(PageDefinition pageDefinition) {
        Optional<FieldPageItem> fieldPageItemOptional = pageDefinition.getField(FIELD_NAME_MANAGER);
        ResourcesFieldPageItem<EmployeeResource> managerResourceFieldPageItem = (ResourcesFieldPageItem<EmployeeResource>) fieldPageItemOptional.get();
        return managerResourceFieldPageItem.getValues();
    }

    @Before
    public void init() {

        // Alain
        // -> Bernard
        // -> Cecille
        // Bernard
        // -> Donald

        ChangesContext changes = new ChangesContext();
        EmployeeResource alain = new EmployeeResource("Alain", "Smith", new Date());
        EmployeeResource bernard = new EmployeeResource("Bernard", "Smith", new Date());
        EmployeeResource cecille = new EmployeeResource("Cecille", "Smith", new Date());
        EmployeeResource donald = new EmployeeResource("Donald", "Smith", new Date());
        changes.getResourcesToAdd().add(alain);
        changes.getResourcesToAdd().add(bernard);
        changes.getResourcesToAdd().add(cecille);
        changes.getResourcesToAdd().add(donald);
        changes.getLinksToAdd().add(new Tuple3<>(bernard, EmployeeResource.LINK_TYPE_MANAGER, alain));
        changes.getLinksToAdd().add(new Tuple3<>(cecille, EmployeeResource.LINK_TYPE_MANAGER, alain));
        changes.getLinksToAdd().add(new Tuple3<>(donald, EmployeeResource.LINK_TYPE_MANAGER, bernard));
        internalServicesContext.getInternalChangeService().changesExecute(changes);
    }

    @Test
    public void testAddResourcePageItem_hasMany() {

        thrown.expectMessage("Too many links of type [MANAGER]");

        // Add more to one
        ChangesContext changes = new ChangesContext();
        EmployeeResource bernardResource = new EmployeeResource("Bernard", "Smith", new Date());
        changes.getLinksToAdd().add(new Tuple3<>(bernardResource, EmployeeResource.LINK_TYPE_MANAGER, new EmployeeResource("Cecille", "Smith", new Date())));
        internalServicesContext.getInternalChangeService().changesExecute(changes);

        // Execute
        PageDefinition pageDefinition = new PageDefinition("");
        EmployeeResource bernard = getEmployee("Bernard");
        CommonResourceLink.addResourcePageItem(commonServicesContext, pageDefinition, bernard, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class, "", FIELD_NAME_MANAGER);

    }

    @Test
    public void testAddResourcePageItem_hasNone() {
        PageDefinition pageDefinition = new PageDefinition("");
        EmployeeResource alain = getEmployee("Alain");
        CommonResourceLink.addResourcePageItem(commonServicesContext, pageDefinition, alain, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class, "", FIELD_NAME_MANAGER);

        EmployeeResource managerResource = getManagerFromResourceFieldPageItem(pageDefinition);
        Assert.assertNull(managerResource);
    }

    @Test
    public void testAddResourcePageItem_hasOne() {
        PageDefinition pageDefinition = new PageDefinition("");
        EmployeeResource bernard = getEmployee("Bernard");
        CommonResourceLink.addResourcePageItem(commonServicesContext, pageDefinition, bernard, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class, "", FIELD_NAME_MANAGER);

        EmployeeResource managerResource = getManagerFromResourceFieldPageItem(pageDefinition);
        Assert.assertEquals("Alain", managerResource.getFirstName());
    }

    @Test
    public void testAddResourcesPageItem_hasNone() {
        PageDefinition pageDefinition = new PageDefinition("");
        EmployeeResource alain = getEmployee("Alain");
        CommonResourceLink.addResourcesPageItem(commonServicesContext, pageDefinition, alain, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class, "", FIELD_NAME_MANAGER);

        List<EmployeeResource> managersResource = getManagerFromResourcesFieldPageItem(pageDefinition);
        Assert.assertTrue(managersResource.isEmpty());
    }

    @Test
    public void testAddResourcesPageItem_hasOne() {
        PageDefinition pageDefinition = new PageDefinition("");
        EmployeeResource bernard = getEmployee("Bernard");
        CommonResourceLink.addResourcesPageItem(commonServicesContext, pageDefinition, bernard, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class, "", FIELD_NAME_MANAGER);

        List<EmployeeResource> managersResource = getManagerFromResourcesFieldPageItem(pageDefinition);
        Assert.assertEquals(1, managersResource.size());
        Assert.assertEquals("Alain", managersResource.get(0).getFirstName());
    }

    @Test
    public void testFillResourceLink_Change() {
        // Check initial value
        EmployeeResource bernard = getEmployee("Bernard");
        List<EmployeeResource> links = commonServicesContext.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(bernard, EmployeeResource.LINK_TYPE_MANAGER,
                EmployeeResource.class);
        Assert.assertEquals(1, links.size());
        Assert.assertEquals("Alain", links.get(0).getFirstName());

        // Change
        Map<String, String> formValues = new HashMap<>();
        formValues.put(FIELD_NAME_MANAGER, String.valueOf(getEmployee("Cecille").getInternalId()));
        ChangesContext changesContext = new ChangesContext();
        CommonResourceLink.fillResourceLink(commonServicesContext, bernard, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class, FIELD_NAME_MANAGER, formValues, changesContext);
        fakeSystemServicesImpl.getInternalServicesContext().getInternalChangeService().changesExecute(changesContext);

        // Check changed value
        links = commonServicesContext.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(bernard, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class);
        Assert.assertEquals(1, links.size());
        Assert.assertEquals("Cecille", links.get(0).getFirstName());
    }

    @Test
    public void testFillResourceLink_Remove() {
        // Check initial value
        EmployeeResource bernard = getEmployee("Bernard");
        List<EmployeeResource> links = commonServicesContext.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(bernard, EmployeeResource.LINK_TYPE_MANAGER,
                EmployeeResource.class);
        Assert.assertEquals(1, links.size());
        Assert.assertEquals("Alain", links.get(0).getFirstName());

        // Change
        Map<String, String> formValues = new HashMap<>();
        formValues.put(FIELD_NAME_MANAGER, null);
        ChangesContext changesContext = new ChangesContext();
        CommonResourceLink.fillResourceLink(commonServicesContext, bernard, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class, FIELD_NAME_MANAGER, formValues, changesContext);
        fakeSystemServicesImpl.getInternalServicesContext().getInternalChangeService().changesExecute(changesContext);

        // Check changed value
        links = commonServicesContext.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(bernard, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class);
        Assert.assertTrue(links.isEmpty());
    }

    @Test
    public void testFillResourcesLink_0() {
        // Check initial value
        EmployeeResource bernard = getEmployee("Bernard");
        List<EmployeeResource> links = commonServicesContext.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(bernard, EmployeeResource.LINK_TYPE_MANAGER,
                EmployeeResource.class);
        Assert.assertEquals(1, links.size());
        Assert.assertEquals("Alain", links.get(0).getFirstName());

        // Change
        Map<String, String> formValues = new HashMap<>();
        formValues.put(FIELD_NAME_MANAGER, null);
        ChangesContext changesContext = new ChangesContext();
        CommonResourceLink.fillResourceLink(commonServicesContext, bernard, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class, FIELD_NAME_MANAGER, formValues, changesContext);
        fakeSystemServicesImpl.getInternalServicesContext().getInternalChangeService().changesExecute(changesContext);

        // Check changed value
        links = commonServicesContext.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(bernard, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class);
        Assert.assertTrue(links.isEmpty());
    }

    @Test
    public void testFillResourcesLink_1() {
        // Check initial value
        EmployeeResource bernard = getEmployee("Bernard");
        List<EmployeeResource> links = commonServicesContext.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(bernard, EmployeeResource.LINK_TYPE_MANAGER,
                EmployeeResource.class);
        Assert.assertEquals(1, links.size());
        Assert.assertEquals("Alain", links.get(0).getFirstName());

        // Change
        Map<String, String> formValues = new HashMap<>();
        formValues.put(FIELD_NAME_MANAGER, String.valueOf(getEmployee("Cecille").getInternalId()));
        ChangesContext changesContext = new ChangesContext();
        CommonResourceLink.fillResourceLink(commonServicesContext, bernard, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class, FIELD_NAME_MANAGER, formValues, changesContext);
        fakeSystemServicesImpl.getInternalServicesContext().getInternalChangeService().changesExecute(changesContext);

        // Check changed value
        links = commonServicesContext.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(bernard, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class);
        Assert.assertEquals(1, links.size());
        Assert.assertEquals("Cecille", links.get(0).getFirstName());
    }

    @Test
    public void testFillResourcesLink_2() {
        // Check initial value
        EmployeeResource bernard = getEmployee("Bernard");
        List<EmployeeResource> links = commonServicesContext.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(bernard, EmployeeResource.LINK_TYPE_MANAGER,
                EmployeeResource.class);
        Assert.assertEquals(1, links.size());
        Assert.assertEquals("Alain", links.get(0).getFirstName());

        // Add
        Map<String, String> formValues = new HashMap<>();
        formValues.put(FIELD_NAME_MANAGER, String.valueOf(getEmployee("Alain").getInternalId()) + "," + String.valueOf(getEmployee("Cecille").getInternalId()));
        ChangesContext changesContext = new ChangesContext();
        CommonResourceLink.fillResourcesLink(commonServicesContext, bernard, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class, FIELD_NAME_MANAGER, formValues, changesContext);
        fakeSystemServicesImpl.getInternalServicesContext().getInternalChangeService().changesExecute(changesContext);

        // Check changed value
        links = commonServicesContext.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(bernard, EmployeeResource.LINK_TYPE_MANAGER, EmployeeResource.class);
        Assert.assertEquals(2, links.size());
        Assert.assertEquals("Alain", links.get(0).getFirstName());
        Assert.assertEquals("Cecille", links.get(1).getFirstName());
    }

}
