FROM frekele/gradle:3.3-jdk8

RUN apt-get update \
    && apt-get install -y netcat \
    && apt-get clean

RUN mkdir -p /tmp/

WORKDIR /tmp/

ARG METADATA_DPU

COPY build /tmp/build
COPY build.test.gradle /tmp/build.gradle
COPY runTests.sh /tmp/
RUN chmod 700 /tmp/runTests.sh
COPY uv-dpu-t-attx-metadata-${METADATA_DPU}.jar /tmp/TransformerATTXMetadata-${METADATA_DPU}.jar 
