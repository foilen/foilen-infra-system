/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2019 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.event.Level;

import com.foilen.infra.plugin.system.utils.UnixShellAndFsUtils;
import com.foilen.infra.plugin.system.utils.UtilsException;
import com.foilen.smalltools.consolerunner.ConsoleRunner;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.DirectoryTools;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.FreemarkerTools;
import com.foilen.smalltools.tools.ResourceTools;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;

public class UnixShellAndFsUtilsImpl extends AbstractBasics implements UnixShellAndFsUtils {

    private static final Joiner PATH_JOINER = Joiner.on('/');

    @Override
    public void executeCommandOrFail(Level loggerLevel, String actionDetails, String command, String... arguments) {
        logger.info("[EXECUTE] {} : {}", actionDetails, command);

        ConsoleRunner consoleRunner = new ConsoleRunner().setRedirectErrorStream(true) //
                .setCommand(command).addArguments(arguments);
        consoleRunner.executeWithLogger(logger, loggerLevel);
        int status = consoleRunner.getStatusCode();
        if (status == 0) {
            logger.info("[EXECUTE] command for [{}] was successful", actionDetails);
        } else {
            logger.error("[EXECUTE] command for [{}] failed", actionDetails);
            throw new UtilsException("[EXECUTE] action [" + actionDetails + "] with command [" + command + "] failed. Return code [" + status + "]");
        }
    }

    @Override
    public void executeCommandOrFail(String actionDetails, String command, String... arguments) {
        logger.info("[EXECUTE] {} : {}", actionDetails, command);

        ConsoleRunner consoleRunner = new ConsoleRunner().setRedirectErrorStream(true) //
                .setCommand(command).addArguments(arguments);
        consoleRunner.executeWithLogger(logger, Level.INFO);
        int status = consoleRunner.getStatusCode();
        if (status == 0) {
            logger.info("[EXECUTE] command for [{}] was successful", actionDetails);
        } else {
            logger.error("[EXECUTE] command for [{}] failed", actionDetails);
            throw new UtilsException("[EXECUTE] action [" + actionDetails + "] with command [" + command + "] failed. Return code [" + status + "]");
        }
    }

    @Override
    public void executeCommandOrFailWithWorkDir(Level loggerLevel, String actionDetails, String workingDirectory, String command, String... arguments) {
        logger.info("[EXECUTE] {} : {}", actionDetails, command);

        ConsoleRunner consoleRunner = new ConsoleRunner().setRedirectErrorStream(true) //
                .setCommand(command).addArguments(arguments) //
                .setWorkingDirectory(workingDirectory);
        consoleRunner.executeWithLogger(logger, loggerLevel);
        int status = consoleRunner.getStatusCode();
        if (status == 0) {
            logger.info("[EXECUTE] command for [{}] was successful", actionDetails);
        } else {
            logger.error("[EXECUTE] command for [{}] failed", actionDetails);
            throw new UtilsException("[EXECUTE] action [" + actionDetails + "] with command [" + command + "] failed. Return code [" + status + "]");
        }
    }

    @Override
    public void executeCommandQuiet(String actionName, String actionDetails, String command, String... arguments) {
        logger.info("[{}] {}", actionName, actionDetails);
        ConsoleRunner consoleRunner = new ConsoleRunner();
        consoleRunner.setCommand(command);
        consoleRunner.addArguments(arguments);
        boolean success = consoleRunner.execute() == 0;
        if (success) {
            logger.info("[{}] {} successfully", actionName, actionDetails);
        } else {
            logger.error("[{}] {} failed", actionName, actionDetails);
            throw new UtilsException("[" + actionName + "] " + actionDetails + " failed");
        }
    }

    @Override
    public String executeCommandQuietAndGetOutput(String actionName, String actionDetails, String command, String... arguments) {
        logger.info("[{}] {}", actionName, actionDetails);
        ConsoleRunner consoleRunner = new ConsoleRunner();
        consoleRunner.setCommand(command);
        consoleRunner.addArguments(arguments);
        String output = consoleRunner.executeForString();
        boolean success = consoleRunner.getStatusCode() == 0;
        if (success) {
            logger.info("[{}] {} successfully", actionName, actionDetails);
            return output;
        } else {
            logger.error("[{}] {} failed", actionName, actionDetails);
            throw new UtilsException("[" + actionName + "] " + actionDetails + " failed");
        }
    }

    @Override
    public void fileChangeOwnerAndPermissions(String path, long owner, long group, String permission) {
        logger.info("[{}] Changing ownership and permissions {} {} {}", path, owner, group, permission);
        executeCommandQuiet("FILE", "Update owner", "/bin/chown", owner + ":" + group, path);
        FileTools.changePermissions(path, false, permission);
    }

    @Override
    public boolean fileDelete(String... fileNames) {
        logger.info("[FILE] Deleting {}", (Object) fileNames);
        boolean result = false;
        for (String fileName : fileNames) {
            result |= new File(fileName).delete();
        }
        return result;
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
            executeCommandQuiet("FILE", "Update owner", "/bin/chown", owner + ":" + group, path);
            return changed;
        } catch (Exception e) {
            logger.error("[FILE] [{}] could not be written to", path, e);
            throw new UtilsException("[FILE] [" + path + "] could not be written to", e);
        }
    }

    @Override
    public boolean fileInstall(String[] pathParts, String content, long owner, long group, String permission) {
        return fileInstall(PATH_JOINER.join(pathParts), content, owner, group, permission);
    }

    @Override
    public boolean fileInstallQuiet(String actionName, String path, String content, long owner, long group, String permissions) {
        logger.info("[{}] Installing {}", actionName, path);
        try {
            boolean changed = FileTools.writeFileWithContentCheck(path, content);
            FileTools.changePermissions(path, false, permissions);
            executeCommandQuiet(actionName, "Update owner", "/bin/chown", owner + ":" + group, path);
            if (changed) {
                logger.info("[{}] {} was modified", actionName, path);
            } else {
                logger.info("[{}] {} stayed the same", actionName, path);
            }
            return changed;
        } catch (Exception e) {
            logger.error("[{}] [{}] could not be written to", actionName, path);
            throw new UtilsException("[" + actionName + "] [" + path + "] could not be written to", e);
        }
    }

    @Override
    public void folderCreate(String directoryPath, Long owner, Long group, String permission) {
        logger.info("[FOLDER] Creating directory {}", directoryPath);
        if (!DirectoryTools.createPath(directoryPath)) {
            throw new UtilsException("[FOLDER] [" + directoryPath + "] Could not be created");
        }
        if (!Strings.isNullOrEmpty(permission)) {
            FileTools.changePermissions(directoryPath, false, permission);
        }
        if (owner != null && group != null) {
            executeCommandQuiet("FOLDER", "Update owner", "/bin/chown", owner + ":" + group, directoryPath);
        }
    }

    @Override
    public void folderCreate(String[] directoryPathParts, Long owner, Long group, String permission) {
        folderCreate(PATH_JOINER.join(directoryPathParts), owner, group, permission);
    }

    @Override
    public boolean folderDelete(String directoryPath) {
        logger.info("[FOLDER] Deleting {}", directoryPath);
        if (FileTools.exists(directoryPath)) {
            DirectoryTools.deleteFolder(directoryPath);
            return !FileTools.exists(directoryPath);
        } else {
            return false;
        }
    }

    @Override
    public boolean folderExists(String directoryPath) {
        File file = new File(directoryPath);
        logger.debug("[FOLDER] Folder exists. Exists {}; is a folder {}", file.exists(), file.isDirectory());
        return file.exists() && file.isDirectory();
    }

    @Override
    public boolean linkCreate(String link, String target) {
        try {
            Path targetPath = new File(target).toPath();
            File linkFile = new File(link);
            Path linkPath = linkFile.toPath();
            if (linkFile.exists()) {
                if (Files.isSymbolicLink(linkPath)) {
                    Path current = Files.readSymbolicLink(linkPath);
                    if (!current.equals(targetPath)) {
                        logger.info("[LINK] Symbolic link {} does not have the right destination. Modifying it", link);
                        linkFile.delete();
                        Files.createSymbolicLink(linkPath, targetPath);
                        return true;
                    }
                } else {
                    logger.error("[LINK] Cannot create a symbolic link here {} since there is a file (not a link) that already exists", link);
                    throw new UtilsException("[LINK] Cannot create a symbolic link here [" + link + "] since there is a file (not a link) that already exists");
                }
            } else {
                logger.info("[LINK] Symbolic link {} does not exists. Creating it", link);
                Files.createSymbolicLink(linkPath, targetPath);
                return true;
            }
        } catch (UtilsException e) {
            throw e;
        } catch (Exception e) {
            throw new UtilsException("[LINK] Problem creating the link", e);
        }
        return false;
    }

    @Override
    public boolean resourceInstall(String resourceName, String installPath, long owner, long group, String permission) {
        try {
            return fileInstall(installPath, ResourceTools.getResourceAsString(resourceName), owner, group, permission);
        } catch (UtilsException e) {
            throw e;
        } catch (Exception e) {
            throw new UtilsException("[RESOURCE] [" + installPath + "] Could not be created with resource [" + resourceName + "]", e);
        }
    }

    @Override
    public boolean templateInstall(String templateName, String installPath, long owner, long group, String permission, Map<String, ?> model) {
        logger.info("[TEMPLATE] Installing {} to {}", templateName, installPath);
        String content = FreemarkerTools.processTemplate(templateName, model);
        return fileInstallQuiet("TEMPLATE", installPath, content, owner, group, permission);
    }

}
