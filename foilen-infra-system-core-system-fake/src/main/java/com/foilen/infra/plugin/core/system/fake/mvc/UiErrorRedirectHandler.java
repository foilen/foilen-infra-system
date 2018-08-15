/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.mvc;

import org.springframework.web.servlet.ModelAndView;

public interface UiErrorRedirectHandler {

    void execute(UiSuccessErrorView uiSuccessErrorView, ModelAndView modelAndView);

}