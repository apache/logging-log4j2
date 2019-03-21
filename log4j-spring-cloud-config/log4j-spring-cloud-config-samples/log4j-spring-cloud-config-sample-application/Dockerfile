# Alpine Linux with OpenJDK
#FROM openjdk:8-jdk-alpine
FROM openjdk:11-jdk-slim

ARG build_version
ENV BUILD_VERSION=${build_version}
RUN mkdir /service

ADD ./target/sampleapp.jar /service/
WORKDIR /service

#EXPOSE 8080 5005
EXPOSE 8080

#CMD java "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005" -jar sampleapp.jar
CMD java -jar -Xmx2G sampleapp.jar
