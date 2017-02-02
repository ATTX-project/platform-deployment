FROM python:2.7.12-alpine

MAINTAINER Stefan Negru <stefan.negru@helsinki.fi>

RUN apk add --update --repository http://dl-cdn.alpinelinux.org/alpine/edge/community/\
    && apk add --no-cache build-base \
    && apk add --no-cache linux-headers \
    && apk add --no-cache bash \
    && apk add --no-cache py-setuptools

EXPOSE 4301
ENV version="0.1"

WORKDIR /app

COPY build/distributions/wf-API-${version}.tar.gz /app
RUN tar -xf wf-API-${version}.tar.gz

RUN mkdir logs

CMD ./bin/wfapi --host 0.0.0.0 --port 4301
