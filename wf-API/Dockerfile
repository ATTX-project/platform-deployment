FROM python:2.7.12-alpine

MAINTAINER Stefan Negru <stefan.negru@helsinki.fi>

RUN apk add --update \
    && apk --no-cache add build-base \
    && apk --no-cache add linux-headers \
    && apk --no-cache add bash \
    && apk --no-cache add py-setuptools \
    && apk --no-cache add supervisor \
    && rm -rf /var/cache/apk/*

EXPOSE 4301
ARG WFAPI

ENV GOSU_VERSION 1.10
RUN set -x \
    && apk add --no-cache --virtual .gosu-deps \
        dpkg \
        gnupg \
        openssl \
    && dpkgArch="$(dpkg --print-architecture | awk -F- '{ print $NF }')" \
    && wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch" \
    && wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch.asc" \
    && export GNUPGHOME="$(mktemp -d)" \
    && gpg --keyserver ha.pool.sks-keyservers.net --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 \
    && gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu \
    && rm -r "$GNUPGHOME" /usr/local/bin/gosu.asc \
    && chmod +x /usr/local/bin/gosu \
    && gosu nobody true \
    && apk del .gosu-deps


WORKDIR /app

COPY build/distributions/wf-API-${WFAPI}.tar.gz /app/wf-API.tar.gz
RUN tar -xf wf-API.tar.gz

RUN addgroup -S nonroot \
    && adduser -S -g nonroot nonroot

COPY default.conf /etc/nginx/conf.d/default.conf
COPY supervisor.conf /etc/supervisor/supervisord.conf
COPY entrypoint.sh /

RUN mkdir logs \
    && chmod +x /entrypoint.sh

ENV GOSU_USER nonroot:nonroot
ENV GOSU_CHOWN /app /var/log /etc/supervisor /var/run

ENTRYPOINT ["/entrypoint.sh"]
