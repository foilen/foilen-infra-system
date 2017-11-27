#!/bin/bash

# Delete old exit code if any
rm -f /_infra/exitcode

# Create any missing user in /etc/passwd
if [ -f /_infra/supervisord_users.txt ]; then
	for i in $(cat /_infra/supervisord_users.txt); do
		USER_NAME=$(echo $i | cut -d':' -f1)
		USER_ID=$(echo $i | cut -d':' -f2)
		if id $USER_ID ; then
			echo User with id $USER_ID already exists
		else
			echo User with id $USER_ID needs to be created
			useradd --uid $USER_ID $USER_NAME
	fi
	done
fi

# Start supervisor and keep the pid
/usr/bin/supervisord -c /_infra/supervisord.conf &
SUPERVISORD_PID=$!

# Wait for the end
while [ -d /proc/$SUPERVISORD_PID ]
do

  # Return the exit code if present
  if [ -f /_infra/exitcode ]; then
    exit $(cat /_infra/exitcode)
  fi

  sleep 1
done

# Return the exit code if present
if [ -f /_infra/exitcode ]; then
  exit $(cat /_infra/exitcode)
fi
