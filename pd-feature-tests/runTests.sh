#!/bin/sh

: ${SLEEP_LENGTH:=2}

wait_for() {
  echo Waiting for $1 to listen on $2... >> /tmp/log  
  while ! nc -z $1 $2; do echo sleeping >> /tmp/log ; sleep $SLEEP_LENGTH; done
}



wait_for "fuseki" "3030"
wait_for "essiren" "9200"
wait_for "essiren" "9300"
wait_for "gmapi" "4302"
wait_for "wfapi" "4301"
wait for "frontend" "8080"

gradle -b /tmp/build.test.gradle --offline test

