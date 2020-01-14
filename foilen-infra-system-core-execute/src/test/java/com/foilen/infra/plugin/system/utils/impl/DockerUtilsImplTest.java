/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.foilen.infra.plugin.v1.model.redirectportregistry.RedirectPortRegistryEntries;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.ResourceTools;
import com.google.common.base.Joiner;
import com.google.common.io.Files;

public class DockerUtilsImplTest {

    private static final Joiner JOINER_NEW_LINE = Joiner.on("\n");

    private void assertDockerPs(String expectedList, String actualOutput) {
        List<DockerPs> actual = new DockerUtilsImpl().convertToDockerPs(ResourceTools.getResourceAsString(actualOutput, this.getClass()));
        List<DockerPs> expected = JsonTools.readFromResourceAsList(expectedList, DockerPs.class, this.getClass());

        AssertTools.assertJsonComparison(expected, actual);
    }

    private void assertList(List<String> expected, List<String> actual) {
        Assert.assertEquals(JOINER_NEW_LINE.join(expected), JOINER_NEW_LINE.join(actual));
    }

    @Test
    public void testContainersManage_manyApplicationsPointingToSameRemoteEndpoint() {

        UnixShellAndFsUtilsMock unixShellAndFsUtils = new UnixShellAndFsUtilsMock();
        unixShellAndFsUtils.setExecuteCommandQuietAndGetOutputCallback((actionName, actionDetails, command, arguments) -> { //

            if ("ps".equals(actionDetails)) {
                return "";
            }

            throw new RuntimeException("Mock: Not implemented");
        });
        DockerUtils dockerUtils = new DockerUtilsImpl(unixShellAndFsUtils);

        DockerState dockerState = new DockerState();
        String baseOutputDirectory = Files.createTempDir().getAbsolutePath();
        ContainersManageContext containersManageContext = new ContainersManageContext().setDockerState(dockerState).setBaseOutputDirectory(baseOutputDirectory);

        // Add multiple applications going on multiple remote points
        {
            DockerContainerOutputContext outputContext = new DockerContainerOutputContext("app1", "app1");
            IPApplicationDefinition applicationDefinition = new IPApplicationDefinition();
            applicationDefinition.addPortEndpoint(8080, DockerContainerEndpoints.HTTP_TCP);
            applicationDefinition.addPortRedirect(3306, "remote.example.com", "app1_mysql", DockerContainerEndpoints.MYSQL_TCP);
            applicationDefinition.setRunAs(65000L);
            applicationDefinition.setCommand("/app1.sh");
            containersManageContext.getAlwaysRunningApplications().add(new ApplicationBuildDetails() //
                    .setOutputContext(outputContext) //
                    .setApplicationDefinition(applicationDefinition));
        }
        {
            DockerContainerOutputContext outputContext = new DockerContainerOutputContext("app2", "app2");
            IPApplicationDefinition applicationDefinition = new IPApplicationDefinition();
            applicationDefinition.addPortEndpoint(8080, DockerContainerEndpoints.HTTP_TCP);
            applicationDefinition.addPortRedirect(3306, "remote.example.com", "app2_mysql", DockerContainerEndpoints.MYSQL_TCP);
            applicationDefinition.setRunAs(65000L);
            applicationDefinition.setCommand("/app2.sh");
            containersManageContext.getAlwaysRunningApplications().add(new ApplicationBuildDetails() //
                    .setOutputContext(outputContext) //
                    .setApplicationDefinition(applicationDefinition));
        }
        {
            DockerContainerOutputContext outputContext = new DockerContainerOutputContext("app-both", "app-both");
            IPApplicationDefinition applicationDefinition = new IPApplicationDefinition();
            applicationDefinition.addPortEndpoint(8080, DockerContainerEndpoints.HTTP_TCP);
            applicationDefinition.addPortRedirect(3306, "remote.example.com", "app1_mysql", DockerContainerEndpoints.MYSQL_TCP);
            applicationDefinition.addPortRedirect(3307, "remote.example.com", "app2_mysql", DockerContainerEndpoints.MYSQL_TCP);
            applicationDefinition.setRunAs(65000L);
            applicationDefinition.setCommand("/app-both.sh");
            containersManageContext.getAlwaysRunningApplications().add(new ApplicationBuildDetails() //
                    .setOutputContext(outputContext) //
                    .setApplicationDefinition(applicationDefinition));
        }

        // Execute first time
        List<String> updatedInstanceNames = dockerUtils.containersManage(containersManageContext);
        assertList(Arrays.asList("infra_redirector_entry", "app-both", "app1", "app2", "infra_redirector_exit"), updatedInstanceNames);

        // Assert infra_redirector_entry
        RedirectPortRegistryEntries entries = JsonTools.readFromString(unixShellAndFsUtils.getFileContentByPath().get("infra_redirector_entry:/data/entry.json"), RedirectPortRegistryEntries.class);
        AssertTools.assertJsonComparison("DockerUtilsImplTest-testContainersManage_manyApplicationsPointingToSameRemoteEndpoint.json", getClass(), entries);
        ;
    }

    @Test
    public void testContainersManage_multipleChanges() {

        UnixShellAndFsUtilsMock unixShellAndFsUtils = new UnixShellAndFsUtilsMock();
        unixShellAndFsUtils.setExecuteCommandQuietAndGetOutputCallback((actionName, actionDetails, command, arguments) -> { //

            if ("ps".equals(actionDetails)) {
                return "11111111111111\tapp1_mysql\t2018-06-25 07:14:58 -0400 EDT\t2 weeks ago\tUp\t0B (virtual 407MB)" //
                        + "\n222222222222\tapp1\t2018-06-25 07:14:58 -0400 EDT\t2 weeks ago\tUp\t0B (virtual 407MB)" //
                        + "\n333333333\tinfra_redirector_exit\t2018-06-25 07:14:58 -0400 EDT\t2 weeks ago\tUp\t0B (virtual 407MB)" //
                        + "\n444444\tinfra_redirector_entry\t2018-06-25 07:14:58 -0400 EDT\t2 weeks ago\tUp\t0B (virtual 407MB)";
            }

            throw new RuntimeException("Mock: Not implemented");
        });
        DockerUtils dockerUtils = new DockerUtilsImpl(unixShellAndFsUtils);

        DockerState dockerState = new DockerState();
        String baseOutputDirectory = Files.createTempDir().getAbsolutePath();
        ContainersManageContext containersManageContext = new ContainersManageContext().setDockerState(dockerState).setBaseOutputDirectory(baseOutputDirectory);

        // Add initial applications: app1 and app1_mysql
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
        containersManageContext = new ContainersManageContext().setDockerState(dockerState).setBaseOutputDirectory(baseOutputDirectory);

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

        updatedInstanceNames = dockerUtils.containersManage(containersManageContext);
        assertList(Arrays.asList(), updatedInstanceNames);

        // Update and Execute (should change)
        containersManageContext = new ContainersManageContext().setDockerState(dockerState).setBaseOutputDirectory(baseOutputDirectory);
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

        updatedInstanceNames = dockerUtils.containersManage(containersManageContext);
        assertList(Arrays.asList("infra_redirector_entry", "app1", "infra_redirector_exit"), updatedInstanceNames);

        // Executing a second time (should not change anything)
        containersManageContext = new ContainersManageContext().setDockerState(dockerState).setBaseOutputDirectory(baseOutputDirectory);
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

        updatedInstanceNames = dockerUtils.containersManage(containersManageContext);
        assertList(Arrays.asList(), updatedInstanceNames);

    }

    @Test
    public void testConvertToDockerPs() {
        assertDockerPs("DockerUtilsImplTest-testConvertToDockerPs-nothing-expected.json", "DockerUtilsImplTest-testConvertToDockerPs-nothing.txt");
        assertDockerPs("DockerUtilsImplTest-testConvertToDockerPs-some-expected.json", "DockerUtilsImplTest-testConvertToDockerPs-some.txt");
        assertDockerPs("DockerUtilsImplTest-testConvertToDockerPs-incompleteStream-expected.json", "DockerUtilsImplTest-testConvertToDockerPs-incompleteStream.txt");
        assertDockerPs("DockerUtilsImplTest-testConvertToDockerPs-sizeWithExponent-expected.json", "DockerUtilsImplTest-testConvertToDockerPs-sizeWithExponent.txt");
    }

    @Test
    public void testNetworkListIpByContainerName() {
        Map<String, String> actual = new DockerUtilsImpl()
                .networkListIpByContainerNameConvert(ResourceTools.getResourceAsString("DockerUtilsImplTest-testNetworkListIpByContainerName.txt", getClass()));
        Map<String, String> expected = new TreeMap<>();
        expected.put("backup_sftp-l3_m_foilen-lab_com", "172.20.5.1");
        expected.put("test_mariadb", "172.20.5.7");
        expected.put("infra_web-l3_m_foilen-lab_com", "172.20.5.17");
        expected.put("testing_www", "172.20.5.15");
        expected.put("infra_redirector_exit", "172.20.5.16");

        AssertTools.assertJsonComparison(expected, actual);
    }

}
