FROM debian:jessie

MAINTAINER João M. da Silva <joao.silva@helsinki.fi>

VOLUME /dpus
ADD dpus /dpus

ENV MASTER_USER=master MASTER_PASSWORD=commander
ENV DOCKERIZE_VERSION v0.3.0

RUN apt-get update \
    && apt-get install -y netcat \
    && apt-get install -y curl \
    && apt-get install -y wget \
    && apt-get clean

RUN wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && rm dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz

ADD startup.sh add-dpu.sh add-pipeline.sh /usr/local/bin/

CMD ["/bin/bash", "/usr/local/bin/startup.sh"]
