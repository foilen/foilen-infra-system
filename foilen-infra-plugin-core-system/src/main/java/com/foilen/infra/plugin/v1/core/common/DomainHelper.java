/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.validator.routines.DomainValidator;

import com.google.common.base.Joiner;

public class DomainHelper {

    private static final Joiner dotJoiner = Joiner.on('.');

    public static boolean isValidDomainName(String domainName) {
        domainName = domainName.replaceAll("_", "");
        return DomainValidator.getInstance().isValid(domainName);
    }

    public static String parentDomainName(String domainName) {
        List<String> parts = Arrays.asList(domainName.split("\\."));
        if (parts.size() < 2) {
            return null;
        }
        Iterator<String> it = parts.iterator();
        it.next();
        String parentDomainName = dotJoiner.join(it);
        if (!isValidDomainName(parentDomainName)) {
            return null;
        }
        return parentDomainName;
    }

    public static String reverseDomainName(String domainName) {
        List<String> parts = Arrays.asList(domainName.split("\\."));
        Collections.reverse(parts);
        return dotJoiner.join(parts);
    }

}
