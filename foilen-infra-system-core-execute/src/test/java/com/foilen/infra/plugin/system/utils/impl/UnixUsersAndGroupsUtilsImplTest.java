/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2020 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.foilen.infra.plugin.system.utils.model.UnixGroupDetail;
import com.foilen.infra.plugin.system.utils.model.UnixUserDetail;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.ResourceTools;
import com.google.common.base.Joiner;
import com.google.common.io.Files;

public class UnixUsersAndGroupsUtilsImplTest {

    private UnixUsersAndGroupsUtilsImpl unixUsersAndGroupsUtils;

    @Before
    public void init() throws Exception {
        unixUsersAndGroupsUtils = new UnixUsersAndGroupsUtilsImpl();

        // Group file
        File groupFile = File.createTempFile("group", null);
        ResourceTools.copyToFile("UnixUsersAndGroupsUtilsImplTest-group.txt", this.getClass(), groupFile);
        unixUsersAndGroupsUtils.setGroupFile(groupFile.getAbsolutePath());

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
        AssertTools.assertJsonComparison("UnixUsersAndGroupsUtilsImplTest-testGetAllUsers-ccloud1.json", getClass(), ccloud1);

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

    @Test
    public void testGroupSaveGroup() throws IOException {

        List<UnixGroupDetail> unixGroupDetails = unixUsersAndGroupsUtils.groupGetAll();

        Assert.assertEquals(22, unixGroupDetails.size());

        // Group file
        File groupFile = File.createTempFile("group", null);
        unixUsersAndGroupsUtils.setGroupFile(groupFile.getAbsolutePath());

        // Save
        unixUsersAndGroupsUtils.groupSaveGroup(unixGroupDetails);

        // Assert
        String expected = ResourceTools.getResourceAsString("UnixUsersAndGroupsUtilsImplTest-group.txt", this.getClass());
        expected = expected.replaceAll("\r", "");
        List<String> expectedLines = Arrays.asList(expected.split("\n")).stream().sorted().collect(Collectors.toList());
        expected = Joiner.on('\n').join(expectedLines);
        expected += "\n";

        String actual = FileTools.getFileAsString(groupFile);
        AssertTools.assertIgnoreLineFeed(expected, actual);
    }

    @Test
    public void testUserSavePasswd() throws IOException {

        List<UnixUserDetail> unixUserDetails = unixUsersAndGroupsUtils.userGetAll();

        Assert.assertEquals(11, unixUserDetails.size());

        // Passwd file
        File passwdFile = File.createTempFile("passwd", null);
        unixUsersAndGroupsUtils.setPasswdFile(passwdFile.getAbsolutePath());

        // Save
        unixUsersAndGroupsUtils.userSavePasswd(unixUserDetails);

        // Assert
        String expected = ResourceTools.getResourceAsString("UnixUsersAndGroupsUtilsImplTest-passwd.txt", this.getClass());
        String actual = FileTools.getFileAsString(passwdFile);
        AssertTools.assertIgnoreLineFeed(expected, actual);
    }

}
