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
import com.foilen.infra.plugin.system.utils.UtilsException;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.DirectoryTools;
import com.foilen.smalltools.tools.FileTools;
import com.google.common.base.Strings;

public class UnixShellAndFsUtilsTrackPermissionsMock extends AbstractBasics implements UnixShellAndFsUtils {

    private Map<String, String> permissionsByPath = new HashMap<>();

    @Override
    public void executeCommandOrFail(Level loggerLevel, String actionDetails, String command, String... arguments) {
        throw new RuntimeException("Mock: Not implemented");
    }

    @Override
    public void executeCommandOrFail(String actionDetails, String command, String... arguments) {
        throw new RuntimeException("Mock: Not implemented");

    }

    @Override
    public void executeCommandOrFailWithWorkDir(Level loggerLevel, String actionDetails, String workingDirectory, String command, String... arguments) {
        throw new RuntimeException("Mock: Not implemented");

    }

    @Override
    public void executeCommandQuiet(String actionName, String actionDetails, String command, String... arguments) {
        throw new RuntimeException("Mock: Not implemented");

    }

    @Override
    public String executeCommandQuietAndGetOutput(String actionName, String actionDetails, String command, String... arguments) {
        throw new RuntimeException("Mock: Not implemented");
    }

    @Override
    public void fileChangeOwner(String path, long owner, long group) {
        logger.info("[{}] Changing ownership {} {}", path, owner, group);
        permissionsByPath.put(removeDoubleSlashes(path), owner + "/" + group);
    }

    @Override
    public void fileChangeOwnerAndPermissions(String path, long owner, long group, String permission) {
        throw new RuntimeException("Mock: Not implemented");

    }

    @Override
    public boolean fileDelete(String... fileNames) {
        throw new RuntimeException("Mock: Not implemented");
    }

    @Override
    public boolean fileInstall(String path, String content, long owner, long group, String permission) {
        logger.info("[FILE] Installing {}", path);
        try {
            boolean changed = FileTools.writeFileWithContentCheck(path, content);
            if (changed) {
                logger.info("[FILE] {} was modified", path);
            } else {
                logger.info("[FILE] {} stayed the same", path);
            }
            FileTools.changePermissions(path, false, permission);
            permissionsByPath.put(removeDoubleSlashes(path), permission + " " + owner + "/" + group);
            return changed;
        } catch (Exception e) {
            logger.error("[FILE] [{}] could not be written to", path, e);
            throw new UtilsException("[FILE] [" + path + "] could not be written to", e);
        }
    }

    @Override
    public boolean fileInstall(String[] pathParts, String content, long owner, long group, String permission) {
        throw new RuntimeException("Mock: Not implemented");
    }

    @Override
    public boolean fileInstallQuiet(String actionName, String path, String content, long owner, long group, String permissions) {
        throw new RuntimeException("Mock: Not implemented");
    }

    @Override
    public void folderCreate(String directoryPath, Long owner, Long group, String permission) {
        logger.info("[FOLDER] Creating directory {}", directoryPath);
        if (!DirectoryTools.createPath(directoryPath)) {
            throw new UtilsException("[FOLDER] [" + directoryPath + "] Could not be created");
        }
        if (!Strings.isNullOrEmpty(permission) || (owner != null && group != null)) {
            permissionsByPath.put(new File(directoryPath).getAbsolutePath() + "/", permission + " " + owner + "/" + group);
        }
    }

    @Override
    public void folderCreate(String[] directoryPathParts, Long owner, Long group, String permission) {
        throw new RuntimeException("Mock: Not implemented");

    }

    @Override
    public void folderCreateWithParentOwnerAndGroup(String directoryPath) {
        throw new RuntimeException("Mock: Not implemented");

    }

    @Override
    public boolean folderDelete(String directoryPath) {
        throw new RuntimeException("Mock: Not implemented");
    }

    @Override
    public boolean folderExists(String directoryPath) {
        throw new RuntimeException("Mock: Not implemented");
    }

    public Map<String, String> getPermissionsByPath() {
        return permissionsByPath;
    }

    @Override
    public boolean linkCreate(String link, String target) {
        throw new RuntimeException("Mock: Not implemented");
    }

    private String removeDoubleSlashes(String path) {
        return path.replace("//", "/");
    }

    @Override
    public boolean resourceInstall(String resourceName, String installPath, long owner, long group, String permission) {
        throw new RuntimeException("Mock: Not implemented");
    }

    @Override
    public boolean templateInstall(String templateName, String installPath, long owner, long group, String permission, Map<String, ?> model) {
        throw new RuntimeException("Mock: Not implemented");
    }

}
