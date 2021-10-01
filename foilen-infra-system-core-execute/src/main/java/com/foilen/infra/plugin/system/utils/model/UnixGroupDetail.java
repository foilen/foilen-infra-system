/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.foilen.infra.plugin.system.utils.UtilsException;
import com.google.common.base.Joiner;
import com.google.common.collect.ComparisonChain;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UnixGroupDetail implements Comparable<UnixGroupDetail> {

    private static final Joiner MEMBERS_SEPARATOR = Joiner.on(',');

    /**
     * Get the {@link UnixGroupDetail} from a line in /etc/group
     *
     * @param line
     *            the line
     * @return the unix group detail
     */
    public static UnixGroupDetail fromGroup(String line) {
        String parts[] = line.split(":");
        if (parts.length != 3 && parts.length != 4) {
            throw new UtilsException("[GROUP] The entry [" + line + "] is invalid in the group file");
        }

        UnixGroupDetail unixGroupDetail = new UnixGroupDetail();
        int i = 0;
        unixGroupDetail.setName(parts[i++]);
        ++i;
        unixGroupDetail.setGid(Long.valueOf(parts[i++]));
        if (parts.length > 3) {
            unixGroupDetail.setMembers(parts[i++].split(","));
        }

        return unixGroupDetail;
    }

    /**
     * Get the {@link UnixGroupDetail} from a line in /etc/gshadow
     *
     * @param line
     *            the line
     * @return the unix group detail
     */
    public static UnixGroupDetail fromGroupShadow(String line) {
        String parts[] = line.split(":");
        if (parts.length < 2) {
            throw new UtilsException("[GROUP SHADOW] The entry [" + line + "] is invalid in the group shadow file");
        }

        UnixGroupDetail unixGroupDetail = new UnixGroupDetail();
        int i = 0;
        unixGroupDetail.setName(parts[i++]);
        unixGroupDetail.setHashedPassword(parts[i++]);

        return unixGroupDetail;
    }

    private String name;
    private Long gid;

    private List<String> members = new ArrayList<>();

    private String hashedPassword;

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

    public String getHashedPassword() {
        return hashedPassword;
    }

    public List<String> getMembers() {
        return members;
    }

    public String getName() {
        return name;
    }

    /**
     * Merge the current unix group by adding the shadow specific details.
     *
     * @param shadowUnixGroupDetail
     *            the shadow details
     */
    public void mergeShadow(UnixGroupDetail shadowUnixGroupDetail) {
        this.hashedPassword = shadowUnixGroupDetail.hashedPassword;
    }

    public UnixGroupDetail setGid(Long gid) {
        this.gid = gid;
        return this;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public UnixGroupDetail setMembers(List<String> members) {
        this.members = members;
        return this;
    }

    public UnixGroupDetail setMembers(String[] members) {
        this.members = new ArrayList<>();
        this.members.addAll(Arrays.asList(members));
        return this;
    }

    public UnixGroupDetail setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the line in /etc/group file.
     *
     * @return the line
     */
    public String toGroup() {
        return name + ":x:" + gid + ":" + MEMBERS_SEPARATOR.join(members);
    }

    /**
     * Get the line in /etc/gshadow file.
     *
     * @return the line
     */
    public String toGroupShadow() {
        String hashPart = hashedPassword == null ? "!" : hashedPassword;
        return name + ":" + hashPart + "::" + MEMBERS_SEPARATOR.join(members);
    }

}
