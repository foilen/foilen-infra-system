/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.impl;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class SearchNextIpTest {

    @Test
    public void test_changing_C() {
        List<String> existingIps = new ArrayList<>();
        existingIps.add("172.20.5.1");
        existingIps.add("172.20.5.2");
        existingIps.add("172.20.5.3");
        existingIps.add("172.20.8.5");
        SearchNextIp searchNextIp = new SearchNextIp(existingIps.stream());

        // 4 to 255
        for (int d = 4; d <= 255; ++d) {
            Assert.assertEquals("172.20.5." + d, searchNextIp.getNext());
        }
        // 1 to 255
        for (int d = 1; d <= 255; ++d) {
            Assert.assertEquals("172.20.6." + d, searchNextIp.getNext());
        }
        // 1 to 255
        for (int d = 1; d <= 255; ++d) {
            Assert.assertEquals("172.20.7." + d, searchNextIp.getNext());
        }
        // 1 to 4
        for (int d = 1; d <= 4; ++d) {
            Assert.assertEquals("172.20.8." + d, searchNextIp.getNext());
        }
        // 6 to 255
        for (int d = 6; d <= 255; ++d) {
            Assert.assertEquals("172.20.8." + d, searchNextIp.getNext());
        }

    }

    @Test
    public void test_empty() {
        List<String> existingIps = new ArrayList<>();
        SearchNextIp searchNextIp = new SearchNextIp(existingIps.stream());

        Assert.assertEquals("172.20.5.1", searchNextIp.getNext());
        Assert.assertEquals("172.20.5.2", searchNextIp.getNext());
        Assert.assertEquals("172.20.5.3", searchNextIp.getNext());
        Assert.assertEquals("172.20.5.4", searchNextIp.getNext());
    }

    @Test
    public void test_rolling() {
        List<String> existingIps = new ArrayList<>();
        existingIps.add("172.20.5.2");
        SearchNextIp searchNextIp = new SearchNextIp(existingIps.stream());

        searchNextIp.c = 255;

        // 1 to 254
        for (int d = 1; d <= 254; ++d) {
            Assert.assertEquals("172.20.255." + d, searchNextIp.getNext());
        }

        Assert.assertEquals("172.20.5.1", searchNextIp.getNext());
        Assert.assertEquals("172.20.5.3", searchNextIp.getNext());
        Assert.assertEquals("172.20.5.4", searchNextIp.getNext());

    }

    @Test
    public void test_withExisting() {
        List<String> existingIps = new ArrayList<>();
        existingIps.add("172.20.5.3");
        existingIps.add("172.20.5.5");
        existingIps.add("172.20.5.6");
        SearchNextIp searchNextIp = new SearchNextIp(existingIps.stream());

        Assert.assertEquals("172.20.5.1", searchNextIp.getNext());
        Assert.assertEquals("172.20.5.2", searchNextIp.getNext());
        Assert.assertEquals("172.20.5.4", searchNextIp.getNext());
        Assert.assertEquals("172.20.5.7", searchNextIp.getNext());
        Assert.assertEquals("172.20.5.8", searchNextIp.getNext());
    }

}
