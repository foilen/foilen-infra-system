/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.service;

/**
 * To send alerting.
 */
public interface MessagingService {

    void alertingError(String shortDescription, String longDescription);

    void alertingInfo(String shortDescription, String longDescription);

    void alertingWarn(String shortDescription, String longDescription);

}
