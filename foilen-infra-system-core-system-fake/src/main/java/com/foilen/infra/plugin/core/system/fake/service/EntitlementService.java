/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.core.system.fake.service;

public interface EntitlementService {

    void canDeleteResourcesOrFailUi(String username);

    void canUpdateResourcesOrFailUi(String username);

    void isAdminOrFailUi(String username);

}
