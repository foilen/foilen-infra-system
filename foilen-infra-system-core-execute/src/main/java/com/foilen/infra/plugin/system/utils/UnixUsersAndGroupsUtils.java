/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils;

import java.util.List;

import com.foilen.infra.plugin.system.utils.model.UnixUserDetail;

public interface UnixUsersAndGroupsUtils {

    /**
     * Add a member to the unix groupo.
     *
     * @param groupName
     *            the name of the group
     * @param userName
     *            the name of the user to add
     * @return true if wasn't present and then added
     */
    boolean groupAddMember(String groupName, String userName);

    /**
     * Create a unix group.
     *
     * @param groupName
     *            the name of the group
     * @return true if created
     */
    boolean groupCreate(String groupName);

    /**
     * Delete a unix group.
     *
     * @param groupName
     *            the name of the group
     * @return true if was deleted
     */
    boolean groupDelete(String groupName);

    /**
     * Tells if the unix group exists.
     *
     * @param groupName
     *            the name of the group
     * @return true if the group exists
     */
    boolean groupExists(String groupName);

    /**
     * Tells if the user is already part of the unix group.
     *
     * @param groupName
     *            the name of the group
     * @param userName
     *            the name of the user
     * @return true if the user is part of the group
     */
    boolean groupMemberIn(String groupName, String userName);

    /**
     * Update a unix group name.
     *
     * @param oldGroupName
     *            the old name of the group
     * @param newGroupName
     *            the new name of the group
     */
    void groupNameUpdate(String oldGroupName, String newGroupName);

    /**
     * Create a unix user or update its information.
     *
     * @param username
     *            the name of the user
     * @param id
     *            the id of the user
     * @param homeFolder
     *            (optional) the full path to the home folder
     * @param shell
     *            (optional) the shell of the user
     * @param hashedPassword
     *            (optional) the already hashed password of the user
     * @return true if was created or updated
     */
    boolean userCreate(String username, Integer id, String homeFolder, String shell, String hashedPassword);

    /**
     * Create a unix user or update its information.
     *
     * @param username
     *            the name of the user
     * @param id
     *            the id of the user
     * @param homeFolder
     *            (optional) the full path to the home folder
     * @param shell
     *            (optional) the shell of the user
     * @param hashedPassword
     *            (optional) the already hashed password of the user
     * @param sudoFileContent
     *            (optional) the content of the sudo file
     * @return true if was created or updated
     */
    boolean userCreate(String username, Integer id, String homeFolder, String shell, String hashedPassword, String sudoFileContent);

    /**
     * Tells if the unix user exists.
     *
     * @param username
     *            the name of the user
     * @return true if it exists
     */
    boolean userExists(String username);

    /**
     * Get the desired user details.
     *
     * @param username
     *            the name of the user
     * @return the user if present ; null otherwise
     */
    UnixUserDetail userGet(String username);

    /**
     * Get all the current users.
     *
     * @return the users in sorted by id order
     */
    List<UnixUserDetail> userGetAll();

    /**
     * Update a unix user home folder.
     *
     * @param username
     *            the name of the user
     * @param newHomeFolder
     *            the new home folder
     */
    void userHomeUpdate(String username, String newHomeFolder);

    /**
     * Update a unix user name.
     *
     * @param oldUsername
     *            the old name of the user
     * @param newUsername
     *            the new name of the user
     */
    void userNameUpdate(String oldUsername, String newUsername);

    /**
     * Update a unix user password.
     *
     * @param username
     *            the name of the user
     * @param hashedPassword
     *            the hashed version of the password
     * @return true if was changed
     */
    boolean userPasswordUpdate(String username, String hashedPassword);

    /**
     * Remove a unix user.
     *
     * @param username
     *            the name of the user
     * @return true if was removed
     */
    boolean userRemove(String username);

    /**
     * Update a unix user shell.
     *
     * @param username
     *            the name of the user
     * @param newShell
     *            the new shell
     */
    void userShellUpdate(String username, String newShell);

}
