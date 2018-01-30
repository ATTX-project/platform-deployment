# platform-deployment

This repository contains configurations for building, publishing, deploying and running ATTX components.

Content of the repository:
* ATTX component images
  * attx-es5
  * attx-fuseki
  * dc-elasticsearch-siren
  * gm-API
  * uv-attx-dpus
  * uv-attx-shared
  * wf-API
* Platform tests
  * pd-feature-tests
* Local VM deployment
  * swarm-mode-vagrant
* Cloud deployment
  * swarm-mode-cpouta


More detailed information can be found in (https://attx-project.github.io/).

## ATTX componens

Gradle configurations define two environments, dev (default) and release, which can be set with -Penv=[environment] parameter. common.gradle contains the main shared configuration for different environments, such as artifact and image tags and repository URLs.

## Running tests

* [Containerized testing](https://attx-project.github.io/Containerized-testing.html)

## Provisioning

* [OpenStack](https://attx-project.github.io/Provisioning-ATTX-Components-on-CSC-Open-Stack-cPouta.html)

## Deployment

* [SWARM in cloud](https://attx-project.github.io/Deploying-ATTX-Components-on-Docker-Swarm.html)
