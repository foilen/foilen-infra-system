/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.exception.SmallToolsException;
import com.foilen.smalltools.tools.AssertTools;

/**
 * Query resources.
 *
 *
 * Remarks:
 * <ul>
 * <li>can equal once per property</li>
 * <li>for tags, can equal/like "AND" or "OR", but not both at the same time</li>
 * </ul>
 *
 * @param <T>
 *            resource type
 */
public class IPResourceQuery<T extends IPResource> {

    private static final Map<String, Class<?>> CLASS_BY_PREMITIVE = new HashMap<>();

    private static final Set<Class<?>> SUPPORTS_EQUALS = new HashSet<>();
    private static final Set<Class<?>> SUPPORTS_LIKE = new HashSet<>();
    private static final Set<Class<?>> SUPPORTS_RANGE = new HashSet<>();

    static {
        CLASS_BY_PREMITIVE.put("boolean", Boolean.class);
        CLASS_BY_PREMITIVE.put("char", Character.class);
        CLASS_BY_PREMITIVE.put("double", Double.class);
        CLASS_BY_PREMITIVE.put("float", Float.class);
        CLASS_BY_PREMITIVE.put("int", Integer.class);
        CLASS_BY_PREMITIVE.put("long", Long.class);

        SUPPORTS_EQUALS.add(Boolean.class);
        SUPPORTS_EQUALS.add(Character.class);
        SUPPORTS_EQUALS.add(Date.class);
        SUPPORTS_EQUALS.add(Double.class);
        SUPPORTS_EQUALS.add(Enum.class);
        SUPPORTS_EQUALS.add(Float.class);
        SUPPORTS_EQUALS.add(Integer.class);
        SUPPORTS_EQUALS.add(Long.class);
        SUPPORTS_EQUALS.add(Set.class);
        SUPPORTS_EQUALS.add(String.class);

        SUPPORTS_LIKE.add(Set.class);
        SUPPORTS_LIKE.add(String.class);

        SUPPORTS_RANGE.add(Character.class);
        SUPPORTS_RANGE.add(Date.class);
        SUPPORTS_RANGE.add(Double.class);
        SUPPORTS_RANGE.add(Enum.class);
        SUPPORTS_RANGE.add(Float.class);
        SUPPORTS_RANGE.add(Integer.class);
        SUPPORTS_RANGE.add(Long.class);

    }

    // On ID
    private List<Long> idsIn;

    private IPResourceDefinition resourceDefinition;

    // On properties
    private Map<String, Object> propertyEquals = new HashMap<>();
    private Map<String, String> propertyLike = new HashMap<>();
    private Map<String, Object> propertyLesserAndEquals = new HashMap<>();
    private Map<String, Object> propertyLesser = new HashMap<>();
    private Map<String, Object> propertyGreaterAndEquals = new HashMap<>();
    private Map<String, Object> propertyGreater = new HashMap<>();

    // On editor name
    private List<String> editorsIn;

    // On tags
    private Set<String> tagsAnd = new HashSet<>();

    private Set<String> tagsOr = new HashSet<>();

    public IPResourceQuery(IPResourceDefinition resourceDefinition) {
        this.resourceDefinition = resourceDefinition;
    }

    public IPResourceQuery<T> addEditorEquals(String... values) {
        if (editorsIn == null) {
            editorsIn = new ArrayList<>();
        }
        for (String value : values) {
            editorsIn.add(value);
        }
        return this;
    }

    public IPResourceQuery<T> addIdEquals(long... values) {
        if (idsIn == null) {
            idsIn = new ArrayList<>();
        }
        for (long value : values) {
            idsIn.add(value);
        }
        return this;
    }

    private void assertProperty(String propertyName, Object value, Set<Class<?>> validTypes, String invalidTypeReason) {
        if (!resourceDefinition.hasProperty(propertyName)) {
            throw new SmallToolsException("Property [" + propertyName + "] does not exists");
        }

        // Check if supported type for the query
        Class<?> expectedValueType = getNonPremitiveType(resourceDefinition.getPropertyType(propertyName));
        if (expectedValueType.isEnum()) {
            if (!validTypes.contains(Enum.class)) {
                throw new SmallToolsException("Property [" + propertyName + "] does not support querying " + invalidTypeReason);
            }
        } else if (!validTypes.contains(expectedValueType)) {
            throw new SmallToolsException("Property [" + propertyName + "] does not support querying " + invalidTypeReason);
        }

        // Check if the value is of the right type
        if (value != null) {
            Class<?> valueType = getNonPremitiveType(value.getClass());
            if ((!expectedValueType.isAssignableFrom(valueType)) //
                    && !(expectedValueType.equals(Set.class) && valueType.equals(String.class))) {
                throw new SmallToolsException("Expected type for property [" + propertyName + "] is [" + expectedValueType.getName() + "], but got [" + valueType.getName() + "]");
            }
        }
    }

    private void assertPropertyNotUsed(String propertyName) {
        if (propertyEquals.containsKey(propertyName)) {
            throw new SmallToolsException("Property [" + propertyName + "] already has a value to check for equals");
        }
    }

    public List<String> getEditorsIn() {
        return editorsIn;
    }

    public List<Long> getIdsIn() {
        return idsIn;
    }

    private Class<?> getNonPremitiveType(Class<?> type) {
        Class<?> prim = CLASS_BY_PREMITIVE.get(type.getName());
        if (prim != null) {
            return prim;
        }

        return type;
    }

    public Map<String, Object> getPropertyEquals() {
        return Collections.unmodifiableMap(propertyEquals);
    }

    public Map<String, Object> getPropertyGreater() {
        return Collections.unmodifiableMap(propertyGreater);
    }

    public Map<String, Object> getPropertyGreaterEquals() {
        return Collections.unmodifiableMap(propertyGreaterAndEquals);
    }

    public Map<String, Object> getPropertyLesser() {
        return Collections.unmodifiableMap(propertyLesser);
    }

    public Map<String, Object> getPropertyLesserAndEquals() {
        return Collections.unmodifiableMap(propertyLesserAndEquals);
    }

    public Map<String, String> getPropertyLike() {
        return Collections.unmodifiableMap(propertyLike);
    }

    public IPResourceDefinition getResourceDefinition() {
        return resourceDefinition;
    }

    public Set<String> getTagsAnd() {
        return Collections.unmodifiableSet(tagsAnd);
    }

    public Set<String> getTagsOr() {
        return Collections.unmodifiableSet(tagsOr);
    }

    /**
     * Does {@link #propertyEquals(String, Object)} on all the properties for the primary key. It supports values being nulls.
     *
     * @param resource
     *            the resource with the properties of the primary key set on it
     * @return this
     */
    public IPResourceQuery<T> primaryKeyEquals(T resource) {
        for (String propertyName : resourceDefinition.getPrimaryKeyProperties()) {
            try {
                propertyEquals(propertyName, resourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource));
            } catch (Exception e) {
                throw new SmallToolsException("Could not retrieve the property value", e);
            }
        }

        return this;
    }

    public IPResourceQuery<T> propertyEquals(String propertyName, Object value) {
        assertProperty(propertyName, value, SUPPORTS_EQUALS, "equal");
        assertPropertyNotUsed(propertyName);
        propertyEquals.put(propertyName, value);
        return this;
    }

    public IPResourceQuery<T> propertyGreater(String propertyName, Object value) {
        assertProperty(propertyName, value, SUPPORTS_RANGE, "greater");
        assertPropertyNotUsed(propertyName);
        propertyGreater.put(propertyName, value);
        return this;
    }

    public IPResourceQuery<T> propertyGreaterAndEquals(String propertyName, Object value) {
        assertProperty(propertyName, value, SUPPORTS_RANGE, "greater or equal");
        assertPropertyNotUsed(propertyName);
        propertyGreaterAndEquals.put(propertyName, value);
        return this;
    }

    public IPResourceQuery<T> propertyLesser(String propertyName, Object value) {
        assertProperty(propertyName, value, SUPPORTS_RANGE, "lesser");
        assertPropertyNotUsed(propertyName);
        propertyLesser.put(propertyName, value);
        return this;
    }

    public IPResourceQuery<T> propertyLesserAndEquals(String propertyName, Object value) {
        assertProperty(propertyName, value, SUPPORTS_RANGE, "lesser or equal");
        assertPropertyNotUsed(propertyName);
        propertyLesserAndEquals.put(propertyName, value);
        return this;
    }

    public IPResourceQuery<T> propertyLike(String propertyName, String value) {
        assertProperty(propertyName, value, SUPPORTS_LIKE, "like");
        assertPropertyNotUsed(propertyName);
        propertyLike.put(propertyName, value);
        return this;
    }

    public void setIdsIn(List<Long> idsIn) {
        this.idsIn = idsIn;
    }

    /**
     * Add tags to check and they must all be present on the item (AND).
     *
     * @param tags
     *            the exact names or like with "%" wildcard
     * @return this
     */
    public IPResourceQuery<T> tagAddAnd(String... tags) {
        tagsAnd.addAll(Arrays.asList(tags));
        AssertTools.assertTrue(tagsOr.isEmpty(), "There can be only tags check as AND or OR, but not both at the same time");
        return this;
    }

    /**
     * Add tags to check and at least one must be present on the item (OR).
     *
     * @param tags
     *            the exact names or like with "%" wildcard
     * @return this
     */
    public IPResourceQuery<T> tagAddOr(String... tags) {
        tagsOr.addAll(Arrays.asList(tags));
        AssertTools.assertTrue(tagsAnd.isEmpty(), "There can be only tags check as AND or OR, but not both at the same time");
        return this;
    }

}
