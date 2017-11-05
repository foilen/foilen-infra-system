/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.service;

import java.util.Locale;

/**
 * To manage timers.
 */
public interface TranslationService {

    String translate(Locale locale, String messageCode);

    String translate(Locale locale, String messageCode, Object... args);

    String translate(String messageCode);

    String translate(String messageCode, Object... args);

    void translationAdd(String basename);

}
