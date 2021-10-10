/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

import com.foilen.infra.plugin.system.utils.UnixShellAndFsUtils;
import com.foilen.infra.plugin.system.utils.UnixUsersAndGroupsUtils;
import com.foilen.infra.plugin.system.utils.UtilsException;
import com.foilen.infra.plugin.system.utils.model.UnixGroupDetail;
import com.foilen.infra.plugin.system.utils.model.UnixUserDetail;
import com.foilen.smalltools.streamwrapper.RenamingOnCloseOutputStreamWrapper;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.AssertTools;
import com.foilen.smalltools.tools.CloseableTools;
import com.foilen.smalltools.tools.DirectoryTools;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.StringTools;
import com.foilen.smalltools.tools.SystemTools;
import com.google.common.base.Strings;

public class UnixUsersAndGroupsUtilsImpl extends AbstractBasics implements UnixUsersAndGroupsUtils {

    private UnixShellAndFsUtils unixShellAndFsUtils = new UnixShellAndFsUtilsImpl();;

    private String hostFs = DirectoryTools.pathTrailingSlash(SystemTools.getPropertyOrEnvironment("HOSTFS", "/"));

    private String groupFile = "/etc/group";
    private String groupShadowFile = "/etc/gshadow";
    private String userFile = "/etc/passwd";
    private String userShadowFile = "/etc/shadow";
    private String rootDirectory = "/";
    private String etcDirectory = "/etc/";
    private String skeletonDirectory = "/etc/skel/";

    public UnixUsersAndGroupsUtilsImpl() {
    }

    public UnixUsersAndGroupsUtilsImpl(UnixShellAndFsUtils unixShellAndFsUtils) {
        this.unixShellAndFsUtils = unixShellAndFsUtils;
    }

    public String getEtcDirectory() {
        return etcDirectory;
    }

    public String getGroupFile() {
        return groupFile;
    }

    public String getGroupShadowFile() {
        return groupShadowFile;
    }

    public String getHostFs() {
        return hostFs;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public String getSkeletonDirectory() {
        return skeletonDirectory;
    }

    public String getUserFile() {
        return userFile;
    }

    public String getUserShadowFile() {
        return userShadowFile;
    }

    @Override
    public boolean groupAddMember(String groupName, String userName) {
        logger.info("[GROUP] Add member {} to group {}", userName, groupName);
        if (groupMemberIn(groupName, userName)) {
            logger.info("[GROUP] Member {} is already in group {}", userName, groupName);
        } else {

            groupUpdateGroupFiles(groupByName -> {
                UnixGroupDetail group = groupByName.get(groupName);
                if (group == null) {
                    throw new UtilsException("[GROUP MEMBER ADD] Group " + groupName + " does not exist");
                }

                group.getMembers().add(userName);

            });

            return true;
        }

        return false;
    }

    @Override
    public boolean groupCreate(String groupName, Long id) {

        logger.info("[GROUP] Creating {} with id {}", groupName, id);
        if (groupExists(groupName)) {
            logger.info("[GROUP] {} already exists", groupName);
        } else {
            groupUpdateGroupFiles(groupByName -> {
                groupByName.putIfAbsent(groupName, new UnixGroupDetail().setGid(id).setName(groupName));
            });
            return true;
        }
        return false;
    }

    @Override
    public boolean groupDelete(String groupName) {
        logger.info("[GROUP] Deleting {}", groupName);
        if (groupExists(groupName)) {

            groupUpdateGroupFiles(groupByName -> {
                groupByName.remove(groupName);
            });

            return true;
        } else {
            logger.info("[GROUP] {} already deleted", groupName);
        }
        return true;
    }

    @Override
    public boolean groupExists(String groupName) {
        String start = groupName + ":";

        for (String it : FileTools.readFileLinesIteration(hostFs + etcDirectory + "group")) {
            if (it.startsWith(start)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public LinkedHashMap<String, UnixGroupDetail> groupGetAll() {

        LinkedHashMap<String, UnixGroupDetail> detailsByName = new LinkedHashMap<>();

        // Read the group file
        for (String line : FileTools.readFileLinesIteration(hostFs + groupFile)) {
            UnixGroupDetail unixGroupDetail = UnixGroupDetail.fromGroup(line);
            detailsByName.put(unixGroupDetail.getName(), unixGroupDetail);
        }

        // Read the group shadow file
        for (String line : FileTools.readFileLinesIteration(hostFs + groupShadowFile)) {
            UnixGroupDetail shadowUnixGroupDetail = UnixGroupDetail.fromGroupShadow(line);
            UnixGroupDetail unixGroupDetail = detailsByName.get(shadowUnixGroupDetail.getName());
            if (unixGroupDetail == null) {
                logger.info("[GROUP] The group {} exists in gshadow, but not in group. Skipping", shadowUnixGroupDetail.getName());
                continue;
            }

            unixGroupDetail.mergeShadow(shadowUnixGroupDetail);
        }

        return detailsByName;

    }

    @Override
    public boolean groupMemberIn(String groupName, String userName) {

        UnixGroupDetail group = groupGetAll().get(groupName);
        if (group == null) {
            return false;
        }

        return group.getMembers().contains(userName);
    }

    @Override
    public void groupNameUpdate(String oldGroupName, String newGroupName) {

        logger.info("[GROUP] Rename {} to {}", oldGroupName, newGroupName);
        groupUpdateGroupFiles(groupByName -> {
            UnixGroupDetail group = groupByName.remove(oldGroupName);
            if (group == null) {
                logger.error("[GROUP] Group {} does not exist", oldGroupName);
                throw new UtilsException("[GROUP NAME UPDATE] Group " + oldGroupName + " does not exist");
            }

            if (groupByName.containsKey(newGroupName)) {
                logger.error("[GROUP] Group {} already exist", newGroupName);
                throw new UtilsException("[GROUP NAME UPDATE] Group " + newGroupName + " already exist");
            }

            group.setName(newGroupName);
            groupByName.put(newGroupName, group);
        });

    }

    /**
     * <p>
     * This is locking the group and the gshadow files, is reading all of the group details, is providing the list that can be updated, is saving the files and releasing the locks.
     * </p>
     *
     * @param unixGroupDetailsConsumer
     *            the consumer that will modify the list
     */
    protected void groupUpdateGroupFiles(Consumer<LinkedHashMap<String, UnixGroupDetail>> unixGroupDetailsConsumer) {

        File actualGroupFile = new File(hostFs + groupFile);
        File actualGroupShadowFile = new File(hostFs + groupShadowFile);
        RandomAccessFile groupRandomAccessFile = null;
        RandomAccessFile groupShadowRandomAccessFile = null;
        try {
            groupRandomAccessFile = new RandomAccessFile(actualGroupFile, "rw");
            groupShadowRandomAccessFile = new RandomAccessFile(actualGroupShadowFile, "rw");

            try (FileLock groupFileLock = groupRandomAccessFile.getChannel().lock(); FileLock groupShadowFileLock = groupShadowRandomAccessFile.getChannel().lock()) {

                // Load
                LinkedHashMap<String, UnixGroupDetail> groupByName = groupGetAll();

                // Request update
                unixGroupDetailsConsumer.accept(groupByName);

                // Save
                saveFileWithStaging(hostFs + groupFile + ".tmp", hostFs + groupFile, printWriter -> {
                    groupByName.values().forEach(unixGroupDetail -> {
                        printWriter.println(unixGroupDetail.toGroup());
                    });
                });

                saveFileWithStaging(hostFs + groupShadowFile + ".tmp", hostFs + groupShadowFile, printWriter -> {
                    groupByName.values().forEach(unixGroupDetail -> {
                        printWriter.println(unixGroupDetail.toGroupShadow());
                    });
                });

            }

        } catch (Exception e) {
            throw new UtilsException("[GROUP UPDATE] Got an exception", e);
        } finally {
            CloseableTools.close(groupRandomAccessFile);
            CloseableTools.close(groupShadowRandomAccessFile);
        }

    }

    protected void saveFileWithStaging(String stagingFileName, String finalFileName, Consumer<PrintWriter> printWriterConsumer) {
        PrintWriter printWriter = null;

        try {
            File stagingFile = new File(stagingFileName);
            File finalFile = new File(finalFileName);
            FileOutputStream outputStream = new FileOutputStream(stagingFile);
            RenamingOnCloseOutputStreamWrapper renamingOnCloseOutputStreamWrapper = new RenamingOnCloseOutputStreamWrapper(outputStream, stagingFile, finalFile, true);
            printWriter = new PrintWriter(new OutputStreamWriter(renamingOnCloseOutputStreamWrapper));

            printWriterConsumer.accept(printWriter);
            renamingOnCloseOutputStreamWrapper.setDeleteOnClose(false);
        } catch (Exception e) {
            logger.info("Problem saving file {}", stagingFileName, e);
        } finally {
            CloseableTools.close(printWriter);
        }
    }

    public UnixUsersAndGroupsUtilsImpl setEtcDirectory(String etcDirectory) {
        this.etcDirectory = etcDirectory;
        return this;
    }

    public UnixUsersAndGroupsUtilsImpl setGroupFile(String groupFile) {
        this.groupFile = groupFile;
        return this;
    }

    public UnixUsersAndGroupsUtilsImpl setGroupShadowFile(String groupShadowFile) {
        this.groupShadowFile = groupShadowFile;
        return this;
    }

    public void setHostFs(String hostFs) {
        this.hostFs = hostFs;
    }

    public UnixUsersAndGroupsUtilsImpl setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
        return this;
    }

    public void setSkeletonDirectory(String skeletonDirectory) {
        this.skeletonDirectory = skeletonDirectory;
    }

    public UnixUsersAndGroupsUtilsImpl setUserFile(String userFile) {
        this.userFile = userFile;
        return this;
    }

    public UnixUsersAndGroupsUtilsImpl setUserShadowFile(String userShadowFile) {
        this.userShadowFile = userShadowFile;
        return this;
    }

    @Override
    public boolean userCreateOrUpdate(String username, Long id, String homeFolder, String shell, String hashedPassword) {

        logger.info("[USER] Creating/updating {} with id {} , home {} , shell {} and hashed password {}", username, id, homeFolder, shell, hashedPassword);

        String previousHomeFoler = null;

        // Create group
        boolean createdOrUpdated = groupCreate(username, id);

        UnixUserDetail currentUserDetail = userGet(username);
        if (currentUserDetail != null) {
            previousHomeFoler = Strings.emptyToNull(currentUserDetail.getHomeFolder());
        }

        // Prepare desired
        UnixUserDetail desiredUserDetail = new UnixUserDetail(id, id, username, homeFolder, shell);
        desiredUserDetail.setHashedPassword(hashedPassword);

        // Create if missing
        if (currentUserDetail == null) {

            userUpdateUserFiles(unixUserDetails -> {
                if (unixUserDetails.containsKey(username)) {
                    throw new UtilsException("[USER] [" + username + "] was created a few ms earlier");
                }

                unixUserDetails.put(username, desiredUserDetail);
            });

            createdOrUpdated = true;
            logger.info("[USER] {} was created", username);

        } else {

            logger.info("[USER] {} already exists", username);

            createdOrUpdated = !StringTools.safeEquals(JsonTools.compactPrintWithoutNulls(desiredUserDetail), JsonTools.compactPrintWithoutNulls(currentUserDetail));

            if (createdOrUpdated) {

                userUpdateUserFiles(unixUserDetails -> {
                    unixUserDetails.put(username, desiredUserDetail);
                });

            }
        }

        // Manage home folder
        if (createdOrUpdated) {
            if (!StringTools.safeEquals(previousHomeFoler, homeFolder)) {
                logger.info("[USER] {} change home folder {} -> {}", username, previousHomeFoler, homeFolder);

                if (previousHomeFoler == null) {
                    logger.info("[USER] {} create home folder {} and copy the skeleton", username, homeFolder);

                    String absoluteHomeFolder = hostFs + homeFolder;
                    unixShellAndFsUtils.folderCreate(absoluteHomeFolder, id, id, "750");

                    // Copy skeleton /etc/skel
                    String absoluteSkeletonFolder = hostFs + skeletonDirectory;
                    DirectoryTools.listFilesAndFoldersRecursively(absoluteSkeletonFolder, false).forEach(fileName -> {
                        String source = absoluteSkeletonFolder + fileName;
                        String destination = absoluteHomeFolder + "/" + fileName;
                        logger.info("[USER] {} copy {} -> {}", username, source, destination);
                        try {
                            Files.copy(new File(source).toPath(), new File(destination).toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                        } catch (IOException e) {
                            throw new UtilsException("[USER] Got an exception", e);
                        }
                        unixShellAndFsUtils.fileChangeOwner(destination, id, id);
                    });
                } else if (homeFolder == null) {
                    logger.info("[USER] {} delete home folder {}", username, previousHomeFoler);
                    DirectoryTools.deleteFolder(hostFs + previousHomeFoler);
                } else {
                    logger.info("[USER] {} move home folder {} -> {}", username, previousHomeFoler, homeFolder);
                    String source = hostFs + previousHomeFoler;
                    String destination = hostFs + homeFolder;
                    AssertTools.assertTrue(new File(source).renameTo(new File(destination)), "Problem moving home folder");
                }
            }
        }

        return createdOrUpdated;
    }

    @Override
    public boolean userExists(String username) {
        String start = username + ":";

        for (String it : FileTools.readFileLinesIteration(hostFs + etcDirectory + "passwd")) {
            if (it.startsWith(start)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public UnixUserDetail userGet(String username) {
        if (username == null) {
            return null;
        }

        return userGetAll().get(username);
    }

    @Override
    public LinkedHashMap<String, UnixUserDetail> userGetAll() {

        LinkedHashMap<String, UnixUserDetail> detailsByName = new LinkedHashMap<>();

        // Read the user file
        for (String line : FileTools.readFileLinesIteration(hostFs + userFile)) {
            UnixUserDetail unixUserDetail = UnixUserDetail.fromUser(line);
            detailsByName.put(unixUserDetail.getName(), unixUserDetail);
        }

        // Read the shadow file
        for (String line : FileTools.readFileLinesIteration(hostFs + userShadowFile)) {
            UnixUserDetail shadowUnixUserDetail = UnixUserDetail.fromUserShadow(line);
            UnixUserDetail unixUserDetail = detailsByName.get(shadowUnixUserDetail.getName());
            if (unixUserDetail == null) {
                logger.info("[USER] The user {} exists in shadow, but not in passwd. Skipping", shadowUnixUserDetail.getName());
                continue;
            }

            unixUserDetail.mergeShadow(shadowUnixUserDetail);

        }

        return detailsByName;
    }

    @Override
    public boolean userRemove(String username) {

        if (!userExists(username)) {
            logger.info("[USER] {} already deleted", username);
            return false;
        }

        userUpdateUserFiles(unixUserDetails -> {
            UnixUserDetail unixUserDetail = unixUserDetails.remove(username);

            // Delete home
            String homePath = Strings.emptyToNull(unixUserDetail.getHomeFolder());
            if (homePath != null) {
                logger.info("[USER] {} delete home folder {}", username, homePath);
                DirectoryTools.deleteFolder(hostFs + homePath);

            }

            // Group delete
            groupDelete(username);

        });

        return true;
    }

    /**
     * <p>
     * This is locking the passwd and shadow files, is reading all of the user details, is providing the list that can be updated, is saving the files and releasing the locks.
     * </p>
     *
     * @param unixUserDetailsConsumer
     *            the consumer that will modify the list
     */
    protected void userUpdateUserFiles(Consumer<LinkedHashMap<String, UnixUserDetail>> unixUserDetailsConsumer) {

        File actualUserFile = new File(hostFs + userFile);
        File actualUserShadowFile = new File(hostFs + userShadowFile);
        RandomAccessFile userRandomAccessFile = null;
        RandomAccessFile userShadowRandomAccessFile = null;
        try {
            userRandomAccessFile = new RandomAccessFile(actualUserFile, "rw");
            userShadowRandomAccessFile = new RandomAccessFile(actualUserShadowFile, "rw");

            try (FileLock userFileLock = userRandomAccessFile.getChannel().lock(); FileLock userShadowFileLock = userShadowRandomAccessFile.getChannel().lock()) {

                // Load
                LinkedHashMap<String, UnixUserDetail> userByName = userGetAll();

                // Request update
                unixUserDetailsConsumer.accept(userByName);

                // Save
                saveFileWithStaging(hostFs + userFile + ".tmp", hostFs + userFile, printWriter -> {
                    userByName.values().forEach(unixGroupDetail -> {
                        printWriter.println(unixGroupDetail.toUser());
                    });
                });

                saveFileWithStaging(hostFs + userShadowFile + ".tmp", hostFs + userShadowFile, printWriter -> {
                    userByName.values().forEach(unixGroupDetail -> {
                        printWriter.println(unixGroupDetail.toUserShadow());
                    });
                });

            }

        } catch (Exception e) {
            throw new UtilsException("[USER PASSWD UPDATE] Got an exception", e);
        } finally {
            CloseableTools.close(userRandomAccessFile);
            CloseableTools.close(userShadowRandomAccessFile);
        }

    }

}
