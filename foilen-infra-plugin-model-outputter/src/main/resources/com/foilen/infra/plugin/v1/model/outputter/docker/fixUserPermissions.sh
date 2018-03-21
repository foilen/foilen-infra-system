#!/bin/bash

set -e

USER_NAME=$1
GROUP_NAME=$2
NEW_USER_ID=$3
NEW_GROUP_ID=$4

FIX_CONTAINER_USER_ID=$(id -u $USER_NAME)
FIX_CONTAINER_GROUP_ID=$(id -g $GROUP_NAME)

usermod -u $NEW_USER_ID $USER_NAME -o
groupmod -g $NEW_GROUP_ID $GROUP_NAME -o

for rootDir in $(ls); do
	if [ "$rootDir" == "boot" ]; then
		continue
	fi
	if [ "$rootDir" == "dev" ]; then
		continue
	fi
	if [ "$rootDir" == "proc" ]; then
		continue
	fi
	if [ "$rootDir" == "sys" ]; then
		continue
	fi
	
	find $rootDir -uid $FIX_CONTAINER_USER_ID -exec chown $USER_NAME {} \;
	find $rootDir -gid $FIX_CONTAINER_GROUP_ID -exec chgrp $GROUP_NAME {} \;
done
