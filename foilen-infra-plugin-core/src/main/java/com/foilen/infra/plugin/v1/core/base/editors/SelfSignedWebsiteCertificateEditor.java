/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.editors;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.foilen.infra.plugin.v1.core.visual.pageItem.field.InputTextFieldPageItem;
import com.foilen.smalltools.crypt.asymmetric.AsymmetricKeys;
import com.foilen.smalltools.crypt.asymmetric.RSACrypt;
import com.foilen.smalltools.crypt.cert.CertificateDetails;
import com.foilen.smalltools.crypt.cert.RSACertificate;
import com.foilen.smalltools.tools.DateTools;
import com.foilen.smalltools.tuple.Tuple2;

public class SelfSignedWebsiteCertificateEditor implements ResourceEditor<WebsiteCertificate> {

    public static final String EDITOR_NAME = "Self-signed WebsiteCertificate";

    private static final String FIELD_NAME_DOMAIN = "domain";

    @Override
    public void fillResource(CommonServicesContext servicesCtx, ChangesContext changesContext, Map<String, String> validFormValues, WebsiteCertificate resource) {

        String domain = validFormValues.get(FIELD_NAME_DOMAIN);

        boolean gen = resource.getInternalId() == null;
        // Not gen
        gen |= resource.getCertificate() == null;
        // Expired
        if (resource.getEnd() == null) {
            gen = true;
        } else {
            gen |= resource.getEnd().getTime() < System.currentTimeMillis();
        }
        // Not the same domain
        Optional<String> currentDomainOptional = resource.getDomainNames().stream().findFirst();
        if (currentDomainOptional.isPresent()) {
            gen |= !currentDomainOptional.get().equals(domain);
        } else {
            gen = true;
        }

        // Generate if needed
        if (gen) {
            AsymmetricKeys keys = RSACrypt.RSA_CRYPT.generateKeyPair(4096);
            RSACertificate rsaCertificate = new RSACertificate(keys).selfSign( //
                    new CertificateDetails().setCommonName(domain) //
                            .addSanDns(domain) //
                            .setEndDate(DateTools.addDate(Calendar.MONTH, 1)));
            CertificateHelper.toWebsiteCertificate(null, rsaCertificate, resource);
        }

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

        PageDefinition pageDefinition = new PageDefinition(translationService.translate("SelfSignedWebsiteCertificateEditor.title"));

        InputTextFieldPageItem domainPageItem = CommonPageItem.createInputTextField(servicesCtx, pageDefinition, "SelfSignedWebsiteCertificateEditor.domain", FIELD_NAME_DOMAIN);

        if (resource != null) {

            pageDefinition.addPageItem(new LabelPageItem().setText( //
                    translationService.translate("SelfSignedWebsiteCertificateEditor.thumbprint", resource.getThumbprint()) //
            ));

            pageDefinition.addPageItem(new LabelPageItem().setText( //
                    translationService.translate("SelfSignedWebsiteCertificateEditor.start", DateTools.formatFull(resource.getStart())) //
            ));

            pageDefinition.addPageItem(new LabelPageItem().setText( //
                    translationService.translate("SelfSignedWebsiteCertificateEditor.end", DateTools.formatFull(resource.getEnd())) //
            ));

            Optional<String> domain = resource.getDomainNames().stream().findFirst();
            domainPageItem.setFieldValue(domain.isPresent() ? domain.get() : null);

        }

        return pageDefinition;

    }

    @Override
    public List<Tuple2<String, String>> validateForm(CommonServicesContext servicesCtx, Map<String, String> rawFormValues) {
        List<Tuple2<String, String>> errors = CommonValidation.validateNotNullOrEmpty(rawFormValues, FIELD_NAME_DOMAIN);
        errors.addAll(CommonValidation.validateDomainName(rawFormValues, FIELD_NAME_DOMAIN));
        return errors;

    }

}
