/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.docker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DockerContainerEndpoints {

    public static final String HTTP_TCP = "HTTP_TCP";
    public static final String MYSQL_TCP = "MYSQL_TCP";
    public static final String SMTP_TCP = "SMTP_TCP";

    public static final List<String> allValues = Collections.unmodifiableList(Arrays.asList( //
            HTTP_TCP, //
            MYSQL_TCP, //
            SMTP_TCP //
    ));

    private DockerContainerEndpoints() {
    }

}
