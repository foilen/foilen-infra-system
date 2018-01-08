/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.impl;

import java.util.List;

import org.junit.Test;

import com.foilen.infra.plugin.system.utils.model.DockerPs;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.ResourceTools;

public class DockerUtilsImplTest {

    private void assertDockerPs(String expectedList, String actualOutput) {
        List<DockerPs> expected = JsonTools.readFromResourceAsList(expectedList, DockerPs.class, this.getClass());
        List<DockerPs> actual = new DockerUtilsImpl().convertToDockerPs(ResourceTools.getResourceAsString(actualOutput, this.getClass()));

        AssertTools.assertJsonComparison(expected, actual);
    }

    @Test
    public void testConvertToDockerPs() {
        assertDockerPs("DockerUtilsImplTest-testConvertToDockerPs-nothing-expected.json", "DockerUtilsImplTest-testConvertToDockerPs-nothing.txt");
        assertDockerPs("DockerUtilsImplTest-testConvertToDockerPs-some-expected.json", "DockerUtilsImplTest-testConvertToDockerPs-some.txt");
    }

}
