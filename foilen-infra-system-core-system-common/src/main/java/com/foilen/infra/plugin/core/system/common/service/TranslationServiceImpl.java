/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.common.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import com.foilen.infra.plugin.v1.core.service.TranslationService;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.CharsetTools;

public class TranslationServiceImpl extends AbstractBasics implements TranslationService {

    @Autowired(required = false)
    private ReloadableResourceBundleMessageSource messageSource;

    public TranslationServiceImpl() {
        messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setDefaultEncoding(CharsetTools.UTF_8.name());
        messageSource.setUseCodeAsDefaultMessage(true);
    }

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
