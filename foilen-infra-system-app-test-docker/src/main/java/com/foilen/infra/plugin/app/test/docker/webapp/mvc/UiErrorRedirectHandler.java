/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.app.test.docker.webapp.mvc;

import org.springframework.web.servlet.ModelAndView;

public interface UiErrorRedirectHandler {

    void execute(UiSuccessErrorView uiSuccessErrorView, ModelAndView modelAndView);

}
