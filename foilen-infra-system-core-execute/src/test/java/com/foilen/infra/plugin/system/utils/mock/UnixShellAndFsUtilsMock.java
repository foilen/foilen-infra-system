/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.mock;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.event.Level;

import com.foilen.infra.plugin.system.utils.UnixShellAndFsUtils;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.DirectoryTools;
import com.foilen.smalltools.tools.FileTools;

public class UnixShellAndFsUtilsMock extends AbstractBasics implements UnixShellAndFsUtils {

    public static interface ExecuteCommandQuietAndGetOutputCallback {
        String handle(String actionName, String actionDetails, String command, String[] arguments);
    }

    private ExecuteCommandQuietAndGetOutputCallback executeCommandQuietAndGetOutputCallback = ((actionName, actionDetails, command, arguments) -> "");

    private Map<String, String> fileContentByPath = new HashMap<String, String>();

    @Override
    public void executeCommandOrFail(Level loggerLevel, String actionDetails, String command, String... arguments) {
        logger.debug("executeCommandOrFail: {} ; {} ; {} ; {}", loggerLevel, actionDetails, command, arguments);

        if (arguments.length >= 3) {
            String dockerCommand = arguments[0];
            if ("cp".equals(dockerCommand)) {
                String localPath = arguments[1];
                String dockerPath = arguments[2];
                if (new File(localPath).isDirectory()) {
                    logger.debug("Keeping track of copied directory {}", dockerPath);
                    DirectoryTools.visitFilesAndFoldersRecursively(localPath, file -> {
                        if (!file.isDirectory()) {
                            String subDockerPath = dockerPath + "/" + file.getName();
                            subDockerPath = subDockerPath.replaceAll("//", "/");
                            logger.debug("Keeping track of copied file {}", subDockerPath);
                            fileContentByPath.put(subDockerPath, FileTools.getFileAsString(file));
                        }
                    });
                } else {
                    logger.debug("Keeping track of copied file {}", dockerPath);
                    fileContentByPath.put(dockerPath, FileTools.getFileAsString(localPath));
                }
            }
        }
    }

    @Override
    public void executeCommandOrFail(String actionDetails, String command, String... arguments) {
        logger.debug("executeCommandOrFail: {} ; {} ; {}", actionDetails, command, arguments);
    }

    @Override
    public void executeCommandOrFailWithWorkDir(Level loggerLevel, String actionDetails, String workingDirectory, String command, String... arguments) {
        logger.debug("executeCommandOrFailWithWorkDir: {} ; {} ; {} ; {} ; {}", loggerLevel, actionDetails, workingDirectory, command, arguments);
    }

    @Override
    public void executeCommandQuiet(String actionName, String actionDetails, String command, String... arguments) {
        logger.debug("executeCommandQuiet: {} ; {} ; {}", actionDetails, command, arguments);
    }

    @Override
    public String executeCommandQuietAndGetOutput(String actionName, String actionDetails, String command, String... arguments) {
        logger.debug("executeCommandQuietAndGetOutput: {} ; {} ; {} ; {}", actionName, actionDetails, command, arguments);
        return executeCommandQuietAndGetOutputCallback.handle(actionName, actionDetails, command, arguments);
    }

    @Override
    public void fileChangeOwner(String path, long owner, long group) {
        logger.debug("fileChangeOwner: {} ; {} ; {}}", path, owner, group);
    }

    @Override
    public void fileChangeOwnerAndPermissions(String path, long owner, long group, String permission) {
        logger.debug("fileChangeOwnerAndPermissions: {} ; {} ; {} ; {}", path, owner, group, permission);
    }

    @Override
    public boolean fileDelete(String... fileNames) {
        logger.debug("fileDelete: {}", (Object[]) fileNames);
        return true;
    }

    @Override
    public boolean fileInstall(String path, String content, long owner, long group, String permission) {
        logger.debug("fileInstall: {} ; {} ; {} ; {} ; {}", path, owner, group, permission, content);
        return true;
    }

    @Override
    public boolean fileInstall(String[] pathParts, String content, long owner, long group, String permission) {
        logger.debug("fileInstall: {} ; {} ; {} ; {} ; {}", pathParts, owner, group, permission, content);
        return true;
    }

    @Override
    public boolean fileInstallQuiet(String actionName, String path, String content, long owner, long group, String permissions) {
        logger.debug("fileInstallQuiet: {} ; {} ; {} ; {} ; {} ; {}", actionName, path, owner, group, permissions, content);
        return true;
    }

    @Override
    public void folderCreate(String directoryPath, Long owner, Long group, String permission) {
        logger.debug("folderCreate: {} ; {} ; {} ; {}", directoryPath, owner, group, permission);
    }

    @Override
    public void folderCreate(String[] directoryPathParts, Long owner, Long group, String permission) {
        logger.debug("folderCreate: {} ; {} ; {} ; {}", directoryPathParts, owner, group, permission);
    }

    @Override
    public void folderCreateWithParentOwnerAndGroup(String directoryPath) {
        logger.debug("folderCreateWithParentOwnerAndGroup: {}", directoryPath);

    }

    @Override
    public boolean folderDelete(String directoryPath) {
        logger.debug("folderDelete: {}", directoryPath);
        return true;
    }

    @Override
    public boolean folderExists(String directoryPath) {
        logger.debug("folderExists: {}", directoryPath);
        return true;
    }

    public ExecuteCommandQuietAndGetOutputCallback getExecuteCommandQuietAndGetOutputCallback() {
        return executeCommandQuietAndGetOutputCallback;
    }

    public Map<String, String> getFileContentByPath() {
        return fileContentByPath;
    }

    @Override
    public boolean linkCreate(String link, String target) {
        logger.debug("linkCreate: {} ; {}", link, target);
        return true;
    }

    @Override
    public boolean resourceInstall(String resourceName, String installPath, long owner, long group, String permission) {
        logger.debug("resourceInstall: {} ; {} ; {} ; {} ; {}", resourceName, installPath, owner, group, permission);
        return true;
    }

    public void setExecuteCommandQuietAndGetOutputCallback(ExecuteCommandQuietAndGetOutputCallback executeCommandQuietAndGetOutputCallback) {
        this.executeCommandQuietAndGetOutputCallback = executeCommandQuietAndGetOutputCallback;
    }

    @Override
    public boolean templateInstall(String templateName, String installPath, long owner, long group, String permission, Map<String, ?> model) {
        logger.debug("templateName: {} ; {} ; {} ; {} ; {}", templateName, installPath, owner, group, permission);
        return true;
    }

}
