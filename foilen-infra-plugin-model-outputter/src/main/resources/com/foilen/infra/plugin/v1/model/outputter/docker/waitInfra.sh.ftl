#!/bin/bash

echo Waiting for infra

<#list portsRedirect as portRedirect>
echo -n 'Waiting for port ${portRedirect.localPort?c} (HEX: ${portRedirect.localPortHex}) '
until egrep ':${portRedirect.localPortHex} ' -q /proc/net/tcp*
do
  echo -n .
  sleep 1
done
echo ' READY'

</#list>

echo Starting the command
${command}
EXIT_CODE=$?

# Save the exit code if not already present
if [ ! -f /_infra/exitcode ]; then
  echo $EXIT_CODE > /_infra/exitcode
fi
