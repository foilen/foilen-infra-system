/*
    Foilen Infra System
    https://github.com/foilen/foilen-infra-system
    Copyright (c) 2017-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.app.test.docker;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foilen.smalltools.tools.AssertTools;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.LogbackTools;
import com.google.common.collect.ComparisonChain;

public class DownloadLatestPluginsApp {

    private static final Logger logger = LoggerFactory.getLogger(DownloadLatestPluginsApp.class);

    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("You need to provide a directory where to output all the plugins");
            System.out.println("Usage: outDir <pluginName1> <pluginName2> <pluginNameN>");
            System.exit(1);
        }

        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        // Check if debug mode
        boolean isDebug = false;
        boolean isInfo = false;
        if (arguments.remove("--debug")) {
            isDebug = true;
        }
        if (arguments.remove("--info")) {
            isInfo = true;
        }

        if (arguments.isEmpty()) {
            System.out.println("You need to provide a directory where to output all the plugins");
            System.out.println("Usage: outDir <pluginName1> <pluginName2> <pluginNameN>");
            System.exit(1);
        }

        if (isDebug) {
            LogbackTools.changeConfig("/logback-debug.xml");
        } else if (isInfo) {
            LogbackTools.changeConfig("/logback-info.xml");
        } else {
            LogbackTools.changeConfig("/logback-quiet.xml");
        }

        // Create the directory if missing
        String outputDirectoryName = arguments.get(0);
        File outputFolder = new File(outputDirectoryName);
        if (outputFolder.exists()) {
            if (!outputFolder.isDirectory()) {
                System.out.println("The path " + outputDirectoryName + " is not a directory");
                System.exit(1);
            }
        } else {
            if (!outputFolder.mkdir()) {
                System.out.println("Could not create " + outputDirectoryName + " directory");
                System.exit(1);
            }
        }
        String outDir = outputFolder.getAbsolutePath() + File.separator;

        // Download all plugins latest version
        System.out.println("Start downloading all plugins");
        for (int i = 1; i < arguments.size(); ++i) {
            String nextPlugin = arguments.get(i);
            System.out.println("Plugin: " + nextPlugin);

            try {
                if (FileTools.exists(outDir + "foilen-infra-plugins-" + nextPlugin + ".jar") || FileTools.exists(outDir + "foilen-infra-resource-" + nextPlugin + ".jar")) {
                    System.out.println("\tAlready got it. Skipping");
                    continue;
                }

                // Get the version
                boolean foundOne = false;
                for (String packageName : Arrays.asList("foilen-infra-plugins-" + nextPlugin, "foilen-infra-resource-" + nextPlugin)) {

                    if (foundOne) {
                        break;
                    }

                    logger.info("Searching for package {}", packageName);
                    try {
                        String jarDestination = outDir + packageName + ".jar";
                        Document doc = Jsoup.connect("https://repo1.maven.org/maven2/com/foilen/" + packageName + "/").get();
                        Elements links = doc.select("a");
                        String version = links.stream() //
                                .peek(it -> logger.info("Raw url {}", it.text())) //
                                .map(it -> it.text().replace("/", "")) //
                                .map(it -> it.split("\\.")) //
                                .filter(it -> it.length == 3) //
                                .map(it -> {
                                    try {
                                        return new int[] { Integer.valueOf(it[0]), Integer.valueOf(it[1]), Integer.valueOf(it[2]) };
                                    } catch (NumberFormatException e) {
                                        return null;
                                    }
                                }) //
                                .filter(it -> it != null) //
                                .peek(it -> logger.info("Possible Version {}", it)) //
                                .sorted((a, b) -> ComparisonChain.start() //
                                        .compare(b[0], a[0]) //
                                        .compare(b[1], a[1]) //
                                        .compare(b[2], a[2]) //
                                        .result()) //
                                .map(it -> "" + it[0] + "." + it[1] + "." + it[2]) //
                                .findFirst().get(); //

                        // Get the jar
                        String jarUrl = "https://repo1.maven.org/maven2/com/foilen/" + packageName + "/" + version + "/" + packageName + "-" + version + ".jar";
                        System.out.println("\tDownloading: " + jarUrl);

                        FileUtils.copyURLToFile(new URL(jarUrl), new File(jarDestination), 5000, 20000);
                        foundOne = true;
                    } catch (Exception e) {
                        logger.error("Problem", e);
                    }
                }

                AssertTools.assertTrue(foundOne, "Could not find plugin " + nextPlugin);
            } catch (Exception e) {
                System.err.println("Problem getting the plugin");
                e.printStackTrace();
                System.exit(1);
            }
        }

    }

}
