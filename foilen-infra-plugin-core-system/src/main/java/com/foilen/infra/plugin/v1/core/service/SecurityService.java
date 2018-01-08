/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.service;

/**
 * To manage some security features.
 */
public interface SecurityService {

    /**
     * Tell which input name is the CSRF (nonce) token.
     *
     * @return the input name (e.g: _csrf)
     */
    String getCsrfParameterName();

    /**
     * Get the CSRF (nonce) token for the specified request.
     *
     * @param request
     *            the request (most likely a HttpServletRequest)
     * @return the generated csrf token for that request
     */
    String getCsrfValue(Object request);

}
