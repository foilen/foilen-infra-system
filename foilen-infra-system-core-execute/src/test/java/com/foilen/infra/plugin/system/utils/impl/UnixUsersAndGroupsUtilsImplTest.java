/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.impl;

import java.io.File;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.foilen.infra.plugin.system.utils.mock.UnixShellAndFsUtilsTrackPermissionsMock;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.DirectoryTools;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.ResourceTools;

public class UnixUsersAndGroupsUtilsImplTest {

    private UnixShellAndFsUtilsTrackPermissionsMock unixShellAndFsUtilsTrackPermissionsMock;
    private UnixUsersAndGroupsUtilsImpl unixUsersAndGroupsUtils;

    private void assertFolder(String expectedResourceName) {

        StringBuilder report = new StringBuilder();

        for (String fileName : DirectoryTools.listFilesAndFoldersRecursively(unixUsersAndGroupsUtils.getHostFs(), false)) {

            String absolutePath = unixUsersAndGroupsUtils.getHostFs() + "/" + fileName;

            report.append("--[ ").append(fileName);
            String permission = unixShellAndFsUtilsTrackPermissionsMock.getPermissionsByPath().get(absolutePath);

            if (permission != null) {
                report.append(" ").append(permission);
            }
            report.append(" ]--\n");

            File file = new File(absolutePath);
            if (file.isFile()) {
                FileTools.readFileLinesStream(file).forEach(line -> report.append(line).append("\n"));
            }

        }

        String expected = ResourceTools.getResourceAsString(expectedResourceName, getClass());
        AssertTools.assertIgnoreLineFeed(expected, report.toString());

    }

    @Before
    public void init() throws Exception {

        unixShellAndFsUtilsTrackPermissionsMock = new UnixShellAndFsUtilsTrackPermissionsMock();
        unixUsersAndGroupsUtils = new UnixUsersAndGroupsUtilsImpl(unixShellAndFsUtilsTrackPermissionsMock);

        String hostFs = Files.createTempDirectory(null).toFile().getAbsolutePath();
        unixUsersAndGroupsUtils.setHostFs(hostFs);
        DirectoryTools.createPath(hostFs + "/etc/skel/deeper");
        DirectoryTools.createPath(hostFs + "/home");

        // User file
        File userFile = new File(hostFs + unixUsersAndGroupsUtils.getUserFile());
        ResourceTools.copyToFile("UnixUsersAndGroupsUtilsImplTest-passwd.txt", this.getClass(), userFile);

        // User Shadow file
        File userShadowFile = new File(hostFs + unixUsersAndGroupsUtils.getUserShadowFile());
        ResourceTools.copyToFile("UnixUsersAndGroupsUtilsImplTest-shadow.txt", this.getClass(), userShadowFile);

        // Group file
        File groupFile = new File(hostFs + unixUsersAndGroupsUtils.getGroupFile());
        ResourceTools.copyToFile("UnixUsersAndGroupsUtilsImplTest-group.txt", this.getClass(), groupFile);

        // Group Shadow file
        File groupShadowFile = new File(hostFs + unixUsersAndGroupsUtils.getGroupShadowFile());
        ResourceTools.copyToFile("UnixUsersAndGroupsUtilsImplTest-gshadow.txt", this.getClass(), groupShadowFile);

        // Skeleton
        ResourceTools.copyToFile("UnixUsersAndGroupsUtilsImplTest-skel-empty.txt", this.getClass(), new File(hostFs + "/etc/skel/.bash_logout"));
        ResourceTools.copyToFile("UnixUsersAndGroupsUtilsImplTest-skel-empty.txt", this.getClass(), new File(hostFs + "/etc/skel/.bashrc"));
        ResourceTools.copyToFile("UnixUsersAndGroupsUtilsImplTest-skel-empty.txt", this.getClass(), new File(hostFs + "/etc/skel/.profile"));
        ResourceTools.copyToFile("UnixUsersAndGroupsUtilsImplTest-skel-empty.txt", this.getClass(), new File(hostFs + "/etc/skel/deeper/in_sub"));

        assertFolder("UnixUsersAndGroupsUtilsImplTest-init.txt");
    }

    @Test
    public void testGroupAddMember() {

        Assert.assertTrue(unixUsersAndGroupsUtils.groupAddMember("dialout", "www-data"));

        assertFolder("UnixUsersAndGroupsUtilsImplTest-testGroupAddMember.txt");

        // Bis
        Assert.assertFalse(unixUsersAndGroupsUtils.groupAddMember("dialout", "www-data"));

        assertFolder("UnixUsersAndGroupsUtilsImplTest-testGroupAddMember.txt");

        // Second member
        Assert.assertTrue(unixUsersAndGroupsUtils.groupAddMember("dialout", "irc"));

        assertFolder("UnixUsersAndGroupsUtilsImplTest-testGroupAddMember-2.txt");

    }

    @Test
    public void testGroupCreate() {

        unixUsersAndGroupsUtils.groupCreate("mygroup", 70001L);

        assertFolder("UnixUsersAndGroupsUtilsImplTest-testGroupCreate.txt");

    }

    @Test
    public void testGroupDelete() {

        unixUsersAndGroupsUtils.groupDelete("dialout");

        assertFolder("UnixUsersAndGroupsUtilsImplTest-testGroupDelete.txt");

    }

    @Test
    public void testGroupNameUpdate() {

        unixUsersAndGroupsUtils.groupNameUpdate("dialout", "nextgendialout");

        assertFolder("UnixUsersAndGroupsUtilsImplTest-testGroupNameUpdate.txt");

    }

    @Test
    public void testUserCreate_all() {

        unixUsersAndGroupsUtils.userCreateOrUpdate("myuser", 70000L, "/home/myuser", "/bin/bash", "$6$nNXWxwVm$s2DFjbpb1hmfgPpCFqKKMYQ0VFygoBn5vq19zRt/ymMP9EfebU/3FlZuWsasyb34pAf8VarmLB3cE6M2ccefO1");

        assertFolder("UnixUsersAndGroupsUtilsImplTest-testUserCreate_all.txt");

    }

    @Test
    public void testUserCreate_minimal() {

        unixUsersAndGroupsUtils.userCreateOrUpdate("myuser", 70000L, null, null, null);

        assertFolder("UnixUsersAndGroupsUtilsImplTest-testUserCreate_minimal.txt");

    }

    @Test
    public void testUserRemove() {

        unixUsersAndGroupsUtils.userCreateOrUpdate("myuser", 70000L, "/home/myuser", "/bin/bash", "$6$nNXWxwVm$s2DFjbpb1hmfgPpCFqKKMYQ0VFygoBn5vq19zRt/ymMP9EfebU/3FlZuWsasyb34pAf8VarmLB3cE6M2ccefO1");
        assertFolder("UnixUsersAndGroupsUtilsImplTest-testUserCreate_all.txt");

        FileTools.writeFile("hello world", unixUsersAndGroupsUtils.getHostFs() + "/home/myuser/deeper/another");
        assertFolder("UnixUsersAndGroupsUtilsImplTest-testUserCreate_all_with_more.txt");

        unixUsersAndGroupsUtils.userRemove("myuser");
        assertFolder("UnixUsersAndGroupsUtilsImplTest-init.txt");
    }

    @Test
    public void testUserUpdateHome() {

        unixUsersAndGroupsUtils.userCreateOrUpdate("myuser", 70000L, "/home/myuser", "/bin/bash", "$6$nNXWxwVm$s2DFjbpb1hmfgPpCFqKKMYQ0VFygoBn5vq19zRt/ymMP9EfebU/3FlZuWsasyb34pAf8VarmLB3cE6M2ccefO1");
        assertFolder("UnixUsersAndGroupsUtilsImplTest-testUserCreate_all.txt");

        FileTools.writeFile("hello world", unixUsersAndGroupsUtils.getHostFs() + "/home/myuser/deeper/another");
        assertFolder("UnixUsersAndGroupsUtilsImplTest-testUserCreate_all_with_more.txt");

        unixUsersAndGroupsUtils.userCreateOrUpdate("myuser", 70000L, "/home/anotherplace", "/bin/bash",
                "$6$nNXWxwVm$s2DFjbpb1hmfgPpCFqKKMYQ0VFygoBn5vq19zRt/ymMP9EfebU/3FlZuWsasyb34pAf8VarmLB3cE6M2ccefO1");
        assertFolder("UnixUsersAndGroupsUtilsImplTest-testUserUpdateHome.txt");

    }

    @Test
    public void testUserUpdateMinimalToAllToMinimal() {

        unixUsersAndGroupsUtils.userCreateOrUpdate("myuser", 70000L, null, null, null);
        assertFolder("UnixUsersAndGroupsUtilsImplTest-testUserCreate_minimal.txt");

        unixUsersAndGroupsUtils.userCreateOrUpdate("myuser", 70000L, "/home/myuser", "/bin/bash", "$6$nNXWxwVm$s2DFjbpb1hmfgPpCFqKKMYQ0VFygoBn5vq19zRt/ymMP9EfebU/3FlZuWsasyb34pAf8VarmLB3cE6M2ccefO1");
        assertFolder("UnixUsersAndGroupsUtilsImplTest-testUserCreate_all.txt");

        FileTools.writeFile("hello world", unixUsersAndGroupsUtils.getHostFs() + "/home/myuser/deeper/another");
        assertFolder("UnixUsersAndGroupsUtilsImplTest-testUserCreate_all_with_more.txt");

        unixUsersAndGroupsUtils.userCreateOrUpdate("myuser", 70000L, null, null, null);
        assertFolder("UnixUsersAndGroupsUtilsImplTest-testUserCreate_minimal.txt");

    }

}
