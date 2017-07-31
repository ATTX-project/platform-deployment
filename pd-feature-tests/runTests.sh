#!/bin/sh

# Wait for MySQL, the big number is because CI is slow.
dockerize -wait tcp://mysql:3306 -timeout 240s
dockerize -wait http://fuseki:3030 -timeout 60s
dockerize -wait http://gmapi:4302/health -timeout 60s
dockerize -wait http://wfapi:4301/health -timeout 60s
dockerize -wait http://essiren:9200 -timeout 60s
dockerize -wait tcp://essiren:9300 -timeout 60s
dockerize -wait http://es5:9210 -timeout 60s
# wait for es5 9310 apparently not working
# dockerize -wait tcp://es5:9310 -timeout 60s

echo  "Archiva repository URL: $REPO"

gradle -PartifactRepoURL=$REPO integTest
