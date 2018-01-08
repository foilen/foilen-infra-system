/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.base.resources.helper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.foilen.infra.plugin.v1.core.base.resources.UnixUser;
import com.foilen.infra.plugin.v1.core.exception.ProblemException;
import com.foilen.infra.plugin.v1.core.service.IPResourceService;
import com.foilen.smalltools.tools.SearchingAvailabilityIntTools;

public class UnixUserAvailableIdHelper {

    private static SearchingAvailabilityIntTools cachedSearchingAvailability;

    public static int getNextAvailableId() {
        if (cachedSearchingAvailability == null) {
            throw new ProblemException("UnixUserAvailableIdHelper has not been initialised");
        }

        Optional<Integer> next = cachedSearchingAvailability.getNext();
        if (!next.isPresent()) {
            throw new ProblemException("There is no more unix user id available");
        }
        return next.get();
    }

    public static void init(IPResourceService resourceService) {
        cachedSearchingAvailability = new SearchingAvailabilityIntTools(2000, 60000, 100, //
                (from, to) -> {
                    int range = to - from + 1;
                    List<UnixUser> unixUsers = resourceService.resourceFindAll(resourceService.createResourceQuery(UnixUser.class) //
                            .propertyGreaterAndEquals(UnixUser.PROPERTY_ID, from) //
                            .propertyLesserAndEquals(UnixUser.PROPERTY_ID, to)).stream() //
                            .sorted((a, b) -> Integer.compare(a.getId(), b.getId())) //
                            .collect(Collectors.toList());

                    // There is one id available
                    if (range != unixUsers.size()) {
                        int foundId = from;
                        for (UnixUser unixUser : unixUsers) {
                            if (foundId != unixUser.getId()) {
                                break;
                            } else {
                                ++foundId;
                            }
                        }
                        return Optional.of(foundId);

                    }

                    return Optional.empty();
                });

    }

}
