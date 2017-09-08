/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.outputter.msmtp;

import org.junit.Test;

import com.foilen.infra.plugin.v1.model.msmtp.MsmtpConfig;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.ResourceTools;

public class MsmtpConfigOutputTest {

    @Test
    public void testToConfig() {
        String expected = ResourceTools.getResourceAsString("MsmtpConfigOutputTest-testToConfig-expected.txt", getClass());
        String actual = MsmtpConfigOutput.toConfig(new MsmtpConfig("192.168.0.1", 547));

        AssertTools.assertIgnoreLineFeed(expected, actual);
    }

}
