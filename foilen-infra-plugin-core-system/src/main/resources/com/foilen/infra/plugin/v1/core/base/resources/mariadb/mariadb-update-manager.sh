#!/bin/bash

# Wait for 3306
echo Waiting for 3306
until egrep ':0CEA' -q /proc/net/tcp*
do
  echo -n .
  sleep 1
done

# Wait for root to be ready
echo Waiting for root user to be ready
until [ -f /var/lib/mysql/CAN_USE ]; do
	echo -n .
  sleep 1
done

# Execute
echo ' READY'
/usr/local/bin/mysql-manager 127.0.0.1:3306 /manager-config.json
