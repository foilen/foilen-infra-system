/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.context;

import com.foilen.infra.plugin.v1.core.eventhandler.UpdateEventHandler;
import com.foilen.smalltools.tools.AbstractBasics;

public class UpdateEventContext extends AbstractBasics {

    private UpdateEventHandler<?> updateEventHandler;
    private String updateHandlerName;

    public UpdateEventContext(UpdateEventHandler<?> updateEventHandler, String updateHandlerName) {
        this.updateEventHandler = updateEventHandler;
        this.updateHandlerName = updateHandlerName;
    }

    public UpdateEventHandler<?> getUpdateEventHandler() {
        return updateEventHandler;
    }

    public String getUpdateHandlerName() {
        return updateHandlerName;
    }

    public void setUpdateEventHandler(UpdateEventHandler<?> updateEventHandler) {
        this.updateEventHandler = updateEventHandler;
    }

    public void setUpdateHandlerName(String updateHandlerName) {
        this.updateHandlerName = updateHandlerName;
    }

}
