FROM frekele/gradle:3.3-jdk8

RUN apt-get update \
    && apt-get install -y netcat \
    && apt-get clean

RUN mkdir -p /tmp/

WORKDIR /tmp/

COPY build /tmp/build
COPY build.test.gradle /tmp/build.gradle
COPY runTests.sh /tmp/
RUN chmod 700 /tmp/runTests.sh
COPY TransformerATTXMetadata-1.0-SNAPSHOT.jar /tmp/
