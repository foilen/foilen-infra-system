/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.foilen.infra.plugin.system.utils.UnixShellAndFsUtils;
import com.foilen.infra.plugin.system.utils.UnixUsersAndGroupsUtils;
import com.foilen.infra.plugin.system.utils.UtilsException;
import com.foilen.infra.plugin.system.utils.model.UnixUserDetail;
import com.foilen.smalltools.consolerunner.ConsoleRunner;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.CharsetTools;
import com.foilen.smalltools.tools.CollectionsTools;
import com.foilen.smalltools.tools.DirectoryTools;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.StringTools;
import com.foilen.smalltools.tools.SystemTools;
import com.google.common.base.Strings;

public class UnixUsersAndGroupsUtilsImpl extends AbstractBasics implements UnixUsersAndGroupsUtils {

    private UnixShellAndFsUtils unixShellAndFsUtils;

    private String hostFs = SystemTools.getPropertyOrEnvironment("HOSTFS", "/");

    private String passwdFile = "/etc/passwd";
    private String shadowFile = "/etc/shadow";
    private String rootDirectory = "/";
    private String etcDirectory = "/etc/";
    private String sudoDirectory = "/etc/sudoers.d/";

    public UnixUsersAndGroupsUtilsImpl() {
        unixShellAndFsUtils = new UnixShellAndFsUtilsImpl();
    }

    public UnixUsersAndGroupsUtilsImpl(UnixShellAndFsUtils unixShellAndFsUtils) {
        this.unixShellAndFsUtils = unixShellAndFsUtils;
        hostFs = DirectoryTools.pathTrailingSlash(hostFs);
    }

    public String getEtcDirectory() {
        return etcDirectory;
    }

    public String getPasswdFile() {
        return passwdFile;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public String getShadowFile() {
        return shadowFile;
    }

    public String getSudoDirectory() {
        return sudoDirectory;
    }

    public UnixShellAndFsUtils getUnixShellAndFsUtils() {
        return unixShellAndFsUtils;
    }

    @Override
    public boolean groupAddMember(String groupName, String userName) {
        logger.info("[GROUP] Add member {} to group {}", userName, groupName);
        if (groupMemberIn(groupName, userName)) {
            logger.info("[GROUP] Member {} is already in group {}", userName, groupName);
        } else {
            ConsoleRunner consoleRunner = new ConsoleRunner();
            consoleRunner.setCommand("gpasswd");
            consoleRunner.addArguments("--root", hostFs + rootDirectory);
            consoleRunner.addArguments("--add");
            consoleRunner.addArguments(userName);
            consoleRunner.addArguments(groupName);
            boolean added = consoleRunner.execute() == 0;
            if (added) {
                logger.info("[GROUP] Member {} was added in group {}", userName, groupName);
            } else {
                logger.error("[GROUP] Member {} failed to be added in group {}", userName, groupName);
                throw new UtilsException("[GROUP] Member [" + userName + "] failed to be added in group [" + groupName + "]");
            }
            return added;
        }

        return false;
    }

    @Override
    public boolean groupCreate(String groupName) {
        logger.info("[GROUP] Creating {}", groupName);
        if (groupExists(groupName)) {
            logger.info("[GROUP] {} already exists", groupName);
        } else {
            ConsoleRunner consoleRunner = new ConsoleRunner();
            consoleRunner.setCommand("groupadd");
            consoleRunner.addArguments("--root", hostFs + rootDirectory);
            consoleRunner.addArguments(groupName);
            boolean created = consoleRunner.execute() == 0;
            if (created) {
                logger.info("[GROUP] {} was created", groupName);
            } else {
                logger.error("[GROUP] {} failed to be created", groupName);
                throw new UtilsException("[GROUP] [" + groupName + "] failed to be created");
            }
            return created;
        }
        return false;
    }

    @Override
    public boolean groupDelete(String groupName) {
        logger.info("[GROUP] Deleting {}", groupName);
        if (groupExists(groupName)) {
            // Delete
            ConsoleRunner consoleRunner = new ConsoleRunner();
            consoleRunner.setCommand("groupdel");
            consoleRunner.addArguments("--root", hostFs + rootDirectory);
            consoleRunner.addArguments(groupName);
            boolean deleted = consoleRunner.execute() == 0;
            if (deleted) {
                logger.info("[GROUP] {} was deleted", groupName);
            } else {
                logger.error("[GROUP] {} failed to be deleted", groupName);
                throw new UtilsException("[GROUP] [" + groupName + "] failed to be deleted");
            }
            return deleted;
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
    public boolean groupMemberIn(String groupName, String userName) {
        return groupMemberIn(hostFs + etcDirectory + "group", groupName, userName);
    }

    protected boolean groupMemberIn(String groupFile, String groupName, String userName) {
        String start = groupName + ":";

        for (String it : FileTools.readFileLinesIteration(groupFile)) {
            if (it.startsWith(start)) {
                String[] parts = it.split(":");
                String[] members = parts[3].split(",");
                return Arrays.asList(members).contains(userName);
            }
        }

        return false;
    }

    @Override
    public void groupNameUpdate(String oldGroupName, String newGroupName) {
        unixShellAndFsUtils.executeCommandQuiet("GROUP", "Update name", "groupmod", //
                "--root", hostFs + rootDirectory, //
                "--new-name", newGroupName, //
                oldGroupName);
    }

    public UnixUsersAndGroupsUtilsImpl setEtcDirectory(String etcDirectory) {
        this.etcDirectory = DirectoryTools.pathTrailingSlash(etcDirectory);
        return this;
    }

    public UnixUsersAndGroupsUtilsImpl setPasswdFile(String passwdFile) {
        this.passwdFile = passwdFile;
        return this;
    }

    public UnixUsersAndGroupsUtilsImpl setRootDirectory(String rootDirectory) {
        this.rootDirectory = DirectoryTools.pathTrailingSlash(rootDirectory);
        return this;
    }

    public UnixUsersAndGroupsUtilsImpl setShadowFile(String shadowFile) {
        this.shadowFile = shadowFile;
        return this;
    }

    public UnixUsersAndGroupsUtilsImpl setSudoDirectory(String sudoDirectory) {
        this.sudoDirectory = DirectoryTools.pathTrailingSlash(sudoDirectory);
        return this;
    }

    public void setUnixShellAndFsUtils(UnixShellAndFsUtils unixShellAndFsUtils) {
        this.unixShellAndFsUtils = unixShellAndFsUtils;
    }

    @Override
    public boolean userCreate(String username, Integer id, String homeFolder, String shell, String hashedPassword) {
        return userCreate(username, id, homeFolder, shell, hashedPassword, null);
    }

    @Override
    public boolean userCreate(String username, Integer id, String homeFolder, String shell, String hashedPassword, String sudoFileContent) {
        logger.info("[USER] Creating {} with id {} , home {} , shell {} and hashed password {}", username, id, homeFolder, shell, hashedPassword);

        boolean shellSet = !Strings.isNullOrEmpty(shell);
        boolean homeFolderSet = !Strings.isNullOrEmpty(homeFolder);
        if (sudoFileContent == null) {
            sudoFileContent = "";
        }

        boolean createdOrUpdated = false;

        UnixUserDetail currentUserDetail = userGet(username);

        // Create if missing
        if (currentUserDetail == null) {
            ConsoleRunner consoleRunner = new ConsoleRunner();
            consoleRunner.setCommand("useradd");
            consoleRunner.addArguments("--root", hostFs + rootDirectory);
            consoleRunner.addArguments("--uid");
            consoleRunner.addArguments(id.toString());
            if (homeFolderSet) {
                consoleRunner.addArguments("--home-dir");
                consoleRunner.addArguments(homeFolder);
                consoleRunner.addArguments("--create-home");
            }
            if (shellSet) {
                consoleRunner.addArguments("--shell");
                consoleRunner.addArguments(shell);
            }
            consoleRunner.addArguments(username);
            boolean created = consoleRunner.execute() == 0;
            if (!created) {
                logger.error("[USER] {} failed to be created", username);
                throw new UtilsException("[USER] [" + username + "] failed to be created");
            }
            createdOrUpdated = true;
            logger.info("[USER] {} was created", username);

            // Set the initial password
            userPasswordUpdate(username, hashedPassword);

        } else {

            logger.info("[USER] {} already exists", username);

            // name changed
            if (!currentUserDetail.getName().equals(username)) {
                userNameUpdate(currentUserDetail.getName(), username);
                groupNameUpdate(currentUserDetail.getName(), username);
                unixShellAndFsUtils.fileDelete(hostFs + sudoDirectory + currentUserDetail.getName());
                createdOrUpdated = true;
            }

            // homeFolder changed
            if (!StringTools.safeEquals(currentUserDetail.getHomeFolder(), homeFolder)) {
                userHomeUpdate(currentUserDetail.getName(), homeFolder);
                createdOrUpdated = true;
            }

            // shell changed
            if (!StringTools.safeEquals(currentUserDetail.getShell(), shell)) {
                userShellUpdate(username, shell);
                createdOrUpdated = true;
            }

            // password changed
            if (!StringTools.safeEquals(currentUserDetail.getHashedPassword(), hashedPassword)) {
                userPasswordUpdate(username, hashedPassword);
                createdOrUpdated = true;
            }
        }

        // Create the home folder
        if (homeFolderSet) {
            unixShellAndFsUtils.folderCreate(hostFs + rootDirectory + homeFolder, id, id, "750");
        }

        // Sudo
        createdOrUpdated |= unixShellAndFsUtils.fileInstall(hostFs + sudoDirectory + username, sudoFileContent, //
                0, 0, //
                "600");

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

        List<UnixUserDetail> users = userGetAll().stream().filter(it -> username.equals(it.getName())).collect(Collectors.toList());
        if (users.isEmpty()) {
            return null;
        }
        return users.get(0);
    }

    @Override
    public List<UnixUserDetail> userGetAll() {

        Map<String, UnixUserDetail> detailsByUsername = new HashMap<>();

        // Read the passwd file
        for (String line : FileTools.readFileLinesIteration(hostFs + passwdFile)) {
            String parts[] = line.split(":");
            if (parts.length < 6) {
                continue;
            }
            UnixUserDetail unixUserDetails = CollectionsTools.getOrCreateEmpty(detailsByUsername, parts[0], UnixUserDetail.class);
            int i = 0;
            unixUserDetails.setName(parts[i++]);
            ++i;
            unixUserDetails.setId(Integer.valueOf(parts[i++]));
            i += 2;
            unixUserDetails.setHomeFolder(parts[i++]);
            if (parts.length >= 7) {
                unixUserDetails.setShell(parts[i++]);
            }
        }

        // Read the shadow file
        for (String line : FileTools.readFileLinesIteration(hostFs + shadowFile)) {
            String parts[] = line.split(":");
            if (parts.length < 3) {
                continue;
            }
            UnixUserDetail unixUserDetails = CollectionsTools.getOrCreateEmpty(detailsByUsername, parts[0], UnixUserDetail.class);
            int i = 0;
            unixUserDetails.setName(parts[i++]);
            unixUserDetails.setHashedPassword(parts[i++]);
        }

        // Read the sudo files
        for (UnixUserDetail unixUserDetails : detailsByUsername.values()) {
            try {
                for (String line : FileTools.readFileLinesIteration(new File(hostFs + sudoDirectory + unixUserDetails.getName()))) {
                    String parts[] = line.split(":", 2);
                    if (parts.length != 2) {
                        continue;
                    }

                    String command = parts[1].replaceAll("\\\\", "").trim();
                    unixUserDetails.getSudos().add(command);
                }
            } catch (FileNotFoundException e) {
            }
        }

        return detailsByUsername.values().stream() //
                .filter(it -> it.getId() != null) // Must have an ID
                .sorted((a, b) -> Integer.compare(a.getId(), b.getId())) // Sort by ID
                .collect(Collectors.toList());
    }

    @Override
    public void userHomeUpdate(String username, String newHomeFolder) {
        // if new is null, delete old, don't move and change
        if (newHomeFolder == null) {
            unixShellAndFsUtils.executeCommandQuiet("USER", "Update home", "usermod", //
                    "--root", hostFs + rootDirectory, //
                    "--home", "/nonexistent", //
                    username);
        } else {
            unixShellAndFsUtils.executeCommandQuiet("USER", "Update home", "usermod", //
                    "--root", hostFs + rootDirectory, //
                    "--move-home", "--home", newHomeFolder, //
                    username);
        }
    }

    @Override
    public void userNameUpdate(String oldUsername, String newUsername) {
        unixShellAndFsUtils.executeCommandQuiet("USER", "Update name", "usermod", //
                "--root", hostFs + rootDirectory, //
                "--login", newUsername, //
                oldUsername);
    }

    @Override
    public boolean userPasswordUpdate(String username, String hashedPassword) {
        logger.info("[USER PASSWORD] Updating password of {} for hashed password {}", username, hashedPassword);
        if (!userExists(username)) {
            logger.error("[USER PASSWORD] User {} does not exists", username);
            throw new UtilsException("[USER PASSWORD] User [" + username + "] does not exists");
        }
        String line = username + ":" + hashedPassword;
        ConsoleRunner consoleRunner = new ConsoleRunner();
        consoleRunner.setCommand("chpasswd");
        consoleRunner.addArguments("--root", hostFs + rootDirectory);
        consoleRunner.addArguments("--encrypted");
        consoleRunner.setConsoleInput(new ByteArrayInputStream(line.getBytes(CharsetTools.UTF_8)));
        boolean success = consoleRunner.execute() == 0;
        if (success) {
            logger.info("[USER PASSWORD] User {} was updated", username);
        } else {
            logger.error("[USER PASSWORD] User {} failed to be updated", username);
            throw new UtilsException("[USER PASSWORD] User [" + username + "] failed to be updated");
        }

        return success;

    }

    @Override
    public boolean userRemove(String username) {
        logger.info("[USER] Deleting {}", username);
        if (userExists(username)) {

            // Delete
            ConsoleRunner consoleRunner = new ConsoleRunner();
            consoleRunner.setCommand("userdel");
            consoleRunner.addArguments("--root", hostFs + rootDirectory);
            consoleRunner.addArguments("--remove");
            consoleRunner.addArguments(username);
            boolean deleted = consoleRunner.execute() == 0;
            if (deleted) {
                logger.info("[USER] {} was deleted", username);
                groupDelete(username);
            } else {
                logger.error("[USER] {} failed to be deleted", username);
                throw new UtilsException("[USER] [" + username + "] failed to be deleted");
            }
            return deleted;
        } else {
            logger.info("[USER] {} already deleted", username);
        }

        unixShellAndFsUtils.fileDelete(hostFs + sudoDirectory + username);

        return true;
    }

    @Override
    public void userShellUpdate(String username, String newShell) {
        unixShellAndFsUtils.executeCommandQuiet("USER", "Update shell", "usermod", //
                "--root", hostFs + rootDirectory, //
                "--shell", newShell, //
                username);
    }

}
