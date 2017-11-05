#!/bin/bash

# Delete old exit code if any
rm -f /_infra/exitcode

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
