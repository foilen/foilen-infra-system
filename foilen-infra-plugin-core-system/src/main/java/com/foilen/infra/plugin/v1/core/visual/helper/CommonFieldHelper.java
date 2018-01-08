/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CommonFieldHelper {

    public static Set<String> fromFormListToSet(Map<String, String> validFormValues, String fieldName) {
        return validFormValues.entrySet().stream() //
                .filter(it -> it.getKey().startsWith(fieldName + "[")) //
                .map(it -> it.getValue()) //
                .collect(Collectors.toSet());
    }

    public static List<String> fromSetToList(Set<String> values) {
        if (values == null) {
            return null;
        }
        List<String> results = new ArrayList<>(values.size());
        results.addAll(values);
        Collections.sort(results);
        return results;
    }

    public static List<String> getAllFieldNames(Map<String, String> formValues, String[] fieldNames) {
        List<String> allFieldNames = new ArrayList<>();
        for (String fieldName : fieldNames) {

            AtomicBoolean gotList = new AtomicBoolean(false);
            formValues.keySet().stream() //
                    .filter(it -> it.startsWith(fieldName + "[")) //
                    .forEach(it -> {
                        allFieldNames.add(it);
                        gotList.set(true);
                    });

            if (!gotList.get()) {
                allFieldNames.add(fieldName);
            }
        }
        return allFieldNames;
    }

    private CommonFieldHelper() {
    }
}
