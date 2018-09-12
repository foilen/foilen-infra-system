/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceChainRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.resource.GzipResourceResolver;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.resource.VersionResourceResolver;

import com.foilen.infra.plugin.core.system.common.service.IPPluginServiceImpl;
import com.foilen.infra.plugin.v1.core.service.IPPluginService;
import com.foilen.smalltools.spring.resourceresolver.BundleResourceResolver;
import com.foilen.smalltools.tools.CharsetTools;

@Configuration
public class ConfigWebUiConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**").addResourceLocations("classpath:/WEB-INF/webui/resources/images/");
        registry.addResourceHandler("/fonts/**").addResourceLocations("classpath:/WEB-INF/webui/resources/fonts/");

        ResourceChainRegistration chain = registry.addResourceHandler("/bundles/**") //
                .setCachePeriod(365 * 24 * 60 * 60) //
                .resourceChain(false) //
                .addResolver(new GzipResourceResolver()); //

        chain.addResolver(new VersionResourceResolver() //
                .addContentVersionStrategy("/**")) //
                .addResolver(new BundleResourceResolver() //
                        .setCache(false) //
                        .setGenerateGzip(true) //
                        .addBundleResource("all.css", "/META-INF/resources/webjars/bootstrap/3.3.7-1/css/bootstrap.css") //
                        .addBundleResource("all.css", "/META-INF/resources/webjars/bootstrap/3.3.7-1/css/bootstrap-theme.css") //
                        .addBundleResource("all.css", "/WEB-INF/webui/resources/css/infra.css") //
                        .addBundleResource("all.css", "/WEB-INF/webui/resources/css/glyphicons.css") //
                        .addBundleResource("all.css", "/WEB-INF/webui/resources/css/glyphicons-bootstrap.css") //
                        .addBundleResource("all.js", "/META-INF/resources/webjars/jquery/1.11.1/jquery.js") //
                        .addBundleResource("all.js", "/META-INF/resources/webjars/bootstrap/3.3.7-1/js/bootstrap.js") //
                        .addBundleResource("all.js", "/META-INF/resources/webjars/typeaheadjs/0.11.1/typeahead.jquery.js") //
                        .addBundleResource("all.js", "/WEB-INF/webui/resources/js/infra.js") //
                        .primeCache() //

                );

    }

    @Override
    public void configurePathMatch(PathMatchConfigurer matcher) {
        matcher.setUseRegisteredSuffixPatternMatch(true);
    }

    @Bean
    public ConversionService conversionService() {
        return new DefaultConversionService();
    }

    @Bean
    public IPPluginService ipPluginService() {
        return new IPPluginServiceImpl();
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }

    @Bean
    public CookieLocaleResolver localeResolver() {
        CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver();
        cookieLocaleResolver.setCookieMaxAge(2 * 7 * 24 * 60 * 60);// 2 weeks
        cookieLocaleResolver.setCookieName("lang");
        return cookieLocaleResolver;
    }

    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.addBasenames("classpath:/WEB-INF/webui/messages/messages");

        messageSource.setCacheSeconds(60);
        messageSource.setDefaultEncoding(CharsetTools.UTF_8.name());
        messageSource.setUseCodeAsDefaultMessage(true);

        return messageSource;
    }

    @Bean
    public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
        ResourceUrlEncodingFilter resourceUrlEncodingFilter = new ResourceUrlEncodingFilter();
        return resourceUrlEncodingFilter;
    }

}
