#!/bin/bash

# Create users in /etc/passwd
if [ -f /cron_users.txt ]; then
	for i in $(cat /cron_users.txt); do
		USER_NAME=$(echo $i | cut -d':' -f1)
		USER_ID=$(echo $i | cut -d':' -f2)
		echo Creating user $USER_NAME with id $USER_ID
		useradd --no-create-home --non-unique --uid $USER_ID $USER_NAME
	done
fi

# Start cron and keep the pid
/usr/sbin/cron
CRON_PID=$(cat /var/run/crond.pid)

# Wait for the end
while [ -d /proc/$CRON_PID ]
do
  sleep 1
done
