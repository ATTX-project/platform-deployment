#!/bin/bash

# Wait for MySQL
: ${SLEEP_LENGTH:=2}

wait_for() {
  echo Waiting for $1 to listen on $2... >> /tmp/log
  while ! nc -z $1 $2; do echo sleeping >> /tmp/log ; sleep $SLEEP_LENGTH; done
}

wait_for "mysql" "3306"

# Wait till Tomcat startup has finished and webapps are started (max 3 minutes)
i=0
until $(curl --output /dev/null --silent --head --fail --user $MASTER_USER:$MASTER_PASSWORD http://frontend:8080/master/api/1/pipelines) || [ "$i" -gt 36 ]; do
    i=$((i+1))
    printf '.'
    sleep 5
done

# Add DPUs
for f in /dpus/*.jar; do bash /usr/local/bin/add-dpu.sh "$f"; done
