# platform-deployment
Configuration and tools for deploying the PAAS Solution based on ATTX components to different environments.

The ATTX PAAS stack is deployed as a Docker Compose app, and thus is requires that Docker Compose is available beforehand in your environment (cf. https://docs.docker.com/compose/install/).

# How to start the ATTX stack
1. Download docker-compose.yml or docker-compose.prod.yml
2. Default scenario (no data persistancy):
    `$ docker-compose up`
3. Alternate scenario (data persistancy):
    `$ docker-compose -f docker-compose.prod.yml up`
