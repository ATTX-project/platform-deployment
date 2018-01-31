# ATTX Linked Data Broker Platform Deployment

This repository contains configurations for building, publishing, deploying and running ATTX components.

Content of the repository:
* ATTX component images
  * attx-es5
  * attx-activemq
  * attx-fuseki
  * uv-elastichsearch with Siren plugin (not in use)
* ATTX services
  * graphManager https://github.com/ATTX-project/graphmanager-service
  * graphFraming (JSON-LD framing) https://github.com/ATTX-project/graphframing-service
  * provenance https://github.com/ATTX-project/provenance-service
  * indexing (in Elasticsearch 5.x) https://github.com/ATTX-project/indexing-service
  * RML https://github.com/ATTX-project/rml-service
  * UVprov (UnifiedViews provenance) https://github.com/ATTX-project/uv-provenance-service
  * ontology https://github.com/ATTX-project/ontology-service
  * validation https://github.com/ATTX-project/validation-service
* Local VM deployment
  * swarm-mode-vagrant
  * attx-satack-dcompose
* Cloud deployment
  * attx-stack-kontena
  * swarm-mode-cpouta
  * swarm-mode-docker-machine

More detailed information can be found in (https://attx-project.github.io/)

## ATTX componens

Gradle configurations define two environments, dev (default) and release, which can be set with -Penv=[environment] parameter. common.gradle contains the main shared configuration for different environments, such as artifact and image tags and repository URLs.

## Running tests

* [Containerized testing](https://attx-project.github.io/Containerized-testing.html)

## Provisioning

* [OpenStack](https://attx-project.github.io/Provisioning-ATTX-Components-on-CSC-Open-Stack-cPouta.html)

## Deployment

* [SWARM in cloud](https://attx-project.github.io/Deploying-ATTX-Components-on-Docker-Swarm.html)
