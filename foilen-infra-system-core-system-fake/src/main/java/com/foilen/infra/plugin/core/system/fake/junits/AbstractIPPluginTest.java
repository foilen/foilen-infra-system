/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.junits;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import com.foilen.infra.plugin.core.system.fake.controller.ResourcesController;
import com.foilen.infra.plugin.core.system.fake.service.FakeSystemServicesImpl;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.visual.PageDefinition;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.google.common.base.Strings;

/**
 * Extends to test your plugin.
 */
public abstract class AbstractIPPluginTest extends AbstractBasics {

    protected FakeSystemServicesImpl fakeSystemServicesImpl;

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
                formValues.put(ResourcesController.RESOURCE_ID_FIELD, String.valueOf(internalId));
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
                resource = resourceEditor.getForResourceType().newInstance();
            } else {
                resource = (T) getCommonServicesContext().getResourceService().resourceFind(internalId).get();
            }

            // Fill
            ChangesContext changesContext = new ChangesContext(fakeSystemServicesImpl);
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
     * Load the page definition.
     *
     * @param editorName
     *            the editor to use
     * @param editedResource
     *            the resource to load
     * @param expectedResource
     *            the filename of the resource
     * @param expectedContext
     *            the class in which the resource file is
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void assertEditorPageDefinition(String editorName, IPResource editedResource, String expectedResource, Class<?> expectedContext) {

        Optional editorOptional = getCommonServicesContext().getPluginService().getResourceEditorByName(editorName);

        ResourceEditor editor = (ResourceEditor) editorOptional.get();

        PageDefinition pageDefinition = editor.providePageDefinition(getCommonServicesContext(), editedResource);

        AssertTools.assertJsonComparison(expectedResource, expectedContext, pageDefinition);

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
            formValues.put(ResourcesController.RESOURCE_ID_FIELD, String.valueOf(internalId));
        }
        resourceEditor.formatForm(getCommonServicesContext(), formValues);
        List<Tuple2<String, String>> errors = resourceEditor.validateForm(getCommonServicesContext(), formValues);

        AssertTools.assertJsonComparison(Arrays.asList(expectedErrors), errors);

    }

    /**
     * Assert the amount of resources of the specified type.
     *
     * @param expectedCount
     *            the expected count
     * @param resourceType
     *            the resource type
     */
    protected void assertResourceCount(int expectedCount, Class<? extends IPResource> resourceType) {

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

    protected <T extends IPResource> T assertResourceExists(boolean expectedExists, T resourcePk, Class<T> resourceClass) {

        IPResourceService resourceService = getCommonServicesContext().getResourceService();

        Optional<T> resourceFindOne = resourceService.resourceFindByPk(resourcePk);
        Assert.assertEquals(expectedExists, resourceFindOne.isPresent());

        if (resourceFindOne.isPresent()) {
            return resourceFindOne.get();
        } else {
            return null;
        }
    }

    protected void assertSet(Set<String> actualItems, String... expectedItems) {
        Assert.assertEquals(expectedItems.length, actualItems.size());
        Assert.assertTrue(actualItems.containsAll(Arrays.asList(expectedItems)));
    }

    protected CommonServicesContext getCommonServicesContext() {
        return fakeSystemServicesImpl.getCommonServicesContext();
    }

    protected InternalServicesContext getInternalServicesContext() {
        return fakeSystemServicesImpl.getInternalServicesContext();
    }

    @Before
    public void init() {
        fakeSystemServicesImpl = FakeSystemServicesTests.init();
    }

    /**
     * When there is a value that is changing between unit tests run and you just want to check if it is not null or empty, call this method and the returned value will be: null, "" or "--IS SET--".
     *
     * @param value
     *            the value to check
     * @return null, "" or "--IS SET--"
     */
    protected String notNullOrEmptyToIsSet(String value) {
        if (!Strings.isNullOrEmpty(value)) {
            return "--IS SET--";
        }
        return value;
    }

}