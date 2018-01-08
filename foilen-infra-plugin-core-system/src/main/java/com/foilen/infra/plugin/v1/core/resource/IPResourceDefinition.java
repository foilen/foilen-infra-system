/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.resource;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.reflection.ReflectionTools;
import com.foilen.smalltools.tools.AssertTools;

/**
 * To define a custom resource.
 */
public class IPResourceDefinition {

    private Class<? extends IPResource> resourceClass;
    private String resourceType;
    private Set<String> primaryKeyProperties = new HashSet<>();
    private Set<String> searchableProperties = new HashSet<>();

    // Cached getters
    private Map<String, Method> getterMethodByPropertyName = new HashMap<>();
    private Map<String, Class<?>> returnTypeByPropertyName = new HashMap<>();

    public IPResourceDefinition(Class<? extends IPResource> resourceClass, String resourceType, Collection<String> primaryKeyProperties, Collection<String> searchableProperties) {
        AssertTools.assertNotNull(resourceClass, "ResourceClass cannot be null");
        AssertTools.assertNotNull(resourceType, "ResourceType cannot be null");
        AssertTools.assertFalse(resourceType.isEmpty(), "ResourceType cannot be empty");
        this.resourceClass = resourceClass;
        this.resourceType = resourceType;
        this.primaryKeyProperties.addAll(primaryKeyProperties);
        this.searchableProperties.addAll(primaryKeyProperties);
        this.searchableProperties.addAll(searchableProperties);

        List<Method> methods = ReflectionTools.allMethods(resourceClass).stream() //
                .filter(it -> {
                    boolean getter = it.getParameterCount() == 0;
                    String name = it.getName();
                    if (getter) {
                        Character nextChar = null;
                        if (name.startsWith("get") && name.length() >= 4) {
                            nextChar = name.charAt(3);
                        } else if (name.startsWith("is") && name.length() >= 3) {
                            nextChar = name.charAt(2);
                        } else {
                            getter = false;
                        }

                        if (nextChar != null && !Character.isUpperCase(nextChar)) {
                            getter = false;
                        }
                    }
                    return getter;
                }) //
                .collect(Collectors.toList());

        for (Method method : methods) {

            // Remove get or is
            String name = method.getName();
            if (name.startsWith("get")) {
                name = name.substring(3);
            } else if (name.startsWith("is")) {
                name = name.substring(2);
            }

            // Lower case the first character
            if (name.length() == 1) {
                name = name.toLowerCase();
            } else {
                name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
            }

            getterMethodByPropertyName.put(name, method);
            returnTypeByPropertyName.put(name, method.getReturnType());
        }

        // Check all searchable fields are available
        for (String propertyName : searchableProperties) {
            AssertTools.assertTrue(getterMethodByPropertyName.containsKey(propertyName), "The property " + propertyName + " does not exists");
        }
    }

    public Set<String> getPrimaryKeyProperties() {
        return primaryKeyProperties;
    }

    public Method getPropertyGetterMethod(String propertyName) {
        return getterMethodByPropertyName.get(propertyName);
    }

    public Class<?> getPropertyType(String propertyName) {
        return returnTypeByPropertyName.get(propertyName);
    }

    public Class<? extends IPResource> getResourceClass() {
        return resourceClass;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Set<String> getSearchableProperties() {
        return searchableProperties;
    }

    public boolean hasProperty(String propertyName) {
        return getterMethodByPropertyName.containsKey(propertyName);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("IPResourceDefinition [resourceType=");
        builder.append(resourceType);
        builder.append(", resourceClass=");
        builder.append(resourceClass);
        builder.append("]");
        return builder.toString();
    }

}
