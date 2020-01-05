#!/bin/bash

set -e

docker run --rm --detach \
  --publish 27085:27017 \
  --name infra-system-mongodb \
   mongo:4.2.2 --replSet rs

sleep 5s

docker exec -i infra-system-mongodb mongo << _EOF
	rs.initiate()
_EOF

echo
echo MongoDB well started
