/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.resources;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.foilen.infra.plugin.v1.model.resource.AbstractIPResource;
import com.foilen.infra.plugin.v1.model.resource.InfraPluginResourceCategory;
import com.foilen.smalltools.tools.CollectionsTools;
import com.foilen.smalltools.tools.DateTools;
import com.google.common.base.Joiner;
import com.google.common.collect.ComparisonChain;

/**
 * This is a web certificate. <br/>
 * Links to:
 * <ul>
 * </ul>
 *
 * Manages:
 * <ul>
 * <li>{@link Domain}: Creates/uses a {@link Domain} to make sure it is owned by the user</li>
 * </ul>
 */
public class WebsiteCertificate extends AbstractIPResource implements Comparable<WebsiteCertificate> {

    public static final String PROPERTY_THUMBPRINT = "thumbprint";
    public static final String PROPERTY_DOMAIN_NAMES = "domainNames";
    public static final String PROPERTY_CA_CERTIFICATE = "caCertificate";
    public static final String PROPERTY_CERTIFICATE = "certificate";
    public static final String PROPERTY_PUBLIC_KEY = "publicKey";
    public static final String PROPERTY_PRIVATE_KEY = "privateKey";
    public static final String PROPERTY_START = "start";
    public static final String PROPERTY_END = "end";

    private String thumbprint;

    private Set<String> domainNames = new HashSet<>();

    private String caCertificate;
    private String certificate;

    private String publicKey;
    private String privateKey;

    private Date start;
    private Date end;

    public WebsiteCertificate() {
    }

    public WebsiteCertificate(String caCertificate, String thumbprint, String certificate, String publicKey, String privateKey, Date start, Date end, String... domainNames) {
        this.caCertificate = caCertificate;
        this.thumbprint = thumbprint;
        this.certificate = certificate;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.start = start;
        this.end = end;
        this.domainNames = Arrays.asList(domainNames).stream().collect(Collectors.toSet());
    }

    @Override
    public int compareTo(WebsiteCertificate o) {
        ComparisonChain cc = ComparisonChain.start();
        cc = cc.compare(thumbprint, o.thumbprint);
        return cc.result();
    }

    @Override
    public boolean equals(Object o) {

        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o.getClass() != getClass()) {
            return false;
        }

        WebsiteCertificate se = (WebsiteCertificate) o;
        return Objects.equals(thumbprint, se.thumbprint);
    }

    public String getCaCertificate() {
        return caCertificate;
    }

    public String getCertificate() {
        return certificate;
    }

    public Set<String> getDomainNames() {
        return domainNames;
    }

    public Date getEnd() {
        return end;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    @Override
    public InfraPluginResourceCategory getResourceCategory() {
        return InfraPluginResourceCategory.NET;
    }

    @JsonIgnore
    @Override
    public String getResourceDescription() {
        return Joiner.on(", ").join( //
                thumbprint, //
                DateTools.formatDateOnly(start), //
                DateTools.formatDateOnly(end) //
        );
    }

    @JsonIgnore
    @Override
    public String getResourceName() {
        return Joiner.on(", ").join(domainNames.stream().sorted().collect(Collectors.toList()));
    }

    public Date getStart() {
        return start;
    }

    public String getThumbprint() {
        return thumbprint;
    }

    @Override
    public int hashCode() {
        return Objects.hash(thumbprint);
    }

    public void setCaCertificate(String caCertificate) {
        this.caCertificate = caCertificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    @Deprecated
    public void setCommonNames(Set<String> commonNames) {
        if (CollectionsTools.isNullOrEmpty(this.domainNames)) {
            this.domainNames = commonNames;
        }
    }

    public void setDomainNames(Set<String> domainNames) {
        this.domainNames = domainNames;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public void setThumbprint(String thumbprint) {
        this.thumbprint = thumbprint;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WebsiteCertificate [thumbprint=");
        builder.append(thumbprint);
        builder.append(", domainNames=");
        builder.append(domainNames);
        builder.append(", caCertificate=");
        builder.append(caCertificate);
        builder.append(", certificate=");
        builder.append(certificate);
        builder.append(", publicKey=");
        builder.append(publicKey);
        builder.append(", privateKey=");
        builder.append(privateKey);
        builder.append(", start=");
        builder.append(start);
        builder.append(", end=");
        builder.append(end);
        builder.append("]");
        return builder.toString();
    }

}
