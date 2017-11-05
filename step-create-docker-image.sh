#!/bin/bash

set -e

RUN_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $RUN_PATH/foilen-infra-plugin-app-test-docker

echo ----[ Prepare folder for docker image ]----
DOCKER_BUILD=$RUN_PATH/build/docker

rm -rf $DOCKER_BUILD
mkdir -p $DOCKER_BUILD/app

cp -v build/distributions/foilen-infra-plugin-app-test-docker-$VERSION.zip $DOCKER_BUILD/app/foilen-infra-plugin-app-test-docker.zip
cp -v docker-release/* $DOCKER_BUILD

cd $DOCKER_BUILD/app
unzip foilen-infra-plugin-app-test-docker.zip
rm foilen-infra-plugin-app-test-docker.zip
mv foilen-infra-plugin-app-test-docker-$VERSION/* .
rm -rf foilen-infra-plugin-app-test-docker-$VERSION

echo ----[ Docker image folder content ]----
find $DOCKER_BUILD

echo ----[ Build docker image ]----
DOCKER_IMAGE=foilen-infra-plugin-app-test-docker:$VERSION
docker build -t $DOCKER_IMAGE $DOCKER_BUILD

rm -rf $DOCKER_BUILD
