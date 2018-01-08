/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.cert;

import org.junit.Test;

import com.foilen.infra.plugin.v1.core.base.resources.WebsiteCertificate;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.ResourceTools;

public class WebsiteCertificateTest {

    @Test
    public void testLegacyJson() {

        WebsiteCertificate oldWebsiteCertificate = JsonTools.readFromResource("WebsiteCertificateTest-testLegacyJson-old.json", WebsiteCertificate.class, getClass());

        String newJson = JsonTools.prettyPrint(oldWebsiteCertificate);
        String expectedJson = ResourceTools.getResourceAsString("WebsiteCertificateTest-testLegacyJson-expected.json", getClass());
        AssertTools.assertIgnoreLineFeed(expectedJson, newJson);
    }

}
