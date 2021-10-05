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
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.foilen.infra.plugin.system.utils.UtilsException;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UnixUserDetail implements Comparable<UnixUserDetail> {

    private final static List<String> noPasswords = Collections.unmodifiableList(Arrays.asList("", "!", "*"));

    /**
     * Get the {@link UnixUserDetail} from a line in /etc/passwd
     *
     * @param line
     *            the line
     * @return the unix user detail
     */
    public static UnixUserDetail fromUser(String line) {
        List<String> parts = split(line);
        if (parts.size() < 6) {
            throw new UtilsException("[USER] The entry [" + line + "] is invalid in the passwd file");
        }

        UnixUserDetail unixUserDetail = new UnixUserDetail();
        int i = 0;
        unixUserDetail.setName(parts.get(i++));
        ++i;
        unixUserDetail.setId(Long.valueOf(parts.get(i++)));
        unixUserDetail.setGid(Long.valueOf(parts.get(i++)));
        unixUserDetail.setGecos(parts.get(i++));
        unixUserDetail.setHomeFolder(parts.get(i++));
        if (parts.size() >= 7) {
            unixUserDetail.setShell(parts.get(i++));
        }

        return unixUserDetail;
    }

    /**
     * Get the {@link UnixUserDetail} from a line in /etc/shadow
     *
     * @param line
     *            the line
     * @return the unix user detail
     */
    public static UnixUserDetail fromUserShadow(String line) {

        String parts[] = line.split(":");
        if (parts.length < 4) {
            throw new UtilsException("[USER SHADOW] The entry [" + line + "] is invalid in the user shadow file");
        }
        UnixUserDetail unixUserDetails = new UnixUserDetail();
        int i = 0;
        unixUserDetails.setName(parts[i++]);
        unixUserDetails.setHashedPassword(parts[i++]);
        unixUserDetails.setLastPasswordChange(Long.valueOf(parts[i++]));

        // Put hashed password to null if no password account
        if (noPasswords.contains(unixUserDetails.getHashedPassword())) {
            unixUserDetails.setHashedPassword(null);
        }

        return unixUserDetails;
    }

    private static List<String> split(String line) {

        List<String> parts = new ArrayList<>();
        int startPos = 0;
        while (startPos <= line.length()) {
            int endPos = line.indexOf(':', startPos);
            if (endPos == -1) {
                endPos = line.length();
            }

            parts.add(line.substring(startPos, endPos));

            startPos = endPos + 1;

        }

        return parts;
    }

    private Long id;
    private Long gid;
    private String name;
    private String gecos;
    private String homeFolder;
    private String shell;
    private String hashedPassword;
    private Long lastPasswordChange;

    public UnixUserDetail() {
    }

    public UnixUserDetail(Long id, Long gid, String name, String homeFolder, String shell) {
        this.id = id;
        this.gid = gid;
        this.name = name;
        this.homeFolder = homeFolder;
        this.shell = shell;
    }

    @Override
    public int compareTo(UnixUserDetail o) {
        return ComparisonChain.start() //
                .compare(id, o.id) //
                .compare(gid, o.gid) //
                .compare(name, o.name) //
                .result();
    }

    public String getGecos() {
        return gecos;
    }

    public Long getGid() {
        return gid;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public String getHomeFolder() {
        return homeFolder;
    }

    public Long getId() {
        return id;
    }

    public Long getLastPasswordChange() {
        return lastPasswordChange;
    }

    public String getName() {
        return name;
    }

    public String getShell() {
        return shell;
    }

    /**
     * Merge the current unix user by adding the shadow specific details.
     *
     * @param shadowUnixUserDetail
     *            the shadow details
     */
    public void mergeShadow(UnixUserDetail shadowUnixUserDetail) {
        this.hashedPassword = shadowUnixUserDetail.hashedPassword;
        this.lastPasswordChange = shadowUnixUserDetail.lastPasswordChange;
    }

    public void setGecos(String gecos) {
        this.gecos = gecos;
    }

    public void setGid(Long gid) {
        this.gid = gid;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public void setHomeFolder(String homeFolder) {
        this.homeFolder = homeFolder;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLastPasswordChange(Long lastPasswordChange) {
        this.lastPasswordChange = lastPasswordChange;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setShell(String shell) {
        this.shell = shell;
    }

    /**
     * Get the line in /etc/passwd file.
     *
     * @return the line
     */
    public String toUser() {
        return name + ":x:" + id + ":" + gid + ":" + Strings.nullToEmpty(gecos) + ":" + Strings.nullToEmpty(homeFolder) + ":" + Strings.nullToEmpty(shell);
    }

    /**
     * Get the line in /etc/shadow file.
     *
     * @return the line
     */
    public String toUserShadow() {
        String hashedPasswordPart = hashedPassword == null ? "*" : hashedPassword;
        long lastPasswordChangePart = lastPasswordChange == null ? 1 : lastPasswordChange;
        return name + ":" + hashedPasswordPart + ":" + lastPasswordChangePart + ":0:99999:7:::";
    }

}
