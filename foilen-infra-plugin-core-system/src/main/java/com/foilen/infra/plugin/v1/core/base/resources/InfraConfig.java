/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.resources;

import com.foilen.infra.plugin.v1.model.resource.AbstractIPResource;
import com.foilen.infra.plugin.v1.model.resource.InfraPluginResourceCategory;

/**
 * This is for managing the basic infrastructure resources: Login and UI. <br>
 * Links to:
 * <ul>
 * <li>{@link WebsiteCertificate}: (optional / 1) LOGIN_USES - The certificate.</li>
 * <li>{@link Machine}: (many) LOGIN_INSTALLED_ON - The machine to install the login service on.</li>
 * <li>{@link MariaDBServer}: (1) LOGIN_USES - The database to use.</li>
 * <li>{@link MariaDBDatabase}: (1) LOGIN_USES - The database to use.</li>
 * <li>{@link MariaDBUser}: (1) LOGIN_USES - The database user to use.</li>
 * <li>{@link UnixUser}: (1) LOGIN_USES - The unix user to RUN_AS.</li>
 * <li>{@link WebsiteCertificate}: (optional / 1) UI_USES - The certificate.</li>
 * <li>{@link Machine}: (many) UI_INSTALLED_ON - The machine to install the ui service on.</li>
 * <li>{@link MariaDBServer}: (1) UI_USES - The database to use.</li>
 * <li>{@link MariaDBDatabase}: (1) UI_USES - The database to use.</li>
 * <li>{@link MariaDBUser}: (1) UI_USES - The database user to use.</li>
 * <li>{@link UnixUser}: (1) UI_USES - The unix user to RUN_AS.</li>
 * </ul>
 *
 * Manages:
 * <ul>
 * <li>{@link Application}: The Login application that will automatically point to the {@link Machine}s on which it is LOGIN_INSTALLED_ON</li>
 * <li>{@link Website}: The Login website</li>
 * <li>{@link Application}: The UI application that will automatically point to the {@link Machine}s on which it is UI_INSTALLED_ON</li>
 * <li>{@link Website}: The UI website</li>
 * </ul>
 */
public class InfraConfig extends AbstractIPResource {

    public static final String LINK_TYPE_LOGIN_INSTALLED_ON = "LOGIN_INSTALLED_ON";
    public static final String LINK_TYPE_LOGIN_USES = "LOGIN_USES";

    public static final String LINK_TYPE_UI_INSTALLED_ON = "UI_INSTALLED_ON";
    public static final String LINK_TYPE_UI_USES = "UI_USES";

    public static final String PROPERTY_APPLICATION_ID = "applicationId";

    public static final String PROPERTY_LOGIN_DOMAIN_NAME = "loginDomainName";
    public static final String PROPERTY_LOGIN_EMAIL_FROM = "loginEmailFrom";
    public static final String PROPERTY_LOGIN_ADMINISTRATOR_EMAIL = "loginAdministratorEmail";
    public static final String PROPERTY_LOGIN_CSRF_SALT = "loginCsrfSalt";
    public static final String PROPERTY_LOGIN_COOKIE_SIGNATURE_SALT = "loginCookieSignatureSalt";

    public static final String PROPERTY_UI_DOMAIN_NAME = "uiDomainName";
    public static final String PROPERTY_UI_EMAIL_FROM = "uiEmailFrom";
    public static final String PROPERTY_UI_ALERTS_TO_EMAIL = "uiAlertsToEmail";
    public static final String PROPERTY_UI_CSRF_SALT = "uiCsrfSalt";
    public static final String PROPERTY_UI_LOGIN_COOKIE_SIGNATURE_SALT = "uiLoginCookieSignatureSalt";

    // Common
    private String applicationId;

    // Login
    private String loginDomainName;
    private String loginEmailFrom;
    private String loginAdministratorEmail;
    private String loginCsrfSalt;
    private String loginCookieSignatureSalt;

    // UI
    private String uiDomainName;
    private String uiEmailFrom;
    private String uiAlertsToEmail;
    private String uiCsrfSalt;
    private String uiLoginCookieSignatureSalt;

    public InfraConfig() {
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getLoginAdministratorEmail() {
        return loginAdministratorEmail;
    }

    public String getLoginCookieSignatureSalt() {
        return loginCookieSignatureSalt;
    }

    public String getLoginCsrfSalt() {
        return loginCsrfSalt;
    }

    public String getLoginDomainName() {
        return loginDomainName;
    }

    public String getLoginEmailFrom() {
        return loginEmailFrom;
    }

    @Override
    public InfraPluginResourceCategory getResourceCategory() {
        return InfraPluginResourceCategory.INFRASTRUCTURE;
    }

    @Override
    public String getResourceDescription() {
        return "Basic Infrastructure (login and UI)";
    }

    @Override
    public String getResourceName() {
        return "Infrastructure Configuration";
    }

    public String getUiAlertsToEmail() {
        return uiAlertsToEmail;
    }

    public String getUiCsrfSalt() {
        return uiCsrfSalt;
    }

    public String getUiDomainName() {
        return uiDomainName;
    }

    public String getUiEmailFrom() {
        return uiEmailFrom;
    }

    public String getUiLoginCookieSignatureSalt() {
        return uiLoginCookieSignatureSalt;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public void setLoginAdministratorEmail(String loginAdministratorEmail) {
        this.loginAdministratorEmail = loginAdministratorEmail;
    }

    public void setLoginCookieSignatureSalt(String loginCookieSignatureSalt) {
        this.loginCookieSignatureSalt = loginCookieSignatureSalt;
    }

    public void setLoginCsrfSalt(String loginCsrfSalt) {
        this.loginCsrfSalt = loginCsrfSalt;
    }

    public void setLoginDomainName(String loginDomainName) {
        this.loginDomainName = loginDomainName;
    }

    public void setLoginEmailFrom(String loginEmailFrom) {
        this.loginEmailFrom = loginEmailFrom;
    }

    public void setUiAlertsToEmail(String uiAlertsToEmail) {
        this.uiAlertsToEmail = uiAlertsToEmail;
    }

    public void setUiCsrfSalt(String uiCsrfSalt) {
        this.uiCsrfSalt = uiCsrfSalt;
    }

    public void setUiDomainName(String uiDomainName) {
        this.uiDomainName = uiDomainName;
    }

    public void setUiEmailFrom(String uiEmailFrom) {
        this.uiEmailFrom = uiEmailFrom;
    }

    public void setUiLoginCookieSignatureSalt(String uiLoginCookieSignatureSalt) {
        this.uiLoginCookieSignatureSalt = uiLoginCookieSignatureSalt;
    }

}
