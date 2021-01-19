# Log4j Spring Cloud Sample Application

This application uses Spring Boot and reads the logging configuration from the companion Spring Cloud Config Server
project. The log4j2.xml file is located in the config-repo directory in that project.

## Running With Docker
This sample packages the application in a docker container that is packaged with rabbit-mq (to allow dynamic updates
from Spring Cloud Config), fluent-bit (to test as a log forwarder), Apache Flume (to test as a log forwarder), and
Apache Kafka also as a log forwarder. It also installs Socat, a proxy to allow access to the Docker REST API.
###Prerequisites
Note: This guide assumes you already have docker installed. If you do not you may either use homebrew to install
it or follow the instructions at https://docs.docker.com/docker-for-mac/install/.

Like Log4j, the sample app uses the Maven toolchains plugin. The sample app may be built with Java 8 but is 
configured to run in a docker container with Java 11.

The KafkaAppender requires a Kafka instance to write to. On MacOS a Kafka instance can be created by
```
brew install kafka
zookeeper-server-start /usr/local/etc/kafka/zookeeper.properties & kafka-server-start /usr/local/etc/kafka/server.properties
```

### Starting the Application
* Start the companion rabbit-mq, fluent-bit and flume images `./docker/up.sh`
* Compile and start local application image `./docker/restartApp.sh`
* The application exposes two endpoints.
    1. http://localhost:8080/sample/log?threads=1&count=100000 This endpoint will log up to 100,000 events using 
    1 or more threads. 
    1. http://localhost:8080/sample/exception This endpoint generates an exception that can be used to verify whether
    multiline logging works with the chosen set of components.

### Viewing the logs

Accessing the log files varies depending on the appending being used. When logging to the console "docker logs" may 
be used. As configured, Flume will write to files in /var/log/flume, fluent-bit to the standard output of its container.
Kafka output may be viewed using a tool like [Kafka Tool](http://www.kafkatool.com/).  

## Running with Kubernetes

This sample has been verified to run in a Docker Desktop for Mac environment with Kubernetes enabled and may run in 
other Kubernetes environments. 

### Prerequisites
Note: This guide assumes you already have Docker and Kubernetes installed. Since the same container is used for 
Kubernetes as with Docker, Java 11 is also required. This implmentation uses an ELK stack which is expected to
be installed. They can be downloaded individually and started as local applications on the development 
machine for testing. Logstash should be configured as shown in 
[Logging in the Cloud](http://logging.apache.org/log4j/2.x/manual/cloud.html).

### Starting the Application   
Run the ```docker/deploy.sh``` command from the base directory of the log4j-spring-cloud-config-sample-application 
project. This will build the application and then deploy it to Kubernetes. You should see the start-up logs in Kibana.
To stop, run the ```docker/undeploy.sh``` script, then run ```docker images``` and perform 
```docker rmi --force  {image id}``` where image id is the id of the image for the sample application. 
 
