/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.memory.service;

import org.junit.Assert;
import org.junit.Test;

import com.foilen.infra.plugin.core.system.junits.AbstractIPResourceServiceTest;
import com.foilen.infra.plugin.core.system.memory.junits.ResourceServicesInMemoryTests;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;

public class ResourceServicesInMemoryImplTest extends AbstractIPResourceServiceTest {

    private ResourceServicesInMemoryImpl resourceServicesInMemoryImpl;

    public ResourceServicesInMemoryImplTest() {
        resourceServicesInMemoryImpl = ResourceServicesInMemoryTests.init();
    }

    @Override
    protected CommonServicesContext getCommonServicesContext() {
        return resourceServicesInMemoryImpl.getCommonServicesContext();
    }

    @Override
    protected InternalServicesContext getInternalServicesContext() {
        return resourceServicesInMemoryImpl.getInternalServicesContext();
    }

    @Test
    public void testMatchingLike_beginning() {
        Assert.assertTrue(resourceServicesInMemoryImpl.matchingLike("%bcd", "bcd"));
        Assert.assertTrue(resourceServicesInMemoryImpl.matchingLike("%bcd", "abcd"));
        Assert.assertTrue(resourceServicesInMemoryImpl.matchingLike("%bcd", "zabcd"));
        Assert.assertFalse(resourceServicesInMemoryImpl.matchingLike("%bcd", "zabcdd"));
    }

    @Test
    public void testMatchingLike_ending() {
        Assert.assertTrue(resourceServicesInMemoryImpl.matchingLike("abc%", "abc"));
        Assert.assertTrue(resourceServicesInMemoryImpl.matchingLike("abc%", "abcd"));
        Assert.assertTrue(resourceServicesInMemoryImpl.matchingLike("abc%", "abcde"));
        Assert.assertFalse(resourceServicesInMemoryImpl.matchingLike("abc%", "zabcde"));
    }

    @Test
    public void testMatchingLike_middle() {
        Assert.assertTrue(resourceServicesInMemoryImpl.matchingLike("a%c", "ac"));
        Assert.assertTrue(resourceServicesInMemoryImpl.matchingLike("a%c", "abc"));
        Assert.assertTrue(resourceServicesInMemoryImpl.matchingLike("a%c", "abtc"));
        Assert.assertFalse(resourceServicesInMemoryImpl.matchingLike("a%c", "zabtc"));
        Assert.assertFalse(resourceServicesInMemoryImpl.matchingLike("a%c", "abtcz"));
    }

}
