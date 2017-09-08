/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.editors;

import java.util.List;
import java.util.Map;

import com.foilen.infra.plugin.v1.core.base.resources.WebsiteCertificate;
import com.foilen.infra.plugin.v1.core.common.CertificateHelper;
import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.service.TranslationService;
import com.foilen.infra.plugin.v1.core.visual.PageDefinition;
import com.foilen.infra.plugin.v1.core.visual.editor.ResourceEditor;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonFormatting;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonPageItem;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonValidation;
import com.foilen.infra.plugin.v1.core.visual.pageItem.LabelPageItem;
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.MultilineInputTextFieldPageItem;
import com.foilen.smalltools.crypt.asymmetric.RSACrypt;
import com.foilen.smalltools.crypt.cert.RSACertificate;
import com.foilen.smalltools.tools.DateTools;
import com.foilen.smalltools.tuple.Tuple2;

public class ManualWebsiteCertificateEditor implements ResourceEditor<WebsiteCertificate> {

    public static final String EDITOR_NAME = "Manual WebsiteCertificate";

    @Override
    public void fillResource(CommonServicesContext servicesCtx, ChangesContext changesContext, Map<String, String> validFormValues, WebsiteCertificate resource) {

        StringBuilder allPem = new StringBuilder();

        allPem.append(validFormValues.get(WebsiteCertificate.PROPERTY_CERTIFICATE)).append("\n");
        allPem.append(validFormValues.get(WebsiteCertificate.PROPERTY_PRIVATE_KEY)).append("\n");

        String value = validFormValues.get(WebsiteCertificate.PROPERTY_PUBLIC_KEY);
        if (value != null) {
            allPem.append(value);
        }

        RSACertificate rsaCertificate = RSACertificate.loadPemFromString(allPem.toString());
        CertificateHelper.toWebsiteCertificate(validFormValues.get(WebsiteCertificate.PROPERTY_CA_CERTIFICATE), rsaCertificate, resource);

    }

    @Override
    public void formatForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues) {
        CommonFormatting.trimSpacesAround(rawFormValues);
    }

    @Override
    public Class<WebsiteCertificate> getForResourceType() {
        return WebsiteCertificate.class;
    }

    @Override
    public PageDefinition providePageDefinition(CommonServicesContext servicesCtx, WebsiteCertificate resource) {

        TranslationService translationService = servicesCtx.getTranslationService();

        PageDefinition pageDefinition = new PageDefinition(translationService.translate("ManualWebsiteCertificateEditor.title"));

        MultilineInputTextFieldPageItem caCertificatePageItem = CommonPageItem.createMultilineInputTextField(servicesCtx, pageDefinition, "ManualWebsiteCertificateEditor.caCertificate",
                WebsiteCertificate.PROPERTY_CA_CERTIFICATE);
        MultilineInputTextFieldPageItem certificatePageItem = CommonPageItem.createMultilineInputTextField(servicesCtx, pageDefinition, "ManualWebsiteCertificateEditor.certificate",
                WebsiteCertificate.PROPERTY_CERTIFICATE);
        MultilineInputTextFieldPageItem publicKeyPageItem = CommonPageItem.createMultilineInputTextField(servicesCtx, pageDefinition, "ManualWebsiteCertificateEditor.publicKey",
                WebsiteCertificate.PROPERTY_PUBLIC_KEY);
        MultilineInputTextFieldPageItem privateKeyPageItem = CommonPageItem.createMultilineInputTextField(servicesCtx, pageDefinition, "ManualWebsiteCertificateEditor.privateKey",
                WebsiteCertificate.PROPERTY_PRIVATE_KEY);

        if (resource != null) {

            pageDefinition.addPageItem(new LabelPageItem().setText( //
                    translationService.translate("ManualWebsiteCertificateEditor.thumbprint", resource.getThumbprint()) //
            ));

            pageDefinition.addPageItem(new LabelPageItem().setText( //
                    translationService.translate("ManualWebsiteCertificateEditor.domainNames", resource.getDomainNames()) //
            ));

            pageDefinition.addPageItem(new LabelPageItem().setText( //
                    translationService.translate("ManualWebsiteCertificateEditor.start", DateTools.formatFull(resource.getStart())) //
            ));

            pageDefinition.addPageItem(new LabelPageItem().setText( //
                    translationService.translate("ManualWebsiteCertificateEditor.end", DateTools.formatFull(resource.getEnd())) //
            ));

            caCertificatePageItem.setFieldValue(resource.getCaCertificate());
            certificatePageItem.setFieldValue(resource.getCertificate());
            publicKeyPageItem.setFieldValue(resource.getPublicKey());
            privateKeyPageItem.setFieldValue(resource.getPrivateKey());
        }

        return pageDefinition;

    }

    @Override
    public List<Tuple2<String, String>> validateForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues) {

        List<Tuple2<String, String>> errors = CommonValidation.validateNotNullOrEmpty(rawFormValues, WebsiteCertificate.PROPERTY_CERTIFICATE, WebsiteCertificate.PROPERTY_PRIVATE_KEY);

        // Validate cert
        try {
            RSACertificate.loadPemFromString(rawFormValues.get(WebsiteCertificate.PROPERTY_CERTIFICATE));
        } catch (Exception e) {
            errors.add(new Tuple2<>(WebsiteCertificate.PROPERTY_CERTIFICATE, "error.cert.notCertificate"));
        }

        // Validate key
        try {
            RSACrypt.RSA_CRYPT.loadKeysPemFromString(rawFormValues.get(WebsiteCertificate.PROPERTY_PRIVATE_KEY));
        } catch (Exception e) {
            errors.add(new Tuple2<>(WebsiteCertificate.PROPERTY_PRIVATE_KEY, "error.cert.notKey"));
        }

        return errors;
    }

}
