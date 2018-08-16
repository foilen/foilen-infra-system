/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ComparisonChain;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UnixGroupDetail implements Comparable<UnixGroupDetail> {

    private String name;
    private Long gid;
    private List<String> members = new ArrayList<>();

    public UnixGroupDetail() {
    }

    @Override
    public int compareTo(UnixGroupDetail o) {
        return ComparisonChain.start() //
                .compare(name, o.name) //
                .compare(gid, o.gid) //
                .result();
    }

    public Long getGid() {
        return gid;
    }

    public List<String> getMembers() {
        return members;
    }

    public String getName() {
        return name;
    }

    public void setGid(Long gid) {
        this.gid = gid;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public void setMembers(String[] members) {
        this.members = new ArrayList<>();
        this.members.addAll(Arrays.asList(members));
    }

    public void setName(String name) {
        this.name = name;
    }

}
