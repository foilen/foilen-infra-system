#!/bin/bash

${command}
EXIT_CODE=$?

# Save the exit code if not already present
if [ ! -f /_infra/exitcode ]; then
  echo $EXIT_CODE > /_infra/exitcode
fi
