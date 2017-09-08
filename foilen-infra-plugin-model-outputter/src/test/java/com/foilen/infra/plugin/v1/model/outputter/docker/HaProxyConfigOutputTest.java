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

import com.foilen.infra.plugin.v1.model.haproxy.HaProxyConfig;
import com.foilen.infra.plugin.v1.model.haproxy.HaProxyConfigPortHttp;
import com.foilen.infra.plugin.v1.model.haproxy.HaProxyConfigPortHttpService;
import com.foilen.infra.plugin.v1.model.haproxy.HaProxyConfigPortHttps;
import com.foilen.infra.plugin.v1.model.haproxy.HaProxyConfigPortTcp;
import com.foilen.infra.plugin.v1.model.outputter.haproxy.HaProxyConfigOutput;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.ResourceTools;
import com.foilen.smalltools.tuple.Tuple2;

public class HaProxyConfigOutputTest {

    private HaProxyConfig haProxyConfig;

    private HaProxyConfigPortTcp tcp3306;
    private HaProxyConfigPortHttp http;
    private HaProxyConfigPortHttps https;

    @Before
    public void init() {
        haProxyConfig = new HaProxyConfig();
        haProxyConfig.setDaemon(true);
        haProxyConfig.setChroot("/var/lib/haproxy");
        haProxyConfig.setPidfile("/_infra/haproxy.pid");
        tcp3306 = haProxyConfig.addPortTcp(3306, new Tuple2<>("172.17.0.4", 12003));
        tcp3306.setBindHost("127.0.0.1");

        http = haProxyConfig.addPortHttp(80);
        https = haProxyConfig.addPortHttps(443, "/certificates");

        http.setDefaultService(new HaProxyConfigPortHttpService(new Tuple2<>("172.17.0.5", 5009)));
        http.addService("dev.test.com", new Tuple2<>("172.17.0.6", 6009));
        http.addService("mysql.test.com", new Tuple2<>("172.17.0.8", 8009));
        http.addService("test.test.com", new Tuple2<>("172.17.0.7", 7009));

        https.setDefaultService(new HaProxyConfigPortHttpService(new Tuple2<>("172.17.0.5", 5009)));
        https.addService("dev.test.com", new Tuple2<>("172.17.0.6", 6009));
        https.addService("mysql.test.com", new Tuple2<>("172.17.0.8", 8009));
        https.addService("test.test.com", new Tuple2<>("172.17.0.7", 7009));
    }

    @Test
    public void testToConfigFile() {
        String actual = HaProxyConfigOutput.toConfigFile(haProxyConfig);
        String expected = ResourceTools.getResourceAsString("HaProxyConfigOutputTest-testToConfigFile-expected.txt", this.getClass());
        AssertTools.assertIgnoreLineFeed(expected, actual);
    }

    @Test
    public void testToConfigFile_NoHttpBack() {
        // Remove backend http_80_mysql.test.com 172.17.0.8:8009
        http.getServiceByHostname().get("mysql.test.com").getEndpointHostPorts().clear();

        String actual = HaProxyConfigOutput.toConfigFile(haProxyConfig);
        String expected = ResourceTools.getResourceAsString("HaProxyConfigOutputTest-testToConfigFile_NoHttpBack-expected.txt", this.getClass());
        AssertTools.assertIgnoreLineFeed(expected, actual);
    }

    @Test
    public void testToConfigFile_NoHttps() {
        haProxyConfig = new HaProxyConfig();
        haProxyConfig.setDaemon(true);
        haProxyConfig.setChroot("/var/lib/haproxy");
        haProxyConfig.setPidfile("/_infra/haproxy.pid");

        http = haProxyConfig.addPortHttp(80);
        https = haProxyConfig.addPortHttps(443, "/certificates");

        http.setDefaultService(new HaProxyConfigPortHttpService(new Tuple2<>("172.17.0.5", 5009)));
        http.addService("dev.test.com", new Tuple2<>("172.17.0.6", 6009));
        http.addService("mysql.test.com", new Tuple2<>("172.17.0.8", 8009));
        http.addService("test.test.com", new Tuple2<>("172.17.0.7", 7009));

        String actual = HaProxyConfigOutput.toConfigFile(haProxyConfig);
        String expected = ResourceTools.getResourceAsString("HaProxyConfigOutputTest-testToConfigFile_NoHttps-expected.txt", this.getClass());
        AssertTools.assertIgnoreLineFeed(expected, actual);
    }

    @Test
    public void testToConfigFile_NoHttpsBack() {
        // Remove backend https_443_mysql.test.com 172.17.0.8:8009
        https.getServiceByHostname().get("mysql.test.com").getEndpointHostPorts().clear();

        String actual = HaProxyConfigOutput.toConfigFile(haProxyConfig);
        String expected = ResourceTools.getResourceAsString("HaProxyConfigOutputTest-testToConfigFile_NoHttpsBack-expected.txt", this.getClass());
        AssertTools.assertIgnoreLineFeed(expected, actual);
    }

    @Test
    public void testToConfigFile_NoTcpBack() {
        // Remove backend port_3306 172.17.0.4:12003
        tcp3306.getEndpointHostPorts().clear();

        String actual = HaProxyConfigOutput.toConfigFile(haProxyConfig);
        String expected = ResourceTools.getResourceAsString("HaProxyConfigOutputTest-testToConfigFile_NoTcpBack-expected.txt", this.getClass());
        AssertTools.assertIgnoreLineFeed(expected, actual);
    }

    @Test
    public void testToConfigFileWithNulls() {

        haProxyConfig = new HaProxyConfig();
        haProxyConfig.setDaemon(true);
        haProxyConfig.setChroot("/var/lib/haproxy");
        haProxyConfig.setPidfile("/_infra/haproxy.pid");
        HaProxyConfigPortTcp tcp = haProxyConfig.addPortTcp(3306, new Tuple2<>(null, 12003));
        tcp.setBindHost("127.0.0.1");

        HaProxyConfigPortHttp http = haProxyConfig.addPortHttp(80);
        HaProxyConfigPortHttps https = haProxyConfig.addPortHttps(443, "/certificates");

        http.setDefaultService(new HaProxyConfigPortHttpService(new Tuple2<>(null, 5009)));
        http.addService("dev.test.com", new Tuple2<>(null, 6009));
        http.addService("mysql.test.com", new Tuple2<>(null, 8009));
        http.addService("test.test.com", new Tuple2<>(null, 7009));

        https.setDefaultService(http.getDefaultService());
        https.setServiceByHostname(http.getServiceByHostname());

        String actual = HaProxyConfigOutput.toConfigFile(haProxyConfig);
        String expected = ResourceTools.getResourceAsString("HaProxyConfigOutputTest-testToConfigFileWithNulls-expected.txt", this.getClass());
        AssertTools.assertIgnoreLineFeed(expected, actual);
    }

    @Test
    public void testToRun() {
        String actual = HaProxyConfigOutput.toRun(haProxyConfig, "/_infra/haproxy.cfg");
        String expected = "/usr/sbin/haproxy -f /_infra/haproxy.cfg -p $(</_infra/haproxy.pid) -st $(</_infra/haproxy.pid)";
        Assert.assertEquals(expected, actual);
    }

}
