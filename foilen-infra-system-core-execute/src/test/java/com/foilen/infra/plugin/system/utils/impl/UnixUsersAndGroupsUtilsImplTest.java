/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.foilen.infra.plugin.system.utils.model.UnixUserDetail;
import com.foilen.smalltools.tools.ResourceTools;
import com.google.common.io.Files;

public class UnixUsersAndGroupsUtilsImplTest {

    private UnixUsersAndGroupsUtilsImpl unixUsersAndGroupsUtils;

    @Before
    public void init() throws Exception {
        unixUsersAndGroupsUtils = new UnixUsersAndGroupsUtilsImpl();

        // Passwd file
        File passwdFile = File.createTempFile("passwd", null);
        ResourceTools.copyToFile("UnixUsersAndGroupsUtilsImplTest-passwd.txt", this.getClass(), passwdFile);
        unixUsersAndGroupsUtils.setPasswdFile(passwdFile.getAbsolutePath());

        // Shadow file
        File shadowFile = File.createTempFile("shadow", null);
        ResourceTools.copyToFile("UnixUsersAndGroupsUtilsImplTest-shadow.txt", this.getClass(), shadowFile);
        unixUsersAndGroupsUtils.setShadowFile(shadowFile.getAbsolutePath());

        // Sudo folder
        File sudoFolderFile = Files.createTempDir();
        unixUsersAndGroupsUtils.setSudoDirectory(sudoFolderFile.getAbsolutePath());
        ResourceTools.copyToFile("UnixUsersAndGroupsUtilsImplTest-sudo-ccloud-1.txt", this.getClass(), new File(unixUsersAndGroupsUtils.getSudoDirectory() + "ccloud-1"));
    }

    @Test
    public void testGetAllUsers() {

        List<UnixUserDetail> unixUserDetails = unixUsersAndGroupsUtils.userGetAll();

        Assert.assertEquals(11, unixUserDetails.size());

        // Check all names
        int i = 0;
        for (int y = 0; y < 6; ++y) {
            Assert.assertEquals("reserved-" + y, unixUserDetails.get(i++).getName());
        }
        for (int y = 0; y < 4; ++y) {
            Assert.assertEquals("ccloud-" + y, unixUserDetails.get(i++).getName());
        }

        // Check details
        UnixUserDetail ccloud1 = unixUserDetails.get(7);
        Assert.assertEquals("ccloud-1", ccloud1.getName());
        Assert.assertEquals((Integer) 10003, ccloud1.getId());
        Assert.assertEquals("$6$oN3S1aK5$O0/HS5p3QQpr68i3epER7xILkTNBX2uBr71Qqnhd1WS6qnn26s/xCodBOTLVTOmc.Ukxy1nKygiCoYngmSIeS.", ccloud1.getHashedPassword());
        Assert.assertEquals("/home/ccloud-1", ccloud1.getHomeFolder());
        Assert.assertEquals("/bin/bash", ccloud1.getShell());
        List<String> sudos = ccloud1.getSudos().stream().sorted().collect(Collectors.toList());
        Assert.assertEquals(2, sudos.size());
        Assert.assertEquals("/bin/chown ccloud-1:ccloud-1 -R /home/ccloud-1/gitlab/", sudos.get(0));
        Assert.assertEquals("/home/ccloud-1/update.sh", sudos.get(1));

        // Check null hashed password
        UnixUserDetail ccloud2 = unixUserDetails.get(8);
        Assert.assertNull(ccloud2.getHashedPassword());
        UnixUserDetail ccloud3 = unixUserDetails.get(8);
        Assert.assertNull(ccloud3.getHashedPassword());

    }

    @Test
    public void testGroupMemberIn() throws IOException {

        // Group file
        File groupFile = File.createTempFile("group", null);
        ResourceTools.copyToFile("UnixUsersAndGroupsUtilsImplTest-group.txt", this.getClass(), groupFile);

        // Asserts
        Assert.assertTrue(unixUsersAndGroupsUtils.groupMemberIn(groupFile.getAbsolutePath(), "foilen-cdn", "www-data"));
        Assert.assertFalse(unixUsersAndGroupsUtils.groupMemberIn(groupFile.getAbsolutePath(), "foilen-cdn", "debian-spamd"));
        Assert.assertFalse(unixUsersAndGroupsUtils.groupMemberIn(groupFile.getAbsolutePath(), "foilen-cdn", "10021"));
        Assert.assertTrue(unixUsersAndGroupsUtils.groupMemberIn(groupFile.getAbsolutePath(), "postfix", "debian-spamd"));
        Assert.assertTrue(unixUsersAndGroupsUtils.groupMemberIn(groupFile.getAbsolutePath(), "postfix", "dovecot"));
        Assert.assertFalse(unixUsersAndGroupsUtils.groupMemberIn(groupFile.getAbsolutePath(), "postfix", "www-data"));

    }

}
