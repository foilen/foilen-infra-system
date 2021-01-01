/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tuple.Tuple2;

/**
 * Will search ips from 172.20.5.1 to 172.20.255.254
 */
public class SearchNextIp extends AbstractBasics {

    private static final int INITIAL_C = 5;

    protected int c = INITIAL_C;
    protected int d = 0;

    private Set<Tuple2<Integer, Integer>> existing = new HashSet<>();

    /**
     * Feed the existing ips.
     *
     * @param existingIpsStream
     *            a stream of existing ips
     */
    public SearchNextIp(Stream<String> existingIpsStream) {
        existingIpsStream.forEach(ip -> {
            String[] parts = ip.split("\\.");
            if (parts.length != 4 || !parts[0].equals("172") || !parts[1].equals("20")) {
                return;
            }
            try {
                int currentC = Integer.parseInt(parts[2]);
                int currentD = Integer.parseInt(parts[3]);
                existing.add(new Tuple2<Integer, Integer>(currentC, currentD));
            } catch (Exception e) {
            }
        });

    }

    public String getNext() {

        // Move until getting the next available one
        boolean found = false;
        while (!found) {
            ++d;
            if (d > 255) {
                d = 1;
                ++c;
            }
            if (c > 255) {
                c = INITIAL_C;
            }
            if (c == 255 && d == 255) {
                c = INITIAL_C;
                d = 1;
            }

            found = !existing.contains(new Tuple2<>(c, d));
        }
        existing.add(new Tuple2<>(c, d));
        return "172.20." + c + "." + d;
    }

}
