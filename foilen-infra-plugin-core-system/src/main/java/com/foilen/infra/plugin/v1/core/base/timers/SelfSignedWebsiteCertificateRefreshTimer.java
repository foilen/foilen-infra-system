/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.timers;

import java.util.Calendar;
import java.util.List;

import com.foilen.infra.plugin.v1.core.base.editors.SelfSignedWebsiteCertificateEditor;
import com.foilen.infra.plugin.v1.core.base.resources.WebsiteCertificate;
import com.foilen.infra.plugin.v1.core.common.CertificateHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.context.TimerEventContext;
import com.foilen.infra.plugin.v1.core.eventhandler.TimerEventHandler;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.smalltools.crypt.spongycastle.asymmetric.AsymmetricKeys;
import com.foilen.smalltools.crypt.spongycastle.cert.CertificateDetails;
import com.foilen.smalltools.crypt.spongycastle.cert.RSACertificate;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.DateTools;

/**
 * Checks the certificates that will expire in 1 week and refresh them.
 */
public class SelfSignedWebsiteCertificateRefreshTimer extends AbstractBasics implements TimerEventHandler {

    public static final String TIMER_NAME = "SelfSignedWebsiteCertificateRefreshTimer";

    @Override
    public void timerHandler(CommonServicesContext services, ChangesContext changes, TimerEventContext event) {

        IPResourceService resourceService = services.getResourceService();

        // Check the certs that will expire in 1 week
        logger.info("Getting self-signed certificates that expire in 1 week");
        List<WebsiteCertificate> certificatesToUpdate = resourceService.resourceFindAll( //
                resourceService.createResourceQuery(WebsiteCertificate.class) //
                        .addEditorEquals(SelfSignedWebsiteCertificateEditor.EDITOR_NAME) //
                        .propertyLesserAndEquals(WebsiteCertificate.PROPERTY_END, DateTools.addDate(Calendar.WEEK_OF_YEAR, 1) //
                ));

        // Update them
        logger.info("Got {} certificates to update", certificatesToUpdate.size());
        for (WebsiteCertificate certificate : certificatesToUpdate) {
            logger.info("Updating certificate {}", certificate.getDomainNames());

            try {
                RSACertificate currentRsaCertificate = CertificateHelper.toRSACertificate(certificate);

                AsymmetricKeys keys = currentRsaCertificate.getKeysForSigning();
                RSACertificate rsaCertificate = new RSACertificate(keys).selfSign( //
                        new CertificateDetails().setCommonName(currentRsaCertificate.getCommonName()) //
                                .addSanDns(currentRsaCertificate.getCommonName()) //
                                .setEndDate(DateTools.addDate(Calendar.MONTH, 1)));
                CertificateHelper.toWebsiteCertificate(null, rsaCertificate, certificate);

                changes.resourceUpdate(certificate.getInternalId(), certificate);
            } catch (Exception e) {
                logger.error("Problem updating self-signed certificate {}", certificate.getDomainNames(), e);
                services.getMessagingService().alertingError("Problem updating self-signed certificate " + certificate.getDomainNames(), e.getMessage());
            }
        }

    }

}
