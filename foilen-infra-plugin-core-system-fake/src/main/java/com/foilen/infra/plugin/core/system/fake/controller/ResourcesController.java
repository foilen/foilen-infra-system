/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.foilen.infra.plugin.core.system.fake.controller.response.ResourceSuggestResponse;
import com.foilen.infra.plugin.core.system.fake.controller.response.ResourceUpdateResponse;
import com.foilen.infra.plugin.core.system.fake.service.FakeSystemServicesImpl;
import com.foilen.infra.plugin.core.system.junits.JunitsHelper;
import com.foilen.infra.plugin.core.system.junits.ResourcesDump;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.exception.ResourcePrimaryKeyCollisionException;
import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.core.service.IPPluginService;
import com.foilen.infra.plugin.v1.core.service.SecurityService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.plugin.v1.core.visual.PageDefinition;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.HiddenFieldPageItem;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.reflection.ReflectionTools;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.DirectoryTools;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;

@Controller
@RequestMapping("resource")
public class ResourcesController extends AbstractBasics {

    public static final String RESOURCE_ID_FIELD = "_resourceId";

    @Autowired
    private CommonServicesContext commonServicesContext;
    @Autowired
    private ConversionService conversionService;
    @Autowired
    private FakeSystemServicesImpl resourceService;
    @Autowired
    private InternalChangeService internalChangeService;;
    @Autowired
    private InternalServicesContext internalServicesContext;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private IPPluginService ipPluginService;
    @Autowired
    private SecurityService securityService;

    @RequestMapping("create/{editorName}")
    public ModelAndView create(@PathVariable("editorName") String editorName) {
        ModelAndView modelAndView = new ModelAndView("resource/create");

        Optional<ResourceEditor<?>> editor = ipPluginService.getResourceEditorByName(editorName);

        if (editor.isPresent()) {
            modelAndView.addObject("editorName", editorName);
            modelAndView.addObject("resourceType", editor.get().getForResourceType().getName());
        }

        return modelAndView;
    }

    @RequestMapping("createPageDefinition/{editorName}")
    public ModelAndView createPageDefinition(@PathVariable("editorName") String editorName, HttpServletRequest httpServletRequest) {
        ModelAndView modelAndView = new ModelAndView("resource/resource");

        Optional<ResourceEditor<?>> editor = ipPluginService.getResourceEditorByName(editorName);

        if (editor.isPresent()) {
            PageDefinition pageDefinition = editor.get().providePageDefinition(commonServicesContext, null);

            // _editorName
            HiddenFieldPageItem editorNameField = new HiddenFieldPageItem();
            editorNameField.setFieldName("_editorName");
            editorNameField.setFieldValue(editorName);
            pageDefinition.addPageItem(editorNameField);

            // _csrf
            HiddenFieldPageItem csrfField = new HiddenFieldPageItem();
            csrfField.setFieldName(securityService.getCsrfParameterName());
            csrfField.setFieldValue(securityService.getCsrfValue(httpServletRequest));
            pageDefinition.addPageItem(csrfField);

            modelAndView.addObject("pageDefinition", pageDefinition);
        } else {
            modelAndView.setViewName("error/single-partial");
            modelAndView.addObject("error", "error.editorNotFound");
        }

        return modelAndView;
    }

    @RequestMapping("createPageDefinitionByType/{resourceType}")
    public ModelAndView createPageDefinitionByType(@PathVariable("resourceType") String resourceType, HttpServletRequest httpServletRequest) {
        ModelAndView modelAndView = new ModelAndView("resource/resource");

        Class<?> resourceClass = ReflectionTools.safelyGetClass(resourceType);
        if (resourceClass == null) {
            modelAndView.setViewName("error/single-partial");
            modelAndView.addObject("error", "error.editorNotFound");
        }
        @SuppressWarnings("unchecked")
        List<String> editors = ipPluginService.getResourceEditorNamesByResourceType((Class<? extends IPResource>) resourceClass);

        if (editors.isEmpty()) {
            modelAndView.setViewName("error/single-partial");
            modelAndView.addObject("error", "error.editorNotFound");
        } else {
            return createPageDefinition(editors.get(0), httpServletRequest);
        }

        return modelAndView;
    }

    @RequestMapping("edit/{resourceId}")
    public ModelAndView edit(@PathVariable("resourceId") long resourceId) {

        ModelAndView modelAndView = new ModelAndView("resource/edit");

        Optional<IPResource> resourceOptional = resourceService.resourceFind(resourceId);
        if (resourceOptional.isPresent()) {

            IPResource resource = resourceOptional.get();

            String editorName = resource.getResourceEditorName();
            List<String> editorNames = ipPluginService.getResourceEditorNamesByResourceType(resource.getClass());
            if (editorName == null) {
                if (!editorNames.isEmpty()) {
                    editorName = editorNames.get(0);
                }
            }

            modelAndView.addObject("editorName", editorName);
            modelAndView.addObject("resourceId", resourceId);
            modelAndView.addObject("resourceType", resource.getClass().getName());

            modelAndView.addObject("editorNames", editorNames);
        }

        return modelAndView;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @RequestMapping("editPageDefinition/{editorName}/{resourceId}")
    public ModelAndView editPageDefinition(@PathVariable("editorName") String editorName, @PathVariable("resourceId") long resourceId) {
        ModelAndView modelAndView = new ModelAndView("resource/resource");

        Optional editorOptional = ipPluginService.getResourceEditorByName(editorName);

        if (editorOptional.isPresent()) {

            ResourceEditor editor = (ResourceEditor) editorOptional.get();

            Optional editedResourceOptional = resourceService.resourceFind(resourceId);

            if (editedResourceOptional.isPresent()) {

                IPResource editedResource = (IPResource) editedResourceOptional.get();

                PageDefinition pageDefinition = editor.providePageDefinition(commonServicesContext, editedResource);

                // _editorName
                HiddenFieldPageItem editorNameField = new HiddenFieldPageItem();
                editorNameField.setFieldName("_editorName");
                editorNameField.setFieldValue(editorName);
                pageDefinition.addPageItem(editorNameField);

                // _resourceId
                HiddenFieldPageItem resourceIdField = new HiddenFieldPageItem();
                resourceIdField.setFieldName(RESOURCE_ID_FIELD);
                resourceIdField.setFieldValue(String.valueOf(editedResource.getInternalId()));
                pageDefinition.addPageItem(resourceIdField);

                modelAndView.addObject("pageDefinition", pageDefinition);
            } else {
                modelAndView.setViewName("error/single-partial");
                modelAndView.addObject("error", "error.resourceNotFound");
            }
        } else {
            modelAndView.setViewName("error/single-partial");
            modelAndView.addObject("error", "error.editorNotFound");
        }

        return modelAndView;
    }

    @ResponseBody
    @RequestMapping("exportFile")
    public ResourcesDump exportFile() {
        return JunitsHelper.dumpExport(commonServicesContext, internalServicesContext);
    }

    @RequestMapping("exportFolder")
    public String exportFolder(@RequestParam String folder, HttpServletRequest httpServletRequest) {
        File exportFolder = new File(folder);
        String path = exportFolder.getAbsolutePath();
        if (exportFolder.exists()) {
            logger.error("The folder {} already exist", exportFolder.getAbsolutePath());
            return "redirect:list";
        }

        logger.info("Exporting in the folder {}", exportFolder.getAbsolutePath());
        if (!DirectoryTools.createPath(exportFolder)) {
            logger.error("Could not create the folder {}", exportFolder.getAbsolutePath());
            return "redirect:list";
        }

        // Export
        List<String> links = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        for (IPResource resource : resourceService.resourceFindAll()) {
            // Export resources
            String resourceType = resourceService.getResourceDefinition(resource).getResourceType();
            String resourceFilename = path + "/" + resourceType + "/" + resource.getResourceName().replaceAll("/", "_") + ".json";
            DirectoryTools.createPathToFile(resourceFilename);
            JsonTools.writeToFile(resourceFilename, resource);

            // Export tags
            resourceService.tagFindAllByResource(resource).stream() //
                    .map(it -> {
                        return resourceType + "/" + resource.getResourceName() + //
                        ";" + it;
                    }) //
                    .forEach(it -> tags.add(it));

            // Export links
            resourceService.linkFindAllByFromResource(resource).stream() //
                    .map(it -> {
                        String toResourceType = resourceService.getResourceDefinition(it.getB()).getResourceType();
                        return resourceType + "/" + resource.getResourceName() + //
                        ";" + it.getA() + ";" //
                                + toResourceType + "/" + it.getB().getResourceName();
                    }) //
                    .forEach(it -> links.add(it));

        }

        // Save tags and links
        Collections.sort(links);
        Collections.sort(tags);
        FileTools.writeFile(Joiner.on('\n').join(tags), path + "/tags.txt");
        FileTools.writeFile(Joiner.on('\n').join(links), path + "/links.txt");

        return "redirect:list";
    }

    @RequestMapping("list")
    public ModelAndView list() {
        ModelAndView modelAndView = new ModelAndView("resource/list");

        List<IPResourceDefinition> resourceDefinitions = resourceService.getResourceDefinitions();
        Map<String, List<?>> resourcesByType = new HashMap<>();
        for (IPResourceDefinition resourceDefinition : resourceDefinitions) {
            String resourceType = resourceDefinition.getResourceType();
            List<?> resources = resourceService.resourceFindAll(resourceService.createResourceQuery(resourceType));
            resourcesByType.put(resourceType, resources);
        }

        modelAndView.addObject("resourcesByType", resourcesByType);
        return modelAndView;
    }

    @ResponseBody
    @RequestMapping("suggest/{resourceType}")
    public List<ResourceSuggestResponse> suggest(@PathVariable("resourceType") Class<? extends IPResource> resourceType) {
        return resourceService.resourceFindAll( //
                resourceService.createResourceQuery(resourceType) //
        ).stream() //
                .map(it -> new ResourceSuggestResponse(it.getInternalId(), it.getResourceName(), it.getResourceDescription())) //
                .sorted((a, b) -> a.getName().compareTo(b.getName())) //
                .collect(Collectors.toList());
    }

    @ResponseBody
    @RequestMapping("suggestEditor/{resourceType}")
    public List<String> suggestEditor(@PathVariable("resourceType") Class<? extends IPResource> resourceType) {
        return ipPluginService.getResourceEditorNamesByResourceType(resourceType);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @ResponseBody
    @RequestMapping(value = "update", method = RequestMethod.POST)
    public ResourceUpdateResponse update(@RequestParam Map<String, String> formValues, Locale locale) {

        ResourceUpdateResponse resourceUpdateResponse = new ResourceUpdateResponse();

        // Get the editor
        String editorName = formValues.get("_editorName");
        if (Strings.isNullOrEmpty(editorName)) {
            resourceUpdateResponse.setTopError(messageSource.getMessage("error.editorNotSpecified", null, locale));
            return resourceUpdateResponse;
        }
        Optional<ResourceEditor<?>> resourceEditorOptional = ipPluginService.getResourceEditorByName(editorName);
        if (resourceEditorOptional.isPresent()) {
            ResourceEditor resourceEditor = resourceEditorOptional.get();

            // Get the resource if is editing
            Long resourceId = conversionService.convert(formValues.get(RESOURCE_ID_FIELD), Long.class);
            boolean isUpdate = resourceId != null;
            IPResource resource = null;
            if (resourceId != null) {
                Optional<IPResource> resourceOptional = resourceService.resourceFind(resourceId);
                if (!resourceOptional.isPresent()) {
                    resourceUpdateResponse.setTopError(messageSource.getMessage("error.resourceNotFound", null, locale));
                    return resourceUpdateResponse;
                }

                resource = resourceOptional.get();

                // Basic validation (resource id is of the supported type for the editor)
                if (!resourceEditor.getForResourceType().isAssignableFrom(resource.getClass())) {
                    resourceUpdateResponse.setTopError(messageSource.getMessage("error.editorCannotEditResource", null, locale));
                    return resourceUpdateResponse;
                }

            }

            // Create empty resource if new
            if (resource == null) {
                Class<?> resourceType = resourceEditor.getForResourceType();
                try {
                    resource = (IPResource) resourceType.newInstance();
                } catch (Exception e) {
                    logger.error("Could not create an empty resource of type {}", resourceType, e);
                    resourceUpdateResponse.setTopError(messageSource.getMessage("error.internalError", null, locale));
                    return resourceUpdateResponse;
                }
            }

            // Editor's formating
            resourceEditor.formatForm(commonServicesContext, formValues);
            resourceUpdateResponse.getFieldsValues().putAll(formValues);

            // Editor's validation
            List<Tuple2<String, String>> errors = resourceEditor.validateForm(commonServicesContext, formValues);

            // Errors: add to resourceUpdateResponse.getFieldsErrors()
            if (errors != null && !errors.isEmpty()) {
                for (Tuple2<String, String> error : errors) {
                    resourceUpdateResponse.getFieldsErrors().put(error.getA(), messageSource.getMessage(error.getB(), null, locale));
                }
                return resourceUpdateResponse;
            }

            // No errors: save and give the redirection link if no issues
            if (isUpdate) {

                // Update existing resource
                IPResource newResource = JsonTools.clone(resource);
                newResource.setInternalId(resource.getInternalId());

                try {
                    ChangesContext changesContext = new ChangesContext(resourceService);
                    resourceEditor.fillResource(commonServicesContext, changesContext, formValues, newResource);
                    newResource.setResourceEditorName(editorName);
                    changesContext.resourceUpdate(resourceId, newResource);
                    internalChangeService.changesExecute(changesContext);
                } catch (ResourcePrimaryKeyCollisionException e) {
                    logger.error("Problem saving the resource", e);
                    resourceUpdateResponse.setTopError(messageSource.getMessage("error.duplicateResource", null, locale));
                    return resourceUpdateResponse;
                } catch (Exception e) {
                    logger.error("Problem saving the resource", e);
                    resourceUpdateResponse.setTopError(messageSource.getMessage("error.internalError", null, locale));
                    return resourceUpdateResponse;
                }

                resource = newResource;

            } else {

                // Create a new resource

                try {
                    ChangesContext changesContext = new ChangesContext(resourceService);
                    resourceEditor.fillResource(commonServicesContext, changesContext, formValues, resource);
                    changesContext.resourceAdd(resource);
                    resource.setResourceEditorName(editorName);
                    internalChangeService.changesExecute(changesContext);
                    resource = resourceService.resourceFindByPk(resource).get();
                } catch (ResourcePrimaryKeyCollisionException e) {
                    logger.error("Problem saving the resource", e);
                    resourceUpdateResponse.setTopError(messageSource.getMessage("error.duplicateResource", null, locale));
                    return resourceUpdateResponse;
                } catch (Exception e) {
                    logger.error("Problem saving the resource", e);
                    resourceUpdateResponse.setTopError(messageSource.getMessage("error.internalError", null, locale));
                    return resourceUpdateResponse;
                }

            }

            // Redirect url
            resourceUpdateResponse.setSuccessResource(resource);
            resourceUpdateResponse.setSuccessResourceId(resource.getInternalId());

        } else {
            resourceUpdateResponse.setTopError(messageSource.getMessage("error.editorNotFound", null, locale));
        }

        return resourceUpdateResponse;
    }

}
