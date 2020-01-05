/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.mvc;

import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

public class UiSuccessErrorView {

    private RedirectAttributes redirectAttributes;

    private String successViewName;
    private String errorViewName;

    private Object[] viewVariables = new Object[] {};
    private Object form;
    private Errors formErrors;

    public UiSuccessErrorView(RedirectAttributes redirectAttributes) {
        this.redirectAttributes = redirectAttributes;
    }

    public UiSuccessErrorView(RedirectAttributes redirectAttributes, Object form) {
        this.redirectAttributes = redirectAttributes;
        this.form = form;
    }

    public ModelAndView execute(UiErrorRedirectHandler handler) {
        Assert.notNull(handler, "handler cannot be null");
        Assert.notNull(redirectAttributes, "redirectAttributes cannot be null");
        Assert.notNull(successViewName, "successViewName cannot be null");
        Assert.notNull(errorViewName, "errorRedirection cannot be null");

        String viewName = successViewName;
        ModelAndView modelAndView = new ModelAndView();

        try {
            handler.execute(this, modelAndView);
        } catch (UiException e) {

            viewName = errorViewName;

            // Check redirect or not
            boolean isRedirection = viewName.startsWith("redirect:");
            if (isRedirection) {
                // Add the error
                redirectAttributes.addFlashAttribute("errorCode", e.getMessage());
                redirectAttributes.addFlashAttribute("errorParams", e.getParams());

                // Add the form
                redirectAttributes.addFlashAttribute("form", form);
                redirectAttributes.addFlashAttribute("formErrors", formErrors);
            } else {
                // Add the error
                modelAndView.addObject("errorCode", e.getMessage());
                modelAndView.addObject("errorParams", e.getParams());

                // Add the form
                modelAndView.addObject("form", form);
                modelAndView.addObject("formErrors", formErrors);
            }

        }

        modelAndView.setViewName(UriComponentsBuilder.fromPath(viewName).buildAndExpand(viewVariables).toString());
        return modelAndView;
    }

    public Object getForm() {
        return form;
    }

    public Errors getFormErrors() {
        return formErrors;
    }

    public UiSuccessErrorView setErrorViewName(String errorViewName) {
        this.errorViewName = errorViewName;
        return this;
    }

    public void setForm(Object form) {
        this.form = form;
    }

    public void setFormErrors(Errors formErrors) {
        this.formErrors = formErrors;
    }

    public UiSuccessErrorView setSuccessViewName(String successViewName) {
        this.successViewName = successViewName;
        return this;
    }

    public UiSuccessErrorView setViewVariables(Object... viewVariables) {
        this.viewVariables = viewVariables;
        return this;
    }

}
