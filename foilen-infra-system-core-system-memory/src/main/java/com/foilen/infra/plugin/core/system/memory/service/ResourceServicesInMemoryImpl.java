/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.memory.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.foilen.infra.plugin.core.system.common.changeexecution.ChangeExecutionLogic;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.changes.ChangeExecutionHook;
import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.core.resource.IPResourceQuery;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalChangeService;
import com.foilen.infra.plugin.v1.core.service.internal.InternalIPResourceService;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.exception.SmallToolsException;
import com.foilen.smalltools.reflection.ReflectionTools;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.AssertTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.StringTools;
import com.foilen.smalltools.tuple.Tuple2;
import com.foilen.smalltools.tuple.Tuple3;

@Component
public class ResourceServicesInMemoryImpl extends AbstractBasics implements IPResourceService, InternalIPResourceService, InternalChangeService {

    // Services
    @Autowired
    private CommonServicesContext commonServicesContext;
    @Autowired
    private InternalServicesContext internalServicesContext;

    // In memory data
    private AtomicLong nextInternalId = new AtomicLong(1);
    private List<IPResource> resources = new ArrayList<>();
    private List<Tuple3<String, String, String>> links = new ArrayList<>();
    private List<Tuple2<String, String>> tags = new ArrayList<>();

    private Map<Class<? extends IPResource>, List<Class<?>>> allClassesByResourceClass = new HashMap<>();
    private Map<Class<? extends IPResource>, IPResourceDefinition> resourceDefinitionByResourceClass = new HashMap<>();
    private Map<String, IPResourceDefinition> resourceDefinitionByResourceType = new HashMap<>();

    private long infiniteLoopTimeoutInMs = 15000;

    private List<ChangeExecutionHook> defaultChangeExecutionHooks = new ArrayList<>();

    public ResourceServicesInMemoryImpl() {
    }

    @Override
    public void changesExecute(ChangesContext changes) {

        changesExecute(changes, Collections.emptyList());

    }

    @Override
    public void changesExecute(ChangesContext changes, List<ChangeExecutionHook> extraChangeExecutionHooks) {

        ChangeExecutionLogic changeExecutionLogic = new ChangeExecutionLogic(commonServicesContext, internalServicesContext);
        changeExecutionLogic.setInfiniteLoopTimeoutInMs(infiniteLoopTimeoutInMs);
        defaultChangeExecutionHooks.forEach(hook -> changeExecutionLogic.addHook(hook));
        extraChangeExecutionHooks.forEach(hook -> changeExecutionLogic.addHook(hook));

        // Create a transaction
        List<IPResource> beforeTxResources = resources.stream().map(it -> it.deepClone()).collect(Collectors.toList());
        List<Tuple3<String, String, String>> beforeTxLinks = links.stream().map(it -> new Tuple3<>(it.getA(), it.getB(), it.getC())).collect(Collectors.toList());
        List<Tuple2<String, String>> beforeTxTags = tags.stream().map(it -> new Tuple2<>(it.getA(), it.getB())).collect(Collectors.toList());

        try {
            changeExecutionLogic.execute(changes);
        } catch (RuntimeException e) {
            // Rollback the transaction
            resources = beforeTxResources;
            links = beforeTxLinks;
            tags = beforeTxTags;
            throw e;
        }
    }

    private <R extends IPResource> R clone(R resource) {
        R clonedResource = JsonTools.clone(resource);
        clonedResource.setInternalId(resource.getInternalId());
        return clonedResource;
    }

    @Override
    public <T extends IPResource> IPResourceQuery<T> createResourceQuery(Class<T> resourceClass) {
        List<IPResourceDefinition> resourceDefinitions = allClassesByResourceClass.entrySet().stream() //
                .filter(it -> it.getValue().contains(resourceClass)) //
                .map(it -> resourceDefinitionByResourceClass.get(it.getKey())) //
                .filter(it -> it != null) //
                .collect(Collectors.toList());

        if (resourceDefinitions.isEmpty()) {
            throw new SmallToolsException("Resource class " + resourceClass.getName() + " is unknown");
        }

        return new IPResourceQuery<>(resourceDefinitions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IPResource> IPResourceQuery<T> createResourceQuery(String resourceType) {
        IPResourceDefinition resourceDefinition = resourceDefinitionByResourceType.get(resourceType);
        if (resourceDefinition == null) {
            throw new SmallToolsException("Resource type " + resourceType + " is unknown");
        }
        return (IPResourceQuery<T>) createResourceQuery(resourceDefinition.getResourceClass());
    }

    public CommonServicesContext getCommonServicesContext() {
        return commonServicesContext;
    }

    @Override
    public List<ChangeExecutionHook> getDefaultChangeExecutionHooks() {
        return defaultChangeExecutionHooks;
    }

    public long getInfiniteLoopTimeoutInMs() {
        return infiniteLoopTimeoutInMs;
    }

    public InternalServicesContext getInternalServicesContext() {
        return internalServicesContext;
    }

    public List<Tuple3<String, String, String>> getLinks() {
        return links;
    }

    @Override
    public IPResourceDefinition getResourceDefinition(Class<? extends IPResource> resourceClass) {
        return resourceDefinitionByResourceClass.get(resourceClass);
    }

    @Override
    public IPResourceDefinition getResourceDefinition(IPResource resource) {
        return resourceDefinitionByResourceClass.get(resource.getClass());
    }

    @Override
    public IPResourceDefinition getResourceDefinition(String resourceType) {
        return resourceDefinitionByResourceType.get(resourceType);
    }

    @Override
    public List<IPResourceDefinition> getResourceDefinitions() {
        return Collections.unmodifiableList(resourceDefinitionByResourceClass.values().stream().collect(Collectors.toList()));
    }

    public List<IPResource> getResources() {
        return resources;
    }

    public List<Tuple2<String, String>> getTags() {
        return tags;
    }

    @Override
    public void linkAdd(String fromResourceId, String linkType, String toResourceId) {
        links.add(new Tuple3<>(fromResourceId, linkType, toResourceId));
    }

    @Override
    public boolean linkDelete(String fromResourceId, String linkType, String toResourceId) {
        return links.removeIf(link -> //
        link.getA() == fromResourceId //
                && link.getB().equals(linkType) //
                && link.getC() == toResourceId);
    }

    @Override
    public boolean linkExists(String fromResourceId, String linkType, String toResourceId) {
        return links.stream().anyMatch(link -> //
        link.getA() == fromResourceId //
                && link.getB().equals(linkType) //
                && link.getC() == toResourceId);
    }

    @Override
    public boolean linkExistsByFromResourceAndLinkTypeAndToResource(IPResource fromResource, String linkType, IPResource toResource) {
        String fromInternalId = resourceFindIdByPk(fromResource);
        if (fromInternalId == null) {
            return false;
        }
        String toInternalId = resourceFindIdByPk(toResource);
        if (toInternalId == null) {
            return false;
        }
        return links.stream().filter( //
                it -> {
                    return fromInternalId.equals(it.getA()) && //
                    linkType.equals(it.getB()) && //
                    toInternalId.equals(it.getC());
                }) //
                .findAny().isPresent();
    }

    @Override
    public List<Tuple2<String, ? extends IPResource>> linkFindAllByFromResource(IPResource fromResource) {
        String fromInternalId = resourceFindIdByPk(fromResource);
        if (fromInternalId == null) {
            return Collections.emptyList();
        }
        return links.stream().filter( //
                it -> fromInternalId.equals(it.getA())) //
                .map(it -> new Tuple2<>(it.getB(), resourceFind(it.getC()).get())) //
                .collect(Collectors.toList());
    }

    @Override
    public List<Tuple2<String, ? extends IPResource>> linkFindAllByFromResource(String fromResourceId) {
        return links.stream().filter( //
                it -> it.getA().equals(fromResourceId)) //
                .map(it -> new Tuple2<>(it.getB(), resourceFind(it.getC()).get())) //
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends IPResource> linkFindAllByFromResourceAndLinkType(IPResource fromResource, String linkType) {
        String fromInternalId = resourceFindIdByPk(fromResource);
        if (fromInternalId == null) {
            return Collections.emptyList();
        }
        return links.stream().filter( //
                it -> fromInternalId.equals(it.getA()) && //
                        linkType.equals(it.getB())) //
                .map(it -> resourceFind(it.getC()).get()) //
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends IPResource> List<R> linkFindAllByFromResourceAndLinkTypeAndToResourceClass(IPResource fromResource, String linkType, Class<R> toResourceType) {
        String fromInternalId = resourceFindIdByPk(fromResource);
        if (fromInternalId == null) {
            return Collections.emptyList();
        }

        return links.stream().filter( //
                it -> {
                    IPResource toResource = resourceFind(it.getC()).get();
                    return fromInternalId.equals(it.getA()) && //
                    linkType.equals(it.getB()) && //
                    toResourceType.isInstance(toResource);
                }) //
                .map(it -> (R) resourceFind(it.getC()).get()) //
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends IPResource> List<R> linkFindAllByFromResourceClassAndLinkTypeAndToResource(Class<R> fromResourceClass, String linkType, IPResource toResource) {
        String toInternalId = resourceFindIdByPk(toResource);
        if (toInternalId == null) {
            return Collections.emptyList();
        }
        return links.stream().filter( //
                it -> {
                    IPResource fromResource = resourceFind(it.getA()).get();
                    return fromResourceClass.isInstance(fromResource) && //
                    linkType.equals(it.getB()) && //
                    toInternalId.equals(it.getC()); //
                }) //
                .map(it -> (R) resourceFind(it.getA()).get()) //
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends IPResource> linkFindAllByLinkTypeAndToResource(String linkType, IPResource toResource) {
        String toInternalId = resourceFindIdByPk(toResource);
        if (toInternalId == null) {
            return Collections.emptyList();
        }
        return links.stream().filter( //
                it -> {
                    return linkType.equals(it.getB()) && //
                    toInternalId.equals(it.getC()); //
                }) //
                .map(it -> resourceFind(it.getA()).get()) //
                .collect(Collectors.toList());
    }

    @Override
    public List<Tuple2<? extends IPResource, String>> linkFindAllByToResource(IPResource toResource) {
        String toInternalId = resourceFindIdByPk(toResource);
        if (toInternalId == null) {
            return Collections.emptyList();
        }
        return links.stream().filter( //
                it -> toInternalId.equals(it.getC())) //
                .map(it -> new Tuple2<>(resourceFind(it.getA()).get(), it.getB())) //
                .collect(Collectors.toList());
    }

    @Override
    public List<Tuple2<? extends IPResource, String>> linkFindAllByToResource(String toResourceId) {
        return links.stream().filter( //
                it -> it.getC().equals(toResourceId)) //
                .map(it -> new Tuple2<>(resourceFind(it.getA()).get(), it.getB())) //
                .collect(Collectors.toList());
    }

    @Override
    public List<Tuple3<IPResource, String, IPResource>> linkFindAllRelatedByResource(IPResource resource) {
        String internalId = resourceFindIdByPk(resource);
        if (internalId == null) {
            return Collections.emptyList();
        }
        return links.stream().filter( //
                it -> internalId.equals(it.getA()) || internalId.equals(it.getC())) //
                .map(it -> new Tuple3<>(resourceFind(it.getA()).get(), it.getB(), resourceFind(it.getC()).get())) //
                .collect(Collectors.toList());
    }

    @Override
    public List<Tuple3<IPResource, String, IPResource>> linkFindAllRelatedByResource(String internalResourceId) {
        return links.stream().filter( //
                it -> internalResourceId.equals(it.getA()) || internalResourceId.equals(it.getC())) //
                .map(it -> new Tuple3<>(resourceFind(it.getA()).get(), it.getB(), resourceFind(it.getC()).get())) //
                .collect(Collectors.toList());
    }

    protected boolean matchingLike(String likeQuery, String textToCheck) {
        return Pattern.compile(likeQuery.replaceAll("%", ".*")).matcher(textToCheck).matches();
    }

    @Override
    public IPResource resourceAdd(IPResource resource) {
        IPResource storedResource = JsonTools.clone(resource);
        storedResource.setInternalId(String.valueOf(nextInternalId.getAndIncrement()));
        resources.add(storedResource);
        return storedResource.deepClone();
    }

    @Override
    public void resourceAdd(IPResourceDefinition resourceDefinition) {
        resourceDefinitionByResourceClass.put(resourceDefinition.getResourceClass(), resourceDefinition);
        resourceDefinitionByResourceType.put(resourceDefinition.getResourceType(), resourceDefinition);

        allClassesByResourceClass.put(resourceDefinition.getResourceClass(), ReflectionTools.allTypes(resourceDefinition.getResourceClass()));
    }

    @Override
    public boolean resourceDelete(String resourceId) {
        links.removeIf(link -> StringTools.safeEquals(link.getA(), resourceId) || StringTools.safeEquals(link.getC(), resourceId));
        tags.removeIf(tag -> StringTools.safeEquals(tag.getA(), resourceId));
        return resources.removeIf(resource -> StringTools.safeEquals(resource.getInternalId(), resourceId));
    }

    @Override
    public <R extends IPResource, T extends IPResource> boolean resourceEqualsPk(R a, T b) {
        // nulls
        if (a == null) {
            return b == null;
        }
        if (b == null) {
            return false;
        }

        // Type
        Class<?> aClass = a.getClass();
        Class<?> bClass = b.getClass();
        if (!aClass.equals(bClass)) {
            return false;
        }

        // PK
        IPResourceDefinition resourceDefinition = resourceDefinitionByResourceClass.get(aClass);
        if (resourceDefinition == null) {
            return false;
        }
        for (String pkName : resourceDefinition.getPrimaryKeyProperties()) {
            Method pkGetter = resourceDefinition.getPropertyGetterMethod(pkName);
            Object aValue;
            Object bValue;
            try {
                aValue = pkGetter.invoke(a);
                bValue = pkGetter.invoke(b);
            } catch (Exception e) {
                return false;
            }

            // Nulls
            if (aValue == null) {
                if (bValue != null) {
                    return false;
                }
            } else {
                if (!aValue.equals(bValue)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public <T extends IPResource> Optional<T> resourceFind(IPResourceQuery<T> query) {
        List<T> results = resourceFindAll(query);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        AssertTools.assertTrue(results.size() <= 1, "There are more than one item matching the query");
        return Optional.of(results.get(0));
    }

    @Override
    public Optional<IPResource> resourceFind(String internalResourceId) {
        Optional<IPResource> resourceOptional = resources.stream() //
                .filter(it -> StringTools.safeEquals(internalResourceId, it.getInternalId())) //
                .map(it -> clone(it)) //
                .findAny();

        return resourceOptional;
    }

    @Override
    public List<? extends IPResource> resourceFindAll() {
        return Collections.unmodifiableList(resources);
    }

    @Override
    public <R extends IPResource> List<R> resourceFindAll(IPResourceQuery<R> query) {
        return resourceFindAllNoCloning(query).stream() //
                .map(it -> clone(it)) //
                .collect(Collectors.toList());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <R extends IPResource> List<R> resourceFindAllNoCloning(IPResourceQuery<R> query) {
        List<R> results = resources.stream() //
                .filter(resource -> {

                    List<IPResourceDefinition> resourceDefinitions = query.getResourceDefinitions();

                    // Right type
                    List<Class<? extends IPResource>> resourceClasses = resourceDefinitions.stream().map(IPResourceDefinition::getResourceClass).collect(Collectors.toList());
                    if (!resourceClasses.contains(resource.getClass())) {
                        return false;
                    }

                    // Right ids
                    if (query.getIdsIn() != null && !query.getIdsIn().contains(resource.getInternalId())) {
                        return false;
                    }

                    // Right editor
                    if (query.getEditorsIn() != null && !query.getEditorsIn().contains(resource.getResourceEditorName())) {
                        return false;
                    }

                    // Equals
                    IPResourceDefinition currentResourceDefinition = resourceDefinitionByResourceClass.get(resource.getClass());
                    for (Entry<String, Object> entry : query.getPropertyEquals().entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (propertyValue == null) {
                                if (currentValue == null) {
                                    // Good value
                                    continue;
                                } else {
                                    // Wrong value
                                    return false;
                                }
                            }
                            if (currentValue instanceof Set) {
                                Set currentValueSet = (Set) currentValue;
                                if (propertyValue instanceof Collection) {
                                    Collection<?> propertyValueCollection = (Collection<?>) propertyValue;
                                    if (currentValueSet.size() != propertyValueCollection.size()) {
                                        return false;
                                    }
                                    for (Object it : currentValueSet) {
                                        if (!propertyValueCollection.contains(it)) {
                                            return false;
                                        }
                                    }
                                    continue;
                                } else {
                                    return false;
                                }
                            } else {
                                if (currentValue == null && propertyValue instanceof Collection) {
                                    Collection<?> propertyValueCollection = (Collection<?>) propertyValue;
                                    if (!propertyValueCollection.isEmpty()) {
                                        return false;
                                    }
                                } else {
                                    if (!propertyValue.equals(currentValue)) {
                                        // Wrong value
                                        return false;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // Contains
                    for (Entry<String, Object> entry : query.getPropertyContains().entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (currentValue instanceof Set) {
                                Set currentValueSet = (Set) currentValue;
                                if (propertyValue instanceof Collection) {
                                    Collection<?> propertyValueCollection = (Collection<?>) propertyValue;
                                    for (Object it : propertyValueCollection) {
                                        if (!currentValueSet.contains(it)) {
                                            return false;
                                        }
                                    }
                                    continue;
                                } else {
                                    return false;
                                }
                            } else {
                                // Wrong value
                                return false;
                            }
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // Like
                    for (Entry<String, String> entry : query.getPropertyLike().entrySet()) {
                        String propertyName = entry.getKey();
                        String propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (currentValue == null) {
                                // Wrong value
                                return false;
                            }

                            if (!matchingLike(propertyValue, (String) currentValue)) {
                                // Wrong value
                                return false;
                            }
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // Greater than
                    for (Entry<String, Object> entry : query.getPropertyGreater().entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (currentValue == null) {
                                // Wrong value
                                return false;
                            }
                            if (propertyValue instanceof Integer) {
                                if (((int) currentValue) > ((int) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Long) {
                                if (((long) currentValue) > ((long) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Float) {
                                if (((float) currentValue) > ((float) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Double) {
                                if (((double) currentValue) > ((double) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Date) {
                                if (((Date) currentValue).getTime() > ((Date) propertyValue).getTime()) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Enum) {
                                if (((Enum) currentValue).ordinal() > ((Enum) propertyValue).ordinal()) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            return false;
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // Greater and equals
                    for (Entry<String, Object> entry : query.getPropertyGreaterEquals().entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (currentValue == null) {
                                // Wrong value
                                return false;
                            }
                            if (propertyValue instanceof Integer) {
                                if (((int) currentValue) >= ((int) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Long) {
                                if (((long) currentValue) >= ((long) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Float) {
                                if (((float) currentValue) >= ((float) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Double) {
                                if (((double) currentValue) >= ((double) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Date) {
                                if (((Date) currentValue).getTime() >= ((Date) propertyValue).getTime()) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Enum) {
                                if (((Enum) currentValue).ordinal() >= ((Enum) propertyValue).ordinal()) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            return false;
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // Lesser than
                    for (Entry<String, Object> entry : query.getPropertyLesser().entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (currentValue == null) {
                                // Wrong value
                                return false;
                            }
                            if (propertyValue instanceof Integer) {
                                if (((int) currentValue) < ((int) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Long) {
                                if (((long) currentValue) < ((long) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Float) {
                                if (((float) currentValue) < ((float) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Double) {
                                if (((double) currentValue) < ((double) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Date) {
                                if (((Date) currentValue).getTime() < ((Date) propertyValue).getTime()) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Enum) {
                                if (((Enum) currentValue).ordinal() < ((Enum) propertyValue).ordinal()) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            return false;
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // Lesser and equals
                    for (Entry<String, Object> entry : query.getPropertyLesserAndEquals().entrySet()) {
                        String propertyName = entry.getKey();
                        Object propertyValue = entry.getValue();

                        try {
                            Object currentValue = currentResourceDefinition.getPropertyGetterMethod(propertyName).invoke(resource);
                            if (currentValue == null) {
                                // Wrong value
                                return false;
                            }
                            if (propertyValue instanceof Integer) {
                                if (((int) currentValue) <= ((int) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Long) {
                                if (((long) currentValue) <= ((long) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Float) {
                                if (((float) currentValue) <= ((float) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Double) {
                                if (((double) currentValue) <= ((double) propertyValue)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Date) {
                                if (((Date) currentValue).getTime() <= ((Date) propertyValue).getTime()) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            if (propertyValue instanceof Enum) {
                                if (((Enum) currentValue).ordinal() <= ((Enum) propertyValue).ordinal()) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }
                            return false;
                        } catch (Exception e) {
                            return false;
                        }

                    }

                    // All the tags
                    Set<String> tags = tagFindAllByResource(resource);
                    for (String tag : query.getTagsAnd()) {
                        if (!tags.contains(tag)) {
                            // Missing tag
                            return false;
                        }
                    }
                    if (!query.getTagsOr().isEmpty()) {
                        boolean gotOne = false;
                        for (String tag : query.getTagsOr()) {
                            if (tags.contains(tag)) {
                                // Got one
                                gotOne = true;
                                break;
                            }
                        }
                        if (!gotOne) {
                            return false;
                        }
                    }

                    return true;
                }) //
                .map(it -> (R) it) //
                .collect(Collectors.toList());
        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends IPResource> Optional<R> resourceFindByPk(R resource) {
        return resources.stream().filter(it -> resourceEqualsPk(it, resource)).map(it -> clone((R) it)).findAny();
    }

    private String resourceFindIdByPk(IPResource resource) {
        // Id already there
        if (resource.getInternalId() != null) {
            return resource.getInternalId();
        }

        // Search by PK
        Optional<IPResource> o = resourceFindByPk(resource);
        if (o.isPresent()) {
            return o.get().getInternalId();
        }

        // Does not exist
        return null;
    }

    @Override
    public void resourceUpdate(IPResource previousResource, IPResource updatedResource) {
        AssertTools.assertTrue(resources.removeIf(resource -> resource.getInternalId() == previousResource.getInternalId()), "Cannot update a resource that does not exist");
        IPResource storedResource = JsonTools.clone(updatedResource);
        storedResource.setInternalId(previousResource.getInternalId());
        resources.add(storedResource);
    }

    public void setCommonServicesContext(CommonServicesContext commonServicesContext) {
        this.commonServicesContext = commonServicesContext;
    }

    @Override
    public void setDefaultChangeExecutionHooks(List<ChangeExecutionHook> defaultChangeExecutionHooks) {
        this.defaultChangeExecutionHooks = defaultChangeExecutionHooks;
    }

    @Override
    public void setInfiniteLoopTimeoutInMs(long infiniteLoopTimeoutInMs) {
        this.infiniteLoopTimeoutInMs = infiniteLoopTimeoutInMs;
    }

    public void setInternalServicesContext(InternalServicesContext internalServicesContext) {
        this.internalServicesContext = internalServicesContext;
    }

    @Override
    public void tagAdd(String resourceId, String tagName) {
        tags.add(new Tuple2<>(resourceId, tagName));
    }

    @Override
    public boolean tagDelete(String resourceId, String tagName) {
        return tags.removeIf(tag -> //
        tag.getA() == resourceId //
                && tag.getB().equals(tagName));
    }

    @Override
    public boolean tagExists(String resourceId, String tagName) {
        return tags.stream().anyMatch(tag -> //
        tag.getA() == resourceId //
                && tag.getB().equals(tagName));
    }

    @Override
    public Set<String> tagFindAllByResource(IPResource resource) {
        String resourceId = resourceFindIdByPk(resource);
        if (resourceId == null) {
            return Collections.emptySet();
        }

        return tags.stream() //
                .filter(it -> resourceId.equals(it.getA())) //
                .map(it -> it.getB()) //
                .collect(Collectors.toSet());
    }

}
