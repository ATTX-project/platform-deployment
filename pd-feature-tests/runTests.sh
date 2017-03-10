#!/bin/sh

dockerize -wait tcp://mysql:3306 -timeout 120s
dockerize -wait http://frontend:8080 -timeout 120s
dockerize -wait http://fuseki:3030 -timeout 120s
# wait for healthcheck endpoint for gmapi and wfapi
# dockerize -wait http://gmapi:4302/ -timeout 120s
# dockerize -wait http://wfapi:4301/ -timeout 120s
dockerize -wait http://essiren:9200 -timeout 120s
dockerize -wait tcp://essiren:9300 -timeout 120s
# dockerize -wait http://es5:9210 -timeout 120s
# dockerize -wait tcp://es5:9310 -timeout 120s

gradle -b build.gradle --offline test
