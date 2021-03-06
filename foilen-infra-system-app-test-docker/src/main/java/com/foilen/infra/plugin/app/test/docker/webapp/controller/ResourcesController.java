/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.app.test.docker.webapp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.foilen.infra.plugin.app.test.docker.webapp.controller.response.ResourceSuggestResponse;
import com.foilen.infra.plugin.app.test.docker.webapp.controller.response.ResourceUpdateResponse;
import com.foilen.infra.plugin.app.test.docker.webapp.mvc.Authentication;
import com.foilen.infra.plugin.app.test.docker.webapp.mvc.UiSuccessErrorView;
import com.foilen.infra.plugin.core.system.memory.service.ResourceServicesInMemoryImpl;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
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
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.google.common.base.Strings;

@Controller
@RequestMapping("resource")
public class ResourcesController extends ResourcesControllerExtra {

    public static final String RESOURCE_ID_FIELD = "_resourceId";
    public static final String VIEW_BASE_PATH = "resource";

    @Autowired
    private CommonServicesContext commonServicesContext;
    @Autowired
    private ResourceServicesInMemoryImpl resourceService;
    @Autowired
    private InternalChangeService internalChangeService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private IPPluginService ipPluginService;
    @Autowired
    private SecurityService securityService;

    @GetMapping("create/{editorName}")
    public ModelAndView create(@PathVariable("editorName") String editorName) {
        ModelAndView modelAndView = new ModelAndView(VIEW_BASE_PATH + "/create");

        Optional<ResourceEditor<?>> editor = ipPluginService.getResourceEditorByName(editorName);

        if (editor.isPresent()) {
            modelAndView.addObject("editorName", editorName);
            modelAndView.addObject("resourceType", editor.get().getForResourceType().getName());
        }

        return modelAndView;
    }

    @GetMapping("createPageDefinition/{editorName}")
    public ModelAndView createPageDefinition(@PathVariable("editorName") String editorName, HttpServletRequest httpServletRequest) {
        ModelAndView modelAndView = new ModelAndView(VIEW_BASE_PATH + "/resource");

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

    @GetMapping("createPageDefinitionByType/{resourceType:.*}")
    public ModelAndView createPageDefinitionByType(@PathVariable("resourceType") String resourceType, HttpServletRequest httpServletRequest) {
        ModelAndView modelAndView = new ModelAndView(VIEW_BASE_PATH + "/resource");

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

    @PostMapping("delete")
    public ModelAndView delete(Authentication authentication, @RequestParam("resourceId") String resourceId, RedirectAttributes redirectAttributes) {
        return new UiSuccessErrorView(redirectAttributes) //
                .setSuccessViewName("redirect:/" + VIEW_BASE_PATH + "/list") //
                .setErrorViewName("redirect:/" + VIEW_BASE_PATH + "/list") //
                .execute((ui, modelAndView) -> {
                    ChangesContext changes = new ChangesContext(resourceService);
                    changes.resourceDelete(resourceId);
                    internalChangeService.changesExecute(changes);
                });
    }

    @GetMapping("edit/{resourceId}")
    public ModelAndView edit(Authentication authentication, @PathVariable("resourceId") String resourceId) {

        ModelAndView modelAndView = new ModelAndView(VIEW_BASE_PATH + "/edit");

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

            modelAndView.addObject("resourceName", resource.getResourceName());

            modelAndView.addObject("editorNames", editorNames);
        }

        return modelAndView;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @GetMapping("editPageDefinition/{editorName}/{resourceId}")
    public ModelAndView editPageDefinition(Authentication authentication, @PathVariable("editorName") String editorName, @PathVariable("resourceId") String resourceId,
            HttpServletRequest httpServletRequest) {
        ModelAndView modelAndView = new ModelAndView(VIEW_BASE_PATH + "/resource");

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

                // _csrf
                HiddenFieldPageItem csrfField = new HiddenFieldPageItem();
                csrfField.setFieldName(securityService.getCsrfParameterName());
                csrfField.setFieldValue(securityService.getCsrfValue(httpServletRequest));
                pageDefinition.addPageItem(csrfField);

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

    @GetMapping("list")
    public ModelAndView list(Authentication authentication) {

        ModelAndView modelAndView = new ModelAndView(VIEW_BASE_PATH + "/list");

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
    @GetMapping("suggest/{resourceType:.+}")
    public List<ResourceSuggestResponse> suggest(Authentication authentication, @PathVariable("resourceType") Class<? extends IPResource> resourceType) {

        return resourceService.resourceFindAll( //
                resourceService.createResourceQuery(resourceType) //
        ).stream() //
                .map(it -> new ResourceSuggestResponse(it.getInternalId(), it.getResourceName(), it.getResourceDescription())) //
                .sorted((a, b) -> a.getName().compareTo(b.getName())) //
                .collect(Collectors.toList());
    }

    @ResponseBody
    @GetMapping("suggestEditor/{resourceType:.+}")
    public List<String> suggestEditor(@PathVariable("resourceType") Class<? extends IPResource> resourceType) {
        return ipPluginService.getResourceEditorNamesByResourceType(resourceType);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @ResponseBody
    @PostMapping("update")
    public ResourceUpdateResponse update(Authentication authentication, @RequestParam Map<String, String> formValues, Locale locale) {

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
            String resourceId = formValues.get(RESOURCE_ID_FIELD);
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
                    resource = (IPResource) resourceType.getConstructor().newInstance();
                } catch (Exception e) {
                    logger.error("Could not create an empty resource of type {}", resourceType, e);
                    resourceUpdateResponse.setTopError(messageSource.getMessage("error.internalError", new Object[] { e.getMessage() }, locale));
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
            String internalId;
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
                    resourceUpdateResponse.setTopError(messageSource.getMessage("error.internalError", new Object[] { e.getMessage() }, locale));
                    return resourceUpdateResponse;
                }

                internalId = newResource.getInternalId();

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
                    resourceUpdateResponse.setTopError(messageSource.getMessage("error.internalError", new Object[] { e.getMessage() }, locale));
                    return resourceUpdateResponse;
                }

                internalId = resource.getInternalId();

            }

            // Redirect url
            resourceUpdateResponse.setSuccessResource(resource);
            resourceUpdateResponse.setSuccessResourceId(internalId);

        } else {
            resourceUpdateResponse.setTopError(messageSource.getMessage("error.editorNotFound", null, locale));
        }

        return resourceUpdateResponse;
    }

}
