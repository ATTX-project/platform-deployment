FROM debian:jessie

MAINTAINER João M. da Silva <joao.silva@helsinki.fi>

VOLUME /dpus
ADD dpus /dpus

ENV MASTER_USER=master MASTER_PASSWORD=commander

RUN apt-get update \
    && apt-get install -y netcat \
    && apt-get install -y curl \
    && apt-get clean

ADD startup.sh add-dpu.sh /usr/local/bin/

CMD ["/bin/bash", "/usr/local/bin/startup.sh"]
