/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.common;

import com.foilen.infra.plugin.v1.core.base.resources.WebsiteCertificate;
import com.foilen.smalltools.crypt.spongycastle.asymmetric.RSACrypt;
import com.foilen.smalltools.crypt.spongycastle.cert.RSACertificate;

public class CertificateHelper {

    public static RSACertificate toRSACertificate(WebsiteCertificate websiteCertificate) {
        return RSACertificate.loadPemFromString( //
                websiteCertificate.getCertificate(), //
                websiteCertificate.getPrivateKey(), //
                websiteCertificate.getPublicKey() //
        );
    }

    public static WebsiteCertificate toWebsiteCertificate(String caCertificate, RSACertificate rsaCertificate) {
        WebsiteCertificate websiteCertificate = new WebsiteCertificate();
        toWebsiteCertificate(caCertificate, rsaCertificate, websiteCertificate);
        return websiteCertificate;
    }

    public static void toWebsiteCertificate(String caCertificate, RSACertificate rsaCertificate, WebsiteCertificate websiteCertificate) {
        websiteCertificate.setCaCertificate(caCertificate);
        websiteCertificate.setThumbprint(rsaCertificate.getThumbprint());
        websiteCertificate.setCertificate(rsaCertificate.saveCertificatePemAsString());
        websiteCertificate.setPublicKey(RSACrypt.RSA_CRYPT.savePublicKeyPemAsString(rsaCertificate.getKeysForSigning()));
        websiteCertificate.setPrivateKey(RSACrypt.RSA_CRYPT.savePrivateKeyPemAsString(rsaCertificate.getKeysForSigning()));
        websiteCertificate.setStart(rsaCertificate.getStartDate());
        websiteCertificate.setEnd(rsaCertificate.getEndDate());
        websiteCertificate.setDomainNames(rsaCertificate.getSubjectAltNames());
    }

}
