/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.junits;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import com.foilen.infra.plugin.core.system.fake.controller.ResourcesController;
import com.foilen.infra.plugin.core.system.fake.service.FakeSystemServicesImpl;
import com.foilen.infra.plugin.core.system.junits.ResourceState;
import com.foilen.infra.plugin.core.system.junits.ResourcesState;
import com.foilen.infra.plugin.core.system.junits.ResourcesStateLink;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tuple.Tuple2;

/**
 * Extends to test your plugin.
 */
public abstract class AbstractIPPluginTest extends AbstractBasics {

    private FakeSystemServicesImpl fakeSystemServicesImpl;

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
            ChangesContext changesContext = new ChangesContext();
            resourceEditor.fillResource(getCommonServicesContext(), changesContext, formValues, resource);

            // Add or update
            if (internalId == null) {
                changesContext.getResourcesToAdd().add(resource);
            } else {
                changesContext.getResourcesToUpdate().add(new Tuple2<>(internalId, resource));
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

    protected void assertSet(Set<String> actualTags, String... expectedTags) {
        Assert.assertEquals(expectedTags.length, actualTags.size());
        Assert.assertTrue(actualTags.containsAll(Arrays.asList(expectedTags)));
    }

    protected void assertState(String resourceName, Class<?> resourceContext) {
        ResourcesState resourcesState = new ResourcesState();

        resourcesState.setResources(fakeSystemServicesImpl.getResources().stream() //
                .map(resource -> {
                    ResourceState resourceState = new ResourceState(getResourceDetails(resource));

                    // Links
                    List<ResourcesStateLink> links = fakeSystemServicesImpl.linkFindAllByFromResource(resource).stream() //
                            .map(link -> new ResourcesStateLink(link.getA(), getResourceDetails(link.getB()))) //
                            .collect(Collectors.toList());
                    resourceState.setLinks(links);

                    // Tags
                    resourceState.setTags(fakeSystemServicesImpl.tagFindAllByResource(resource).stream().sorted().collect(Collectors.toList()));

                    return resourceState;
                }) //
                .collect(Collectors.toList()));

        resourcesState.sort();

        AssertTools.assertJsonComparison(resourceName, resourceContext, resourcesState);

    }

    protected CommonServicesContext getCommonServicesContext() {
        return fakeSystemServicesImpl.getCommonServicesContext();
    }

    protected InternalServicesContext getInternalServicesContext() {
        return fakeSystemServicesImpl.getInternalServicesContext();
    }

    protected String getResourceDetails(IPResource resource) {
        return resource.getClass().getSimpleName() + " | " + resource.getResourceName() + " | " + resource.getResourceDescription();
    }

    @Before
    public void init() {
        fakeSystemServicesImpl = FakeSystemServicesTests.init();
    }

}