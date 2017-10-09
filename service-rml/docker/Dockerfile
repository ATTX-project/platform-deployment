FROM openjdk:8-jdk-alpine

RUN apk add --update \
    && apk add --no-cache build-base \
    && apk add --no-cache linux-headers \
    && apk add --no-cache bash \
    && rm -rf /var/cache/apk/*

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
    && gpg --keyserver ipv4.pool.sks-keyservers.net --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 \
    && gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu \
    && rm -r /usr/local/bin/gosu.asc \
    && rm -r -f "$GNUPGHOME" \
    && chmod +x /usr/local/bin/gosu \
    && gosu nobody true \
    && apk del .gosu-deps

WORKDIR /app
ADD jars/rmlservice.jar /app/rmlservice.jar
COPY entrypoint.sh /
RUN chmod +x /entrypoint.sh
RUN chmod +x /app/rmlservice.jar

RUN addgroup -S nonroot \
    && adduser -S -g nonroot nonroot

ENV GOSU_USER nonroot:nonroot
ENV GOSU_CHOWN /app /var/run

ENTRYPOINT ["/entrypoint.sh", "--brokerURL=${MHOST}", "--queue=${MQUEUE}", "--exchange=${MEXCHANGE}", "--username=${MUSER}", "--password=${MPASS}"]
