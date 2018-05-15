/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.foilen.infra.plugin.v1.core.resource.IPResourceDefinition;
import com.foilen.infra.plugin.v1.core.resource.IPResourceQuery;
import com.foilen.infra.plugin.v1.model.resource.IPResource;
import com.foilen.smalltools.tuple.Tuple2;
import com.foilen.smalltools.tuple.Tuple3;

/**
 * To find any custom resource.
 */
public interface IPResourceService {

    /**
     * Create a query object to use to retrieve resources.
     *
     * @param resourceClass
     *            the class of the resource
     * @return a new resource query
     * @param <T>
     *            type of resource
     */
    <T extends IPResource> IPResourceQuery<T> createResourceQuery(Class<T> resourceClass);

    /**
     * Create a query object to use to retrieve resources.
     *
     * @param resourceType
     *            the type of the resource
     * @return a new resource query
     * @param <T>
     *            type of resource
     */
    <T extends IPResource> IPResourceQuery<T> createResourceQuery(String resourceType);

    /**
     * Get the resource definition for the resource class.
     *
     * @param resourceClass
     *            the resource class
     * @return the resource definition
     */
    IPResourceDefinition getResourceDefinition(Class<? extends IPResource> resourceClass);

    /**
     * Get the resource definition for the resource.
     *
     * @param resource
     *            the resource
     * @return the resource definition
     */
    IPResourceDefinition getResourceDefinition(IPResource resource);

    /**
     * Get the resource definition for the resource type.
     *
     * @param resourceType
     *            the resource type
     * @return the resource definition
     */
    IPResourceDefinition getResourceDefinition(String resourceType);

    /**
     * Get all the resource definitions.
     *
     * @return the resource definitions
     */
    List<IPResourceDefinition> getResourceDefinitions();

    /**
     * Tells if the link exists.
     *
     * @param fromResource
     *            the "from" resource
     * @param linkType
     *            the link type
     * @param toResource
     *            the "to" resource
     * @return true if the link exists
     */
    boolean linkExistsByFromResourceAndLinkTypeAndToResource(IPResource fromResource, String linkType, IPResource toResource);

    /**
     * Find all the "to" resources.
     *
     * @param fromResource
     *            the "from" resource
     * @return all the linkType -&gt; "to" resources
     */
    List<Tuple2<String, ? extends IPResource>> linkFindAllByFromResource(IPResource fromResource);

    /**
     * Find all the "to" resources.
     *
     * @param fromResource
     *            the "from" resource
     * @param linkType
     *            the link type
     * @return all the "to" resources
     */
    List<? extends IPResource> linkFindAllByFromResourceAndLinkType(IPResource fromResource, String linkType);

    /**
     * Find all the "to" resources.
     *
     * @param fromResource
     *            the "from" resource
     * @param linkType
     *            the link type
     * @param toResourceClass
     *            the "to" resource type
     * @return all the "to" resources
     * @param <R>
     *            type of linked resource
     */
    <R extends IPResource> List<R> linkFindAllByFromResourceAndLinkTypeAndToResourceClass(IPResource fromResource, String linkType, Class<R> toResourceClass);

    /**
     * Find all the "from" resources.
     *
     * @param fromResourceClass
     *            the "from" resource type
     * @param linkType
     *            the link type
     * @param toResource
     *            the "to" resource
     * @return all the "from" resources
     * @param <R>
     *            type of linked resource
     */
    <R extends IPResource> List<R> linkFindAllByFromResourceClassAndLinkTypeAndToResource(Class<R> fromResourceClass, String linkType, IPResource toResource);

    /**
     * Find all the "from" resources.
     *
     * @param linkType
     *            the link type
     * @param toResource
     *            the "to" resource
     * @return all the "from" resources
     */
    List<? extends IPResource> linkFindAllByLinkTypeAndToResource(String linkType, IPResource toResource);

    /**
     * Find all the "from" resources.
     *
     * @param toResource
     *            the "to" resource
     * @return all the "from" resources
     */
    List<Tuple2<? extends IPResource, String>> linkFindAllByToResource(IPResource toResource);

    /**
     * Find the links that are "from" or "to" the resource.
     *
     * @param resource
     *            the resource
     * @return the links
     */
    List<Tuple3<IPResource, String, IPResource>> linkFindAllRelatedByResource(IPResource resource);

    /**
     * Find the links that are "from" or "to" the resource.
     *
     * @param internalResourceId
     *            the resource id
     * @return the links
     */
    List<Tuple3<IPResource, String, IPResource>> linkFindAllRelatedByResource(Long internalResourceId);

    /**
     * Tells if 2 resources has their current primary keys equals.
     *
     * @param a
     *            the first resource
     * @param b
     *            the second resource
     * @return true if the pk is the same
     * @param <R>
     *            type of resource
     * @param <T>
     *            type of resource
     */
    <R extends IPResource, T extends IPResource> boolean resourceEqualsPk(R a, T b);

    /**
     * Find the resource that matches the query.
     *
     * @param query
     *            the query
     * @return the resource
     * @param <T>
     *            type of resource
     */
    <T extends IPResource> Optional<T> resourceFind(IPResourceQuery<T> query);

    Optional<IPResource> resourceFind(long internalResourceId);

    /**
     * Find all the resources that match the query.
     *
     * @param query
     *            the query
     * @return the resources
     * @param <T>
     *            type of resource
     */
    <T extends IPResource> List<T> resourceFindAll(IPResourceQuery<T> query);

    /**
     * Find a resource that has the right primary key.
     *
     * @param resource
     *            the resource with the primary key
     * @return the resource if present
     * @param <R>
     *            type of resource
     */
    <R extends IPResource> Optional<R> resourceFindByPk(R resource);

    Set<String> tagFindAllByResource(IPResource resource);

}
