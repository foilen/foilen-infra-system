/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.editor.simpleresourceditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.exception.ProblemException;
import com.foilen.infra.plugin.v1.core.visual.PageDefinition;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonFieldHelper;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonPageItem;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonValidation;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.AbstractFieldPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.ListInputTextFieldPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.ResourceFieldPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.ResourcesFieldPageItem;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tuple.Tuple2;
import com.google.common.base.Strings;

public class SimpleResourceEditorDefinition {

    private CommonServicesContext servicesCtx;
    private String prefix;

    private IPResource editedResource;
    private BeanWrapper beanWrapper;
    private List<SimpleResourceEditorDefinitionFieldConfig> fieldConfigs = new ArrayList<>();

    public SimpleResourceEditorDefinition(ResourceEditor<?> resourceEditor, CommonServicesContext servicesCtx, IPResource editedResource) {
        this.servicesCtx = servicesCtx;
        this.editedResource = editedResource;
        prefix = resourceEditor.getClass().getSimpleName();

        if (editedResource != null) {
            beanWrapper = new BeanWrapperImpl(editedResource);
        }

    }

    public void addFieldConfig(Consumer<SimpleResourceEditorDefinitionFieldConfig> fieldConfigConsumer) {
        SimpleResourceEditorDefinitionFieldConfig fieldConfig = new SimpleResourceEditorDefinitionFieldConfig();
        fieldConfigConsumer.accept(fieldConfig);
        fieldConfigs.add(fieldConfig);
    }

    public void addInputText(String propertyName) {
        addInputText(propertyName, (fc) -> {
        });
    }

    public void addInputText(String propertyName, Consumer<SimpleResourceEditorDefinitionFieldConfig> fieldConfigConsumer) {
        SimpleResourceEditorDefinitionFieldConfig fieldConfig = new SimpleResourceEditorDefinitionFieldConfig();
        fieldConfig.setPropertyName(propertyName);
        fieldConfig.setGenAndAddPageItem((pageDefinition) -> CommonPageItem.createInputTextField(servicesCtx, pageDefinition, prefix + "." + propertyName, propertyName));
        fieldConfigConsumer.accept(fieldConfig);
        fieldConfigs.add(fieldConfig);
    }

    public void addListInputText(String propertyName) {
        addListInputText(propertyName, (fc) -> {
        });
    }

    @SuppressWarnings("unchecked")
    public void addListInputText(String propertyName, Consumer<SimpleResourceEditorDefinitionFieldConfig> fieldConfigConsumer) {
        SimpleResourceEditorDefinitionFieldConfig fieldConfig = new SimpleResourceEditorDefinitionFieldConfig();
        fieldConfig.setPropertyName(propertyName);
        fieldConfig.setGenAndAddPageItem((pageDefinition) -> CommonPageItem.createListInputTextFieldPageItem(servicesCtx, pageDefinition, prefix + "." + propertyName, propertyName));
        fieldConfig.setPopulatePageItem(ctx -> ((ListInputTextFieldPageItem) ctx.getPageItem()).setFieldValues(CommonFieldHelper.fromSetToList((Set<String>) ctx.getPropertyValue())));
        fieldConfig.setPopulateResource(ctx -> ctx.getEditedResourceBeanWrapper().setPropertyValue(ctx.getPropertyName(), ctx.getTextValues()));
        fieldConfigConsumer.accept(fieldConfig);
        fieldConfigs.add(fieldConfig);
    }

    public void addMultilineInputText(String propertyName) {
        addMultilineInputText(propertyName, (fc) -> {
        });
    }

    public void addMultilineInputText(String propertyName, Consumer<SimpleResourceEditorDefinitionFieldConfig> fieldConfigConsumer) {
        SimpleResourceEditorDefinitionFieldConfig fieldConfig = new SimpleResourceEditorDefinitionFieldConfig();
        fieldConfig.setPropertyName(propertyName);
        fieldConfig.setGenAndAddPageItem((pageDefinition) -> CommonPageItem.createMultilineInputTextField(servicesCtx, pageDefinition, prefix + "." + propertyName, propertyName));
        fieldConfigConsumer.accept(fieldConfig);
        fieldConfigs.add(fieldConfig);
    }

    public <R extends IPResource> void addResource(String propertyName, String linkType, Class<R> resourceType) {
        addResource(propertyName, linkType, resourceType, (fc) -> {
        });
    }

    @SuppressWarnings("unchecked")
    public <R extends IPResource> void addResource(String propertyName, String linkType, Class<R> toResourceType, Consumer<SimpleResourceEditorDefinitionFieldConfig> fieldConfigConsumer) {
        SimpleResourceEditorDefinitionFieldConfig fieldConfig = new SimpleResourceEditorDefinitionFieldConfig();
        fieldConfig.setPropertyName(propertyName);
        fieldConfig.setGenAndAddPageItem((pageDefinition) -> {
            ResourceFieldPageItem<R> pageItem = new ResourceFieldPageItem<>();
            pageItem.setFieldName(propertyName);
            pageItem.setLabel(servicesCtx.getTranslationService().translate(prefix + "." + propertyName));
            pageItem.setResourceType(toResourceType);
            pageDefinition.addPageItem(pageItem);
            return pageItem;
        });
        fieldConfig.setPopulatePageItem(ctx -> {
            List<R> resources = servicesCtx.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(editedResource, linkType, toResourceType);
            if (!resources.isEmpty()) {
                if (resources.size() > 1) {
                    throw new ProblemException("Too many links of type [" + linkType + "]");
                }
                R resource = resources.get(0);
                ((ResourceFieldPageItem<R>) ctx.getPageItem()).setValue(resource);
            }
        });
        fieldConfig.setPopulateResource(ctx -> {
            String value = ctx.getTextValue();
            if (value == null) {
                // Remove previous links
                if (editedResource.getInternalId() != null) {
                    List<R> currentLinks = servicesCtx.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(editedResource, linkType, toResourceType);
                    currentLinks.stream() //
                            .forEach(it -> {
                                ctx.getChangesContext().linkDelete(editedResource, linkType, it);
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
                List<R> currentLinks;
                if (editedResource.getInternalId() == null) {
                    currentLinks = new ArrayList<>();
                } else {
                    currentLinks = servicesCtx.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(editedResource, linkType, toResourceType);
                    currentLinks.stream() //
                            .filter(it -> !finalLink.equals(it)) //
                            .forEach(it -> {
                                ctx.getChangesContext().linkDelete(editedResource, linkType, it);
                            });
                }

                // Add the new links if not the right ones or there were none
                if (!currentLinks.contains(finalLink)) {
                    ctx.getChangesContext().linkAdd(editedResource, linkType, finalLink);
                }

            }

        });
        fieldConfigConsumer.accept(fieldConfig);
        fieldConfigs.add(fieldConfig);
    }

    public <R extends IPResource> void addResources(String propertyName, String linkType, Class<R> resourceType) {
        addResources(propertyName, linkType, resourceType, (fc) -> {
        });
    }

    @SuppressWarnings("unchecked")
    public <R extends IPResource> void addResources(String propertyName, String linkType, Class<R> toResourceType, Consumer<SimpleResourceEditorDefinitionFieldConfig> fieldConfigConsumer) {
        SimpleResourceEditorDefinitionFieldConfig fieldConfig = new SimpleResourceEditorDefinitionFieldConfig();
        fieldConfig.setPropertyName(propertyName);
        fieldConfig.setGenAndAddPageItem((pageDefinition) -> {
            ResourcesFieldPageItem<R> pageItem = new ResourcesFieldPageItem<>();
            pageItem.setFieldName(propertyName);
            pageItem.setLabel(servicesCtx.getTranslationService().translate(prefix + "." + propertyName));
            pageItem.setResourceType(toResourceType);
            pageDefinition.addPageItem(pageItem);
            return pageItem;
        });
        fieldConfig.setPopulatePageItem(ctx -> {
            List<R> resources = servicesCtx.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(editedResource, linkType, toResourceType);
            if (!resources.isEmpty()) {
                ((ResourcesFieldPageItem<R>) ctx.getPageItem()).setValues(resources);
            }
        });
        fieldConfig.setPopulateResource(ctx -> {
            String values = ctx.getTextValue();
            if (values == null) {
                // Remove previous links
                if (editedResource.getInternalId() != null) {
                    List<R> currentLinks = servicesCtx.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(editedResource, linkType, toResourceType);
                    currentLinks.stream() //
                            .forEach(it -> {
                                ctx.getChangesContext().linkDelete(editedResource, linkType, it);
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
                List<R> currentLinks;
                if (editedResource.getInternalId() == null) {
                    currentLinks = new ArrayList<>();
                } else {
                    currentLinks = servicesCtx.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(editedResource, linkType, toResourceType);
                    currentLinks.stream() //
                            .filter(it -> !finalLinks.contains(it)) //
                            .forEach(it -> {
                                ctx.getChangesContext().linkDelete(editedResource, linkType, it);
                            });
                }

                // Add the new links if not the right ones or there were none
                finalLinks.stream() //
                        .filter(it -> !currentLinks.contains(it)) //
                        .forEach(it -> {
                            ctx.getChangesContext().linkAdd(editedResource, linkType, it);
                        });
            }

        });
        fieldConfigConsumer.accept(fieldConfig);
        fieldConfigs.add(fieldConfig);
    }

    @SuppressWarnings("unchecked")
    public <R extends IPResource> void addReverseResource(String propertyName, Class<R> fromResourceType, String linkType, Consumer<SimpleResourceEditorDefinitionFieldConfig> fieldConfigConsumer) {
        SimpleResourceEditorDefinitionFieldConfig fieldConfig = new SimpleResourceEditorDefinitionFieldConfig();
        fieldConfig.setPropertyName(propertyName);
        fieldConfig.setGenAndAddPageItem((pageDefinition) -> {
            ResourceFieldPageItem<R> pageItem = new ResourceFieldPageItem<>();
            pageItem.setFieldName(propertyName);
            pageItem.setLabel(servicesCtx.getTranslationService().translate(prefix + "." + propertyName));
            pageItem.setResourceType(fromResourceType);
            pageDefinition.addPageItem(pageItem);
            return pageItem;
        });
        fieldConfig.setPopulatePageItem(ctx -> {
            List<R> resources = servicesCtx.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(fromResourceType, linkType, editedResource);
            if (!resources.isEmpty()) {
                if (resources.size() > 1) {
                    throw new ProblemException("Too many links of type [" + linkType + "]");
                }
                R resource = resources.get(0);
                ((ResourceFieldPageItem<R>) ctx.getPageItem()).setValue(resource);
            }
        });
        fieldConfig.setPopulateResource(ctx -> {
            String value = ctx.getTextValue();
            if (value == null) {
                // Remove previous links
                if (editedResource.getInternalId() != null) {
                    List<R> currentLinks = servicesCtx.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(fromResourceType, linkType, editedResource);
                    currentLinks.stream() //
                            .forEach(it -> {
                                ctx.getChangesContext().linkDelete(editedResource, linkType, it);
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
                        servicesCtx.getResourceService().createResourceQuery(fromResourceType) //
                                .addIdEquals(linkedResourceId) //
                );
                if (!linkedResourceOptional.isPresent()) {
                    throw new ProblemException("The linked resource does not exist");
                }

                IPResource finalLink = linkedResourceOptional.get();

                // Remove previous links if not the right one
                List<R> currentLinks;
                if (editedResource.getInternalId() == null) {
                    currentLinks = new ArrayList<>();
                } else {
                    currentLinks = servicesCtx.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(fromResourceType, linkType, editedResource);
                    currentLinks.stream() //
                            .filter(it -> !finalLink.equals(it)) //
                            .forEach(it -> {
                                ctx.getChangesContext().linkDelete(editedResource, linkType, it);
                            });
                }

                // Add the new links if not the right ones or there were none
                if (!currentLinks.contains(finalLink)) {
                    ctx.getChangesContext().linkAdd(editedResource, linkType, finalLink);
                }

            }

        });
        fieldConfigConsumer.accept(fieldConfig);
        fieldConfigs.add(fieldConfig);
    }

    public <R extends IPResource> void addReverseResource(String propertyName, Class<R> resourceType, String linkType) {
        addReverseResource(propertyName, resourceType, linkType, (fc) -> {
        });
    }

    @SuppressWarnings("unchecked")
    public <R extends IPResource> void addReverseResources(String propertyName, Class<R> fromResourceType, String linkType, Consumer<SimpleResourceEditorDefinitionFieldConfig> fieldConfigConsumer) {
        SimpleResourceEditorDefinitionFieldConfig fieldConfig = new SimpleResourceEditorDefinitionFieldConfig();
        fieldConfig.setPropertyName(propertyName);
        fieldConfig.setGenAndAddPageItem((pageDefinition) -> {
            ResourcesFieldPageItem<R> pageItem = new ResourcesFieldPageItem<>();
            pageItem.setFieldName(propertyName);
            pageItem.setLabel(servicesCtx.getTranslationService().translate(prefix + "." + propertyName));
            pageItem.setResourceType(fromResourceType);
            pageDefinition.addPageItem(pageItem);
            return pageItem;
        });
        fieldConfig.setPopulatePageItem(ctx -> {
            List<R> resources = servicesCtx.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(fromResourceType, linkType, editedResource);
            if (!resources.isEmpty()) {
                ((ResourcesFieldPageItem<R>) ctx.getPageItem()).setValues(resources);
            }
        });
        fieldConfig.setPopulateResource(ctx -> {
            String values = ctx.getTextValue();
            if (values == null) {
                // Remove previous links
                if (editedResource.getInternalId() != null) {
                    List<R> currentLinks = servicesCtx.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(fromResourceType, linkType, editedResource);
                    currentLinks.stream() //
                            .forEach(it -> {
                                ctx.getChangesContext().linkDelete(editedResource, linkType, it);
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
                        servicesCtx.getResourceService().createResourceQuery(fromResourceType) //
                                .addIdEquals(linkedResourceIds) //
                );

                // Remove previous links if not the right one
                List<R> currentLinks;
                if (editedResource.getInternalId() == null) {
                    currentLinks = new ArrayList<>();
                } else {
                    currentLinks = servicesCtx.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(fromResourceType, linkType, editedResource);
                    currentLinks.stream() //
                            .filter(it -> !finalLinks.contains(it)) //
                            .forEach(it -> {
                                ctx.getChangesContext().linkDelete(editedResource, linkType, it);
                            });
                }

                // Add the new links if not the right ones or there were none
                finalLinks.stream() //
                        .filter(it -> !currentLinks.contains(it)) //
                        .forEach(it -> {
                            ctx.getChangesContext().linkAdd(editedResource, linkType, it);
                        });
            }

        });
        fieldConfigConsumer.accept(fieldConfig);
        fieldConfigs.add(fieldConfig);
    }

    public <R extends IPResource> void addReverseResources(String propertyName, Class<R> resourceType, String linkType) {
        addReverseResources(propertyName, resourceType, linkType, (fc) -> {
        });
    }

    public void addSelectOptionsField(String propertyName, List<String> validValues) {
        addSelectOptionsField(propertyName, validValues, (fc) -> {
        });
    }

    public void addSelectOptionsField(String propertyName, List<String> validValues, Consumer<SimpleResourceEditorDefinitionFieldConfig> fieldConfigConsumer) {

        SimpleResourceEditorDefinitionFieldConfig fieldConfig = new SimpleResourceEditorDefinitionFieldConfig();
        fieldConfig.setPropertyName(propertyName);
        fieldConfig.setGenAndAddPageItem((pageDefinition) -> CommonPageItem.createSelectOptionsField(servicesCtx, pageDefinition, prefix + "." + propertyName, propertyName, validValues));
        fieldConfig.addValidator((fieldName, fieldValue) -> CommonValidation.validateInList(fieldName, fieldValue, validValues));
        fieldConfigConsumer.accept(fieldConfig);
        fieldConfigs.add(fieldConfig);
    }

    public void fill(ChangesContext changeContext, Map<String, String> validFormValues) {
        fieldConfigs.forEach(fieldConfig -> {
            String propertyName = fieldConfig.getPropertyName();
            String textValue = validFormValues.get(propertyName);
            Set<String> textValues = CommonFieldHelper.fromFormListToSet(validFormValues, propertyName);
            fieldConfig.getPopulateResource().accept(new PopulateResourceCtx(changeContext, beanWrapper, editedResource, propertyName, textValue, textValues));
        });

    }

    public void format(Map<String, String> rawFormValues) {
        for (SimpleResourceEditorDefinitionFieldConfig fieldConfig : fieldConfigs) {
            for (String propertyName : CommonFieldHelper.getAllFieldNames(rawFormValues, fieldConfig.getPropertyName())) {
                String propertyValue = rawFormValues.get(propertyName);
                for (Function<String, String> formator : fieldConfig.getFormators()) {
                    propertyValue = formator.apply(propertyValue);
                }
                rawFormValues.put(propertyName, propertyValue);
            }

        }
    }

    public PageDefinition getPageDefinition() {
        PageDefinition pageDefinition = new PageDefinition(servicesCtx.getTranslationService().translate(prefix + ".title"));

        fieldConfigs.forEach(fieldConfig -> {
            AbstractFieldPageItem pageItem = fieldConfig.getGenAndAddPageItem().apply(pageDefinition);
            fieldConfig.getAugmentPageItem().accept(pageItem);

            if (beanWrapper != null) {
                try {
                    Object propertyValue = beanWrapper.getPropertyValue(fieldConfig.getPropertyName());
                    if (propertyValue != null) {
                        fieldConfig.getPopulatePageItem().accept(new PopulatePageItemCtx(pageItem, editedResource, propertyValue));
                    }
                } catch (InvalidPropertyException e) {
                    fieldConfig.getPopulatePageItem().accept(new PopulatePageItemCtx(pageItem, editedResource, null));
                }
            }
        });

        return pageDefinition;
    }

    public List<Tuple2<String, String>> validate(Map<String, String> rawFormValues) {
        List<Tuple2<String, String>> errors = new ArrayList<>();
        for (SimpleResourceEditorDefinitionFieldConfig fieldConfig : fieldConfigs) {
            for (BiFunction<String, String, List<Tuple2<String, String>>> validator : fieldConfig.getValidators()) {
                for (String propertyName : CommonFieldHelper.getAllFieldNames(rawFormValues, fieldConfig.getPropertyName())) {
                    errors.addAll(validator.apply(propertyName, rawFormValues.get(propertyName)));
                }
            }
        }
        return errors;
    }

}
