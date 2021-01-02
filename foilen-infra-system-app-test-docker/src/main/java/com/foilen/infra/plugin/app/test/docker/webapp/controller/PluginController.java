/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.app.test.docker.webapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.foilen.infra.plugin.v1.core.service.IPPluginService;

@Controller
@RequestMapping("plugin")
public class PluginController {

    @Autowired
    private IPPluginService ipPluginService;

    @RequestMapping("list")
    public ModelAndView list() {
        ModelAndView modelAndView = new ModelAndView("plugin/list");
        modelAndView.addObject("availables", ipPluginService.getAvailablePlugins());
        modelAndView.addObject("brokens", ipPluginService.getBrokenPlugins());
        return modelAndView;
    }

}
