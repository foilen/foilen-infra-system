/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.impl;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.foilen.infra.plugin.system.utils.DockerUtils;
import com.foilen.infra.plugin.system.utils.mock.UnixShellAndFsUtilsMock;
import com.foilen.infra.plugin.system.utils.model.ApplicationBuildDetails;
import com.foilen.infra.plugin.system.utils.model.ContainersManageContext;
import com.foilen.infra.plugin.system.utils.model.DockerPs;
import com.foilen.infra.plugin.system.utils.model.DockerState;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.docker.DockerContainerEndpoints;
import com.foilen.infra.plugin.v1.model.outputter.docker.DockerContainerOutputContext;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.ResourceTools;
import com.google.common.base.Joiner;

public class DockerUtilsImplTest {

    private static final Joiner JOINER_NEW_LINE = Joiner.on("\n");

    private void assertDockerPs(String expectedList, String actualOutput) {
        List<DockerPs> expected = JsonTools.readFromResourceAsList(expectedList, DockerPs.class, this.getClass());
        List<DockerPs> actual = new DockerUtilsImpl().convertToDockerPs(ResourceTools.getResourceAsString(actualOutput, this.getClass()));

        AssertTools.assertJsonComparison(expected, actual);
    }

    private void assertList(List<String> expected, List<String> actual) {
        Assert.assertEquals(JOINER_NEW_LINE.join(expected), JOINER_NEW_LINE.join(actual));
    }

    @Test
    public void testContainersManage() {

        UnixShellAndFsUtilsMock unixShellAndFsUtils = new UnixShellAndFsUtilsMock();
        unixShellAndFsUtils.setExecuteCommandQuietAndGetOutputCallback((actionName, actionDetails, command, arguments) -> //
        "11111111111111\tapp1_mysql\t2018-06-25 07:14:58 -0400 EDT\t2 weeks ago\tUp\t0B (virtual 407MB)" //
                + "\n222222222222\tapp1\t2018-06-25 07:14:58 -0400 EDT\t2 weeks ago\tUp\t0B (virtual 407MB)" //
                + "\n333333333\tinfra_redirector_exit\t2018-06-25 07:14:58 -0400 EDT\t2 weeks ago\tUp\t0B (virtual 407MB)" //
                + "\n444444\tinfra_redirector_entry\t2018-06-25 07:14:58 -0400 EDT\t2 weeks ago\tUp\t0B (virtual 407MB)");
        DockerUtils dockerUtils = new DockerUtilsImpl(unixShellAndFsUtils);

        ContainersManageContext containersManageContext = new ContainersManageContext();
        DockerState dockerState = new DockerState();
        containersManageContext.setDockerState(dockerState);

        // alwaysRunningApplications
        {
            DockerContainerOutputContext outputContext = new DockerContainerOutputContext("app1", "app1");
            IPApplicationDefinition applicationDefinition = new IPApplicationDefinition();
            applicationDefinition.addPortEndpoint(8080, DockerContainerEndpoints.HTTP_TCP);
            applicationDefinition.addPortRedirect(3306, "localhost", "app1_mysql", DockerContainerEndpoints.MYSQL_TCP);
            applicationDefinition.setRunAs(65000L);
            applicationDefinition.setCommand("/app1.sh");
            containersManageContext.getAlwaysRunningApplications().add(new ApplicationBuildDetails() //
                    .setOutputContext(outputContext) //
                    .setApplicationDefinition(applicationDefinition));
        }
        {
            DockerContainerOutputContext outputContext = new DockerContainerOutputContext("app1_mysql", "app1_mysql");
            IPApplicationDefinition applicationDefinition = new IPApplicationDefinition();
            applicationDefinition.addPortEndpoint(3306, DockerContainerEndpoints.MYSQL_TCP);
            applicationDefinition.setRunAs(65000L);
            applicationDefinition.setCommand("/mysql-start.sh");
            containersManageContext.getAlwaysRunningApplications().add(new ApplicationBuildDetails() //
                    .setOutputContext(outputContext) //
                    .setApplicationDefinition(applicationDefinition));
        }

        // Execute first time
        List<String> updatedInstanceNames = dockerUtils.containersManage(containersManageContext);
        assertList(Arrays.asList("app1_mysql", "app1", "infra_redirector_exit"), updatedInstanceNames);

        // Executing a second time (should not change anything)
        updatedInstanceNames = dockerUtils.containersManage(containersManageContext);
        assertList(Arrays.asList(), updatedInstanceNames);

        // Update
        containersManageContext.getAlwaysRunningApplications().clear();
        {
            DockerContainerOutputContext outputContext = new DockerContainerOutputContext("app1", "app1");
            IPApplicationDefinition applicationDefinition = new IPApplicationDefinition();
            applicationDefinition.addPortEndpoint(8080, DockerContainerEndpoints.HTTP_TCP);
            applicationDefinition.addPortRedirect(3306, "remote", "app1_mysql", DockerContainerEndpoints.MYSQL_TCP);
            applicationDefinition.setRunAs(65000L);
            applicationDefinition.setCommand("/app1.sh");
            containersManageContext.getAlwaysRunningApplications().add(new ApplicationBuildDetails() //
                    .setOutputContext(outputContext) //
                    .setApplicationDefinition(applicationDefinition));
        }

        // Execute (should change)
        updatedInstanceNames = dockerUtils.containersManage(containersManageContext);
        assertList(Arrays.asList("infra_redirector_entry", "app1", "infra_redirector_exit"), updatedInstanceNames);

    }

    @Test
    public void testConvertToDockerPs() {
        assertDockerPs("DockerUtilsImplTest-testConvertToDockerPs-nothing-expected.json", "DockerUtilsImplTest-testConvertToDockerPs-nothing.txt");
        assertDockerPs("DockerUtilsImplTest-testConvertToDockerPs-some-expected.json", "DockerUtilsImplTest-testConvertToDockerPs-some.txt");
    }

}
