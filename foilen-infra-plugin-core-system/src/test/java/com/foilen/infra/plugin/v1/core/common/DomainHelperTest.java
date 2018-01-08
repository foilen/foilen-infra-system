/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.common;

import org.junit.Assert;
import org.junit.Test;

public class DomainHelperTest {

    @Test
    public void testIsValidDomainName() {
        Assert.assertTrue(DomainHelper.isValidDomainName("example.com"));
        Assert.assertTrue(DomainHelper.isValidDomainName("www.example.com"));
        Assert.assertTrue(DomainHelper.isValidDomainName("123.example.com"));
        Assert.assertTrue(DomainHelper.isValidDomainName("www.example-is-good.com"));
        Assert.assertTrue(DomainHelper.isValidDomainName("www.éxample.com"));

        Assert.assertFalse(DomainHelper.isValidDomainName("com"));
        Assert.assertFalse(DomainHelper.isValidDomainName(".example.com"));
        Assert.assertFalse(DomainHelper.isValidDomainName("www.exampl/e.com"));
        Assert.assertFalse(DomainHelper.isValidDomainName("www..com"));
        Assert.assertFalse(DomainHelper.isValidDomainName("."));
        Assert.assertFalse(DomainHelper.isValidDomainName(""));
        Assert.assertFalse(DomainHelper.isValidDomainName("www.example@good.com"));
        Assert.assertFalse(DomainHelper.isValidDomainName("www.example good.com"));
    }

    @Test
    public void testParentDomainName() {
        Assert.assertEquals("node.example.com", DomainHelper.parentDomainName("f001.node.example.com"));
        Assert.assertEquals("example.com", DomainHelper.parentDomainName("node.example.com"));
        Assert.assertEquals(null, DomainHelper.parentDomainName("example.com"));
    }

    @Test
    public void testReverseDomainName() {
        Assert.assertEquals("com.example", DomainHelper.reverseDomainName("example.com"));
        Assert.assertEquals("com.example.www", DomainHelper.reverseDomainName("www.example.com"));
    }

}
