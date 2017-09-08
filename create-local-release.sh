#!/bin/bash

set -e

# Set environment
export LANG="C.UTF-8"
export VERSION=$1

RUN_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $RUN_PATH

echo ----==[ Update copyrights ]==----
./scripts/javaheaderchanger.sh > /dev/null

echo ----==[ Compile and deploy locally ]==----
./gradlew clean install
