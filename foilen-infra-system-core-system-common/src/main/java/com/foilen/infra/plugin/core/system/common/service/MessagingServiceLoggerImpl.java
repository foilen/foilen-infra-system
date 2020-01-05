/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.common.service;

import com.foilen.infra.plugin.v1.core.service.MessagingService;
import com.foilen.smalltools.tools.AbstractBasics;

/**
 * It logs all the received alerts.
 */
public class MessagingServiceLoggerImpl extends AbstractBasics implements MessagingService {

    @Override
    public void alertingError(String shortDescription, String longDescription) {
        logger.error("ALERT [{}]: [{}]", shortDescription, longDescription);
    }

    @Override
    public void alertingInfo(String shortDescription, String longDescription) {
        logger.info("ALERT [{}]: [{}]", shortDescription, longDescription);
    }

    @Override
    public void alertingWarn(String shortDescription, String longDescription) {
        logger.warn("ALERT [{}]: [{}]", shortDescription, longDescription);
    }

}
