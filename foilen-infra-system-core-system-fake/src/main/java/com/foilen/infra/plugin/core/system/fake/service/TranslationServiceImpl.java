/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import com.foilen.infra.plugin.v1.core.service.TranslationService;
import com.foilen.smalltools.tools.AbstractBasics;

@Component
public class TranslationServiceImpl extends AbstractBasics implements TranslationService {

    @Autowired
    private ReloadableResourceBundleMessageSource messageSource;

    @Override
    public String translate(Locale locale, String messageCode) {
        return messageSource.getMessage(messageCode, null, locale);
    }

    @Override
    public String translate(Locale locale, String messageCode, Object... args) {
        return messageSource.getMessage(messageCode, args, locale);
    }

    @Override
    public String translate(String messageCode) {
        Locale locale = LocaleContextHolder.getLocale();
        return translate(locale, messageCode);
    }

    @Override
    public String translate(String messageCode, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return translate(locale, messageCode, args);
    }

    @Override
    public void translationAdd(String basename) {
        messageSource.addBasenames("classpath:" + basename);
    }

}
