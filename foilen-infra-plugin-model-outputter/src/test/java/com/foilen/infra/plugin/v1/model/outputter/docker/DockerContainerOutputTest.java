/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.outputter.docker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.docker.DockerContainerEndpoints;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.ResourceTools;
import com.google.common.base.Joiner;

public class DockerContainerOutputTest {

    private static final Joiner joiner = Joiner.on(" ");

    private IPApplicationDefinition applicationDefinition;
    private DockerContainerOutputContext ctx;

    @Before
    public void init() {
        applicationDefinition = new IPApplicationDefinition();
        applicationDefinition.setFrom("ubuntu:16.04");
        applicationDefinition.addBuildStepCommand("export TERM=dumb ; apt-get update && apt-get install -y haproxy && apt-get clean && rm -rf /var/lib/apt/lists/*");
        applicationDefinition.addBuildStepCopy("asset/a.zip", "/tmp/a.zip");
        applicationDefinition.addBuildStepCommand("unzip /tmp/a.zip");
        applicationDefinition.addBuildStepCopy("asset/adir", "/asserts/adir");
        applicationDefinition.addContainerUserToChangeId("containerUser1", 1000);
        applicationDefinition.addContainerUserToChangeId("containerUser2", 1000);
        applicationDefinition.addVolume("/tmp/docker/config", "/volumes/config");
        applicationDefinition.addVolume("/tmp/docker/etc", "/volumes/etc");
        applicationDefinition.addPortExposed(80, 8080);
        applicationDefinition.addPortExposed(443, 8443);
        applicationDefinition.addPortRedirect(3306, "d001.node.example.com", "mysql01.db.example.com", DockerContainerEndpoints.MYSQL_TCP);
        applicationDefinition.addPortEndpoint(8080, "HTTP");
        applicationDefinition.setRunAs(10001);
        applicationDefinition.setCommand("/usr/sbin/haproxy -f /volumes/config/haproxy");

        ctx = new DockerContainerOutputContext("Uroot_Stest", "Uroot_Stest", "Uroot_Stest");

    }

    @Test
    public void testSanitize() {
        Assert.assertEquals("/tmp/space", DockerContainerOutput.sanitize("/tmp/space"));
        Assert.assertEquals("/tmp/l\\'ecole", DockerContainerOutput.sanitize("/tmp/l'ecole"));
        Assert.assertEquals("/tmp/l\\\"ecole", DockerContainerOutput.sanitize("/tmp/l\"ecole"));
    }

    @Test
    public void testToDockerfile() {
        String actual = DockerContainerOutput.toDockerfile(applicationDefinition, ctx);
        String expected = ResourceTools.getResourceAsString("DockerContainerOutputTest-testToDockerfile-expected.txt", this.getClass());
        AssertTools.assertIgnoreLineFeed(expected, actual);
    }

    @Test
    public void testToDockerfile_NoInfra() {

        applicationDefinition.getPortsRedirect().clear();

        String actual = DockerContainerOutput.toDockerfile(applicationDefinition, ctx);
        String expected = ResourceTools.getResourceAsString("DockerContainerOutputTest-testToDockerfile_NoInfra-expected.txt", this.getClass());
        AssertTools.assertIgnoreLineFeed(expected, actual);
    }

    @Test
    public void testToRunCommandArgumentsSinglePassAttached() {
        applicationDefinition.setIp("172.20.0.10");
        String expected = "run -i --rm --volume /tmp/docker/config:/volumes/config --volume /tmp/docker/etc:/volumes/etc --publish 80:8080 --publish 443:8443 --ip 172.20.0.10 --net infra -u 10001 --name Uroot_Stest --hostname Uroot_Stest Uroot_Stest";
        String actual = joiner.join(DockerContainerOutput.toRunCommandArgumentsSinglePassAttached(applicationDefinition, ctx));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToRunCommandWithRestart() {
        String expected = "run --detach --restart always --volume /tmp/docker/config:/volumes/config --volume /tmp/docker/etc:/volumes/etc --publish 80:8080 --publish 443:8443 -u 10001 --name Uroot_Stest --hostname Uroot_Stest Uroot_Stest";
        String actual = joiner.join(DockerContainerOutput.toRunCommandArgumentsWithRestart(applicationDefinition, ctx));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToRunCommandWithRestart_NoInfra() {

        applicationDefinition.getPortsRedirect().clear();

        String expected = "run --detach --restart always --volume /tmp/docker/config:/volumes/config --volume /tmp/docker/etc:/volumes/etc --publish 80:8080 --publish 443:8443 -u 10001 --name Uroot_Stest --hostname Uroot_Stest Uroot_Stest";
        String actual = joiner.join(DockerContainerOutput.toRunCommandArgumentsWithRestart(applicationDefinition, ctx));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToRunCommandWithRestartAndIp() {
        applicationDefinition.setIp("172.20.0.10");
        String expected = "run --detach --restart always --volume /tmp/docker/config:/volumes/config --volume /tmp/docker/etc:/volumes/etc --publish 80:8080 --publish 443:8443 --ip 172.20.0.10 --net infra -u 10001 --name Uroot_Stest --hostname Uroot_Stest Uroot_Stest";
        String actual = joiner.join(DockerContainerOutput.toRunCommandArgumentsWithRestart(applicationDefinition, ctx));
        Assert.assertEquals(expected, actual);
    }

}
