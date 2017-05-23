#!/usr/bin/env bash

if [[ "$(uname -s )" == "Linux" ]]; then
  export VIRTUALBOX_SHARE_FOLDER="$PWD:$PWD"
fi

for i in 1 2 3; do
    docker-machine create \
        -d virtualbox \
        swarm-$i
done

eval $(docker-machine env swarm-1)

docker swarm init \
  --advertise-addr $(docker-machine ip swarm-1)

TOKEN=$(docker swarm join-token -q manager)

for i in 2 3; do
    eval $(docker-machine env swarm-$i)

    docker swarm join \
        --token $TOKEN \
        --advertise-addr $(docker-machine ip swarm-$i) \
        $(docker-machine ip swarm-1):2377
done

for i in 1 2 3; do
    eval $(docker-machine env swarm-$i)

    docker node update \
        --label-add env=prod \
        swarm-$i
done

echo ">> The swarm cluster is up and running"

eval $(docker-machine env swarm-1)

docker node ls

curl -o docker-compose-proxy.yml \
    https://raw.githubusercontent.com/\
vfarcic/docker-flow-proxy/master/docker-compose.yml



export DOCKER_IP=$(docker-machine ip swarm-1)

docker-compose -f docker-compose-proxy.yml \
    up -d consul-server

echo ">> The Consul server is up and running"

export CONSUL_SERVER_IP=$(docker-machine ip swarm-1)

for i in 2 3; do
    eval $(docker-machine env swarm-$i)

    export DOCKER_IP=$(docker-machine ip swarm-$i)

    docker-compose -f docker-compose-proxy.yml \
        up -d consul-agent
done

echo ">> The Consul agents are up and running"

eval $(docker-machine env swarm-1)

docker network create --driver overlay proxy

docker service create --name proxy \
    -p 80:80 \
    -p 443:443 \
    -p 8080:8080 \
    --network proxy \
    -e MODE=swarm \
    --replicas 3 \
    -e CONSUL_ADDRESS="$(docker-machine ip swarm-1):8500,$(docker-machine ip swarm-2):8500,$(docker-machine ip swarm-3):8500" \
    vfarcic/docker-flow-proxy

docker service ps proxy

echo ">> The proxy service is up and running"

docker network create --driver overlay backend

docker service create --name swarm-listener \
    --network proxy \
    --mount "type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock" \
    -e DF_NOTIFY_CREATE_SERVICE_URL=http://proxy:8080/v1/docker-flow-proxy/reconfigure \
    -e DF_NOTIFY_REMOVE_SERVICE_URL=http://proxy:8080/v1/docker-flow-proxy/remove \
    --constraint 'node.role==manager' \
    vfarcic/docker-flow-swarm-listener
    
docker service ps swarm-listener

echo ">> The swarm-listener service is up and running"

docker service create --name util \
    --network proxy --mode global \
    alpine sleep 1000000000

docker service ps util

ID=$(docker ps -q --filter label=com.docker.swarm.service.name=util)

docker exec -it $ID apk add --update drill

echo ">> The DNS drill util service is up and running"



