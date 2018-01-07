/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.helper;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class CommonFormatting {

    public static void emptyIfNull(Map<String, String> formValues, String... fieldNames) {
        for (String fieldName : CommonFieldHelper.getAllFieldNames(formValues, fieldNames)) {
            if (formValues.get(fieldName) == null) {
                formValues.put(fieldName, "");
            }
        }
    }

    public static void firstLetterOfEachWordCapital(Map<String, String> formValues) {
        Set<String> fieldNames = formValues.keySet();
        firstLetterOfEachWordCapital(formValues, fieldNames.toArray(new String[fieldNames.size()]));
    }

    public static void firstLetterOfEachWordCapital(Map<String, String> formValues, String... fieldNames) {
        for (String fieldName : CommonFieldHelper.getAllFieldNames(formValues, fieldNames)) {
            String fieldValue = formValues.get(fieldName);
            if (fieldValue != null) {
                fieldValue = fieldValue.toLowerCase();
                StringBuilder sb = new StringBuilder();
                boolean nextUpper = true;
                for (int i = 0; i < fieldValue.length(); ++i) {
                    char ch = fieldValue.charAt(i);
                    if (nextUpper) {
                        nextUpper = false;
                        sb.append(String.valueOf(ch).toUpperCase());
                    } else {
                        sb.append(ch);
                    }
                    if (ch == ' ' || ch == ',') {
                        nextUpper = true;
                    }
                }
                formValues.put(fieldName, sb.toString());
            }
        }
    }

    public static void toLowerCase(Map<String, String> formValues) {
        Set<String> fieldNames = formValues.keySet();
        toLowerCase(formValues, fieldNames.toArray(new String[fieldNames.size()]));
    }

    public static void toLowerCase(Map<String, String> formValues, String... fieldNames) {
        for (String fieldName : CommonFieldHelper.getAllFieldNames(formValues, fieldNames)) {
            String fieldValue = formValues.get(fieldName);
            if (fieldValue != null) {
                formValues.put(fieldName, fieldValue.toLowerCase());
            }
        }
    }

    public static void toUpperCase(Map<String, String> formValues, String... fieldNames) {
        for (String fieldName : CommonFieldHelper.getAllFieldNames(formValues, fieldNames)) {
            String fieldValue = formValues.get(fieldName);
            if (fieldValue != null) {
                formValues.put(fieldName, fieldValue.toUpperCase());
            }
        }
    }

    public static void trimSpaces(Map<String, String> formValues) {
        Set<String> fieldNames = formValues.keySet();
        trimSpaces(formValues, fieldNames.toArray(new String[fieldNames.size()]));
    }

    public static void trimSpaces(Map<String, String> formValues, String... fieldNames) {
        for (String fieldName : CommonFieldHelper.getAllFieldNames(formValues, fieldNames)) {
            String fieldValue = formValues.get(fieldName);
            if (fieldValue != null) {
                fieldValue = fieldValue.replaceAll("[ \t\n\r]", "");
                formValues.put(fieldName, fieldValue);
            }
        }
    }

    public static void trimSpacesAround(Map<String, String> formValues) {
        Set<String> fieldNames = formValues.keySet();
        trimSpacesAround(formValues, fieldNames.toArray(new String[fieldNames.size()]));
    }

    public static void trimSpacesAround(Map<String, String> formValues, String... fieldNames) {
        for (String fieldName : CommonFieldHelper.getAllFieldNames(formValues, fieldNames)) {
            String fieldValue = formValues.get(fieldName);
            if (fieldValue != null) {
                fieldValue = StringUtils.trim(fieldValue);
                formValues.put(fieldName, fieldValue);
            }
        }
    }

    private CommonFormatting() {
    }
}
