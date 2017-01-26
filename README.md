# platform-deployment
Configuration and tools for deploying the PAAS Solution based on ATTX components to different environments.

The ATTX PAAS stack is deployed as a Docker Compose app, and thus is requires that Docker Engine and Docker Compose are available beforehand in your environment (cf. https://docs.docker.com/engine/getstarted/step_one/ and https://docs.docker.com/compose/install/).

# How to deploy and start the ATTX platform stack manually
1. Download docker-compose.yml (no data persistency) or docker-compose.prod.yml (local data volume for persistency)
2. Default scenario (no data persistency):
    `$ docker-compose up`
3. Alternate scenario (data persistency):
    `$ docker-compose -f docker-compose.prod.yml up`

# How to deploy and start the ATTX platform automatically in RHEL7 and Centos7:
1. Pre-requisite: sharing your SSH public keys with the target host
2. Download the "single_host_deployment.yml" Ansible playbook
3. Edit the "hosts" and "remote_user" (must have sudo rights) as appropriate
4. Run the ansible-playbook (e.g. `ansible-playbook -i hosts --ask-become-pass single_host_deployment.yml`)
