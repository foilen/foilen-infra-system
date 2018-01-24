/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.exception.ProblemException;
import com.foilen.infra.plugin.v1.core.visual.PageDefinition;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.ResourceFieldPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.ResourcesFieldPageItem;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.google.common.base.Strings;

public class CommonResourceLink {

    /**
     * Create a {@link ResourceFieldPageItem} for the specified link.
     *
     * @param servicesCtx
     *            the services
     * @param pageDefinition
     *            the page definition on which to add the item
     * @param editedResource
     *            the currently edited resource
     * @param linkType
     *            the type of link. Can be one from {@link LinkTypeConstants} or any other String.
     * @param resourceType
     *            the type of resource for the link
     * @param labelCode
     *            the code for the label to display
     * @param fieldName
     *            the name of the field that contains the id of the resource to link
     */
    public static <L extends IPResource> void addResourcePageItem(CommonServicesContext servicesCtx, PageDefinition pageDefinition, IPResource editedResource, String linkType, Class<L> resourceType,
            String labelCode, String fieldName) {
        ResourceFieldPageItem<L> pageItem = new ResourceFieldPageItem<>();
        pageItem.setFieldName(fieldName);
        pageItem.setLabel(servicesCtx.getTranslationService().translate(labelCode));
        pageItem.setResourceType(resourceType);

        if (editedResource != null) {
            List<L> resources = servicesCtx.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(editedResource, linkType, resourceType);
            if (!resources.isEmpty()) {
                if (resources.size() > 1) {
                    throw new ProblemException("Too many links of type [" + linkType + "]");
                }
                L resource = resources.get(0);
                pageItem.setValue(resource);
            }
        }

        pageDefinition.addPageItem(pageItem);
    }

    /**
     * Create a {@link ResourcesFieldPageItem} for the specified link.
     *
     * @param servicesCtx
     *            the services
     * @param pageDefinition
     *            the page definition on which to add the item
     * @param editedResource
     *            the currently edited resource
     * @param linkType
     *            the type of link. Can be one from {@link LinkTypeConstants} or any other String.
     * @param resourceType
     *            the type of resource for the link
     * @param labelCode
     *            the code for the label to display
     * @param fieldName
     *            the name of the field that contains the ids of the resources to link
     */
    public static <L extends IPResource> void addResourcesPageItem(CommonServicesContext servicesCtx, PageDefinition pageDefinition, IPResource editedResource, String linkType, Class<L> resourceType,
            String labelCode, String fieldName) {

        ResourcesFieldPageItem<L> pageItem = new ResourcesFieldPageItem<>();
        pageItem.setFieldName(fieldName);
        pageItem.setLabel(servicesCtx.getTranslationService().translate(labelCode));
        pageItem.setResourceType(resourceType);

        if (editedResource != null) {
            List<L> resources = servicesCtx.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(editedResource, linkType, resourceType);
            if (!resources.isEmpty()) {
                pageItem.setValues(resources);
            }
        }

        pageDefinition.addPageItem(pageItem);
    }

    /**
     * Set the link on "editedResource" that goes to the resource with id set by "fieldName". (will remove previous unneeded links)
     *
     * @param servicesCtx
     *            the services
     * @param editedResource
     *            the currently edited resource
     * @param linkType
     *            the link type. Can be one from {@link LinkTypeConstants} or any other String
     * @param toResourceType
     *            the type of the resource that gets linked.
     * @param fieldName
     *            the name of the field that contains the id of the resource to link
     * @param formValues
     *            the form
     * @param changesContext
     *            the change context where to add the modifications
     */
    public static <L extends IPResource> void fillResourceLink(CommonServicesContext servicesCtx, IPResource editedResource, String linkType, Class<L> toResourceType, String fieldName,
            Map<String, String> formValues, ChangesContext changesContext) {

        String value = formValues.get(fieldName);
        if (value == null) {
            // Remove previous links
            if (editedResource.getInternalId() != null) {
                List<L> currentLinks = servicesCtx.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(editedResource, linkType, toResourceType);
                currentLinks.stream() //
                        .forEach(it -> {
                            changesContext.linkDelete(editedResource, linkType, it);
                        });
            }
        } else {
            Long linkedResourceId;
            try {
                linkedResourceId = Long.parseLong(value);
            } catch (Exception e) {
                throw new ProblemException("The link id is not numerical", e);
            }

            Optional<? extends IPResource> linkedResourceOptional = servicesCtx.getResourceService().resourceFind( //
                    servicesCtx.getResourceService().createResourceQuery(toResourceType) //
                            .addIdEquals(linkedResourceId) //
            );
            if (!linkedResourceOptional.isPresent()) {
                throw new ProblemException("The linked resource does not exist");
            }

            IPResource finalLink = linkedResourceOptional.get();

            // Remove previous links if not the right one
            List<L> currentLinks;
            if (editedResource.getInternalId() == null) {
                currentLinks = new ArrayList<>();
            } else {
                currentLinks = servicesCtx.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(editedResource, linkType, toResourceType);
                currentLinks.stream() //
                        .filter(it -> !finalLink.equals(it)) //
                        .forEach(it -> {
                            changesContext.linkDelete(editedResource, linkType, it);
                        });
            }

            // Add the new links if not the right ones or there were none
            if (!currentLinks.contains(finalLink)) {
                changesContext.linkAdd(editedResource, linkType, finalLink);
            }

        }

    }

    /**
     * Set the links on "editedResource" that goes to the resources with ids set by "fieldName". (will remove previous unneeded links)
     *
     * @param servicesCtx
     *            the services
     * @param editedResource
     *            the currently edited resource
     * @param linkType
     *            the link type. Can be one from {@link LinkTypeConstants} or any other String.
     * @param toResourceType
     *            the type of the resource that gets linked
     * @param fieldName
     *            the name of the field that contains the ids of the resources to link
     * @param formValues
     *            the form
     * @param changesContext
     *            the change context where to add the modifications
     */
    public static <L extends IPResource> void fillResourcesLink(CommonServicesContext servicesCtx, IPResource editedResource, String linkType, Class<L> toResourceType, String fieldName,
            Map<String, String> formValues, ChangesContext changesContext) {

        String values = formValues.get(fieldName);
        if (values == null) {
            // Remove previous links
            if (editedResource.getInternalId() != null) {
                List<L> currentLinks = servicesCtx.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(editedResource, linkType, toResourceType);
                currentLinks.stream() //
                        .forEach(it -> {
                            changesContext.linkDelete(editedResource, linkType, it);
                        });
            }
        } else {
            String[] valuesParts = values.split(",");
            long[] linkedResourceIds = new long[valuesParts.length];
            try {
                int idx = 0;
                for (String valuePart : valuesParts) {
                    if (!Strings.isNullOrEmpty(valuePart)) {
                        linkedResourceIds[idx++] = Long.parseLong(valuePart);
                    }
                }
            } catch (Exception e) {
                throw new ProblemException("The link id is not numerical", e);
            }

            List<? extends IPResource> finalLinks = servicesCtx.getResourceService().resourceFindAll( //
                    servicesCtx.getResourceService().createResourceQuery(toResourceType) //
                            .addIdEquals(linkedResourceIds) //
            );

            // Remove previous links if not the right one
            List<L> currentLinks;
            if (editedResource.getInternalId() == null) {
                currentLinks = new ArrayList<>();
            } else {
                currentLinks = servicesCtx.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(editedResource, linkType, toResourceType);
                currentLinks.stream() //
                        .filter(it -> !finalLinks.contains(it)) //
                        .forEach(it -> {
                            changesContext.linkDelete(editedResource, linkType, it);
                        });
            }

            // Add the new links if not the right ones or there were none
            finalLinks.stream() //
                    .filter(it -> !currentLinks.contains(it)) //
                    .forEach(it -> {
                        changesContext.linkAdd(editedResource, linkType, it);
                    });
        }

    }

    private CommonResourceLink() {
    }
}
