FROM frolvlad/alpine-oraclejdk8:slim

WORKDIR /app
ADD jars/rmlservice.jar /app/rmlservice.jar

ENTRYPOINT ["java", "-jar", "/app/rmlservice.jar", "--brokerURL=${BROKER_URL}", "--queue=${QUEUE}", "--exchange=${EXCHANGE}"]
