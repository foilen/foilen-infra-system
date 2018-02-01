/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.editor.simpleresourceditor;

import java.util.Set;

import org.springframework.beans.BeanWrapper;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.model.resource.IPResource;

public class PopulateResourceCtx {

    private ChangesContext changesContext;

    private BeanWrapper editedResourceBeanWrapper;
    private IPResource editedResource;

    private String propertyName;
    private String textValue;
    private Set<String> textValues;

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    public Set<String> getTextValues() {
        return textValues;
    }

    public void setTextValues(Set<String> textValues) {
        this.textValues = textValues;
    }

    public PopulateResourceCtx(ChangesContext changesContext, BeanWrapper editedResourceBeanWrapper, IPResource editedResource, String propertyName, String textValue, Set<String> textValues) {
        this.changesContext = changesContext;
        this.editedResourceBeanWrapper = editedResourceBeanWrapper;
        this.editedResource = editedResource;
        this.propertyName = propertyName;
        this.textValue = textValue;
        this.textValues = textValues;
    }

    public ChangesContext getChangesContext() {
        return changesContext;
    }

    public void setChangesContext(ChangesContext changesContext) {
        this.changesContext = changesContext;
    }

    public IPResource getEditedResource() {
        return editedResource;
    }

    public void setEditedResource(IPResource editedResource) {
        this.editedResource = editedResource;
    }

    public BeanWrapper getEditedResourceBeanWrapper() {
        return editedResourceBeanWrapper;
    }

    public void setEditedResourceBeanWrapper(BeanWrapper editedResourceBeanWrapper) {
        this.editedResourceBeanWrapper = editedResourceBeanWrapper;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

}
