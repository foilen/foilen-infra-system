/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.service;

import org.junit.Assert;
import org.junit.Test;

import com.foilen.infra.plugin.core.system.fake.junits.FakeSystemServicesTests;
import com.foilen.infra.plugin.core.system.junits.AbstractIPResourceServiceTest;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.internal.InternalServicesContext;

public class FakeSystemServicesImplTest extends AbstractIPResourceServiceTest {

    private FakeSystemServicesImpl fakeSystemServicesImpl;

    public FakeSystemServicesImplTest() {
        fakeSystemServicesImpl = FakeSystemServicesTests.init();
    }

    @Override
    protected CommonServicesContext getCommonServicesContext() {
        return fakeSystemServicesImpl.getCommonServicesContext();
    }

    @Override
    protected InternalServicesContext getInternalServicesContext() {
        return fakeSystemServicesImpl.getInternalServicesContext();
    }

    @Test
    public void testMatchingLike_beginning() {
        Assert.assertTrue(fakeSystemServicesImpl.matchingLike("%bcd", "bcd"));
        Assert.assertTrue(fakeSystemServicesImpl.matchingLike("%bcd", "abcd"));
        Assert.assertTrue(fakeSystemServicesImpl.matchingLike("%bcd", "zabcd"));
        Assert.assertFalse(fakeSystemServicesImpl.matchingLike("%bcd", "zabcdd"));
    }

    @Test
    public void testMatchingLike_ending() {
        Assert.assertTrue(fakeSystemServicesImpl.matchingLike("abc%", "abc"));
        Assert.assertTrue(fakeSystemServicesImpl.matchingLike("abc%", "abcd"));
        Assert.assertTrue(fakeSystemServicesImpl.matchingLike("abc%", "abcde"));
        Assert.assertFalse(fakeSystemServicesImpl.matchingLike("abc%", "zabcde"));
    }

    @Test
    public void testMatchingLike_middle() {
        Assert.assertTrue(fakeSystemServicesImpl.matchingLike("a%c", "ac"));
        Assert.assertTrue(fakeSystemServicesImpl.matchingLike("a%c", "abc"));
        Assert.assertTrue(fakeSystemServicesImpl.matchingLike("a%c", "abtc"));
        Assert.assertFalse(fakeSystemServicesImpl.matchingLike("a%c", "zabtc"));
        Assert.assertFalse(fakeSystemServicesImpl.matchingLike("a%c", "abtcz"));
    }

}
