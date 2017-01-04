FROM openjdk:7-alpine

RUN set -ex \
    && mkdir /src \
    && apk update \
    && apk add curl \
    && mkdir /opt \
    && cd /opt \
    && curl -fsSL http://www-us.apache.org/dist/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz -o maven.tar.gz \
    && tar -xzf maven.tar.gz \
    && rm -f maven.tar.gz
COPY . /src
RUN set -ex \
    && cd /src \
    && /opt/apache-maven-3.3.9/bin/mvn verify
