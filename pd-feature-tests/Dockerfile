FROM frekele/gradle:3.3-jdk8

RUN apt-get update \
    && apt-get install -y wget \
    && apt-get clean

ENV DOCKERIZE_VERSION v0.3.0

RUN wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && rm dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz

RUN mkdir -p /tmp/

WORKDIR /tmp/

ARG METADATA_DPU

COPY build /tmp/build
COPY build.test.gradle /tmp/build.gradle
COPY runTests.sh /tmp/
RUN chmod 700 /tmp/runTests.sh
COPY uv-dpu-t-attx-metadata-${METADATA_DPU}.jar /tmp/uv-dpu-t-attx-metadata-${METADATA_DPU}.jar
