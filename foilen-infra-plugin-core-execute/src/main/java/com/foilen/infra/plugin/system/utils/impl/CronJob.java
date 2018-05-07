/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.system.utils.impl;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.foilen.infra.plugin.system.utils.DockerUtils;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.outputter.docker.DockerContainerOutputContext;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.JsonTools;

public class CronJob extends AbstractBasics implements Job {

    protected static DockerUtils dockerUtils;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String containerName = context.getJobDetail().getKey().getName();
        IPApplicationDefinition applicationDefinition = JsonTools.readFromString((String) context.get("applicationDefinition"), IPApplicationDefinition.class);
        DockerContainerOutputContext ctx = JsonTools.readFromString((String) context.get("dockerContainerOutputContext"), DockerContainerOutputContext.class);

        if (dockerUtils.containerIsRunningByContainerNameOrId(containerName)) {
            logger.info("[CRON] [{}] Container already running. Skipping", containerName);
            return;
        }

        logger.info("[CRON] [{}] Starting the container", containerName);
        dockerUtils.containerStartOnce(applicationDefinition, ctx);
    }

}
