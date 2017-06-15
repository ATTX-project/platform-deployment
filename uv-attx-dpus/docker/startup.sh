#!/bin/bash

# Wait for MySQL
dockerize -wait tcp://mysql:3306 -timeout 240s

# Wait till Tomcat startup has finished and webapps are started (max 3 minutes)
if [ "$UV_PORT" != "" ] ; then
   FRONTENDPORT=$UV_PORT
else
   FRONTENDPORT=8080
fi

i=0
until $(curl --output /dev/null --silent --head --fail --user $MASTER_USER:$MASTER_PASSWORD http://frontend:$FRONTENDPORT/master/api/1/pipelines) || [ "$i" -gt 36 ]; do
    i=$((i+1))
    printf '.'
    sleep 5
done

# Add DPUs
for f in /dpus/*.jar; do bash /usr/local/bin/add-dpu.sh "$f"; done


# data_directory=/dpus/demo

# if [ -d "$data_directory" ]; then
#     # Add Pipelines
#     for f in /dpus/demo/*.zip; do bash /usr/local/bin/add-pipeline.sh "$f"; done
# fi
