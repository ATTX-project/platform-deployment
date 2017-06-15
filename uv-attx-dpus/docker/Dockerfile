FROM alpine:3.3

MAINTAINER João M. da Silva <joao.silva@helsinki.fi>

VOLUME /dpus
ADD dpus /dpus

ENV MASTER_USER=master MASTER_PASSWORD=commander
ENV DOCKERIZE_VERSION v0.5.0

RUN apk add --no-cache bash curl wget
RUN apk add --no-cache openssl

ENV DOCKERIZE_VERSION v0.5.0
RUN wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && rm dockerize-alpine-linux-amd64-$DOCKERIZE_VERSION.tar.gz

RUN mkdir -p /var/log/adddpus
RUN chmod 777 /var/log/adddpus
VOLUME /var/log/adddpus
ADD startup.sh add-dpu.sh /usr/local/bin/
# ADD startup.sh add-dpu.sh add-pipeline.sh /usr/local/bin/

CMD ["/bin/bash", "/usr/local/bin/startup.sh"]
