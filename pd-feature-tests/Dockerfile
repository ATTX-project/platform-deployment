FROM frekele/gradle:3.3-jdk8


RUN apt-get update
RUN apt-get install -y netcat 

RUN mkdir -p /tmp/ 
COPY build /tmp/build
COPY build.test.gradle /tmp/
COPY runTests.sh /tmp/
RUN chmod 755 /tmp/runTests.sh
COPY TransformerATTXMetadata-1.0-SNAPSHOT.jar /tmp/
WORKDIR /tmp/
