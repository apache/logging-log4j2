<!-- vim: set syn=markdown : -->
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Using Log4j in Cloud Enabled Applications

## The Twelve-Factor Application

The Logging Guidelines for [The Twelve-Factor App](https://12factor.net/logs) state that all logs should be routed 
unbuffered to stdout. Since this is the least common denominator it is guaranteed to work for all applications. Howeever,
as with any set of general guidelines, choosing the least common denominator approach comes at a cost. Some of the costs
in Java applications include:

1. Java stack traces are multi-line log messages. The standard docker log driver cannot handle these properly. See 
[Docker Issue #22920](https://github.com/moby/moby/issues/22920) which was closed with the message "Don't Care".
Solutions for this are to:
    a. Use a docker log driver that does support multi-line log message,
    b. Use a logging format that does not produce multi-line messages,
    c. Log from Log4j directly to a logging forwarder or aggregator and bypass the docker logging driver.
1. When logging to stdout in Docker, log events pass through Java's standard output handling which is then directed 
to the operating system so that the output can be piped into a file. The overhead of all this is measurably slower
than just writing directly to a file as can be seen by the performance results below where logging 
to stdout is anywhere from 20 to 200% slower than logging directly to the file. However, these results alone
would not be enough to argue against writing to the standard output stream as they only amount to about 20-30 
microseconds per logging call. 
1. When performing audit logging using a framework such as log4j-audit guaranteed delivery of the audit events
is required. Many of the options for writing the output, including writing to the standard output stream, do
not guarantee delivery. In these cases the event must be delivered to a "forwarder" that acknowledges receipt
only when it has placed the event in durable storage, such as what Apache Flume or Apache Kafka will do.

## Logging Approaches

All the solutions discussed on this page are predicated with the idea that log files cannot permanently
reside on the file system and that all log events should be routed to one or more log analysis tools that will 
be used for reporting and alerting. There are many ways to forward and collect events to be sent to the 
log analysis tools. 

Note that any approach that bypasses Docker's logging drivers requires Log4j's 
[Docker Loookup](lookups.html#DockerLookup) to allow Docker attributes to be injected into the log events.  

### Logging to the Standard Output Stream

As discussed above, this is the recommended 12-Factor approach for applications running in a docker container.
The Log4j team does not recommend this approach if exceptions will be logged by the Java application.

![Stdout](../images/DockerStdout.png "Application Logging to the Standard Output Stream")

### Logging to the Standard Output Stream with the Docker Fluentd Logging Driver

Docker provides alternate [logging drivers](https://docs.docker.com/config/containers/logging/configure/), 
such as [gelf](https://docs.docker.com/config/containers/logging/gelf/) or 
[fluentd](https://docs.docker.com/config/containers/logging/fluentd/), that
can be used to redirect the standard output stream to a log forwarder or log aggregator. 

When routing to a log forwarder it is expected that the forwarder will have the same lifetime as the 
application. If the forwarder should fail the management tools would be expected to also terminate 
other containers dependent on the forwarder.

![Docker Fluentbit](../images/DockerFluentd.png "Logging via StdOut using the Docker Fluentd Logging Driver to Fluent-bit")

As an alternative the logging drivers could be configured to route events directly to a logging aggregator.
This is generally not a good idea as the logging drivers only allow a single host and port to be configured. 
The docker documentation isn't clear but infers that log events will be dropped when log events cannot be
delivered so this method should not be used if a highly available solution is required.

![Docker Fluentd](../images/DockerFluentdAggregator.png "Logging via StdOut using the Docker Fluentd Logging Driver to Fluentd")

### Logging to a File

While this is not the recommended 12-Factor approach, it performs very well. However, it requires that the 
application declare a volume where the log files will reside and then configure the log forwarder to tail 
those files. Care must also be taken to automatically manage the disk space used for the logs, which Log4j 
can perform via the Delete action on the [RollingFileAppender](appenders.html#RollingFileAppender).

![File](../images/DockerLogFile.png "Logging to a File")

### Sending Directly to a Log Forwarder via TCP

Sending logs directly to a Log Forwarder is simple as it generally just requires that the forwarder's
host and port be configured on a SocketAppender with an appropriate layout.

![TCP](../images/DockerTCP.png "Application Logging to a Forwarder via TCP")

### Sending Directly to a Log Aggregator via TCP

Similar to sending logs to a forwarder, logs can also be sent to a cluster of aggregators. However,
setting this up is not as simple since, to be highly available, a cluster of aggregators must be used.
However, the SocketAppender currently can only be configured with a single host and port. To allow 
for failover if the primary aggregator fails the SocketAppender must be enclosed in a 
[FailoverAppender](appenders.html#FailoverAppender),
which would also have the secondary aggregator configured. Another option is to have the SocketAppender 
point to a highly available proxy that can forward to the Log Aggregator.

If the log aggregator used is Apache Flume or Apache Kafka (or similar) the Appenders for these support 
being configured with a list of hosts and ports so high availability is not an issue. 

![Aggregator](../images/LoggerAggregator.png "Application Logging to an Aggregator via TCP")

## Managing Logging Configuration

Spring Boot provides another least common denominator approach to logging configuration. It will let you set the 
log level for various Loggers within an application which can be dynamically updated via REST endpoints provided 
by Spring. While this works in a lot of cases it does not support any of the more advanced filtering featurs of 
Log4j. For example, since it cannot add or modify any Filters other than the log level of a logger, changes cannot be made to allow 
all log events for a specific user or customer to temporarily be logged 
(see [DynamicThresholdFilter](filters.html#DynamicThresholdFilter) or 
[ThreadContextMapFilter](filters.html#ThreadContextMapFilter)) or any other kinds of changes to filters. 
Also, in a micro-services, clustered environment it is quite likely that these changes will need to be propagated
to multiple servers at the same time. Trying to achieve this via REST calls could be difficult.
  
Since its first release Log4j has supported reconfiguration through a file.
Beginning with Log4j 2.12.0 Log4j also supports accessing the configuration via HTTP(S) and monitoring the file 
for changes by using the HTTP "If-Modified-Since" header. A patch has also been integrated into Spring Cloud Config
starting with versions 2.0.3 and 2.1.1 for it to honor the If-Modified-Since header. In addition, the 
log4j-spring-cloud-config project will listen for update events published by Spring Cloud Bus and then verify
that the configuratoin file has been modified, so polling via HTTP is not required.

Log4j also supports composite configurations. A distributed application spread across microservices could 
share a common configuration file that could be used to control things like enabling debug logging for a 
specific user.

While the standard Spring Boot REST endpoints to update logging will still work any changes made by those 
REST endpoints will be lost if Log4j reconfigures itself do to changes in the logging configuration file.

Further information regarding integration of the log4j-spring-cloud-config-client can be found at 
[Log4j Spring Cloud Config Client](../log4j-spring-cloud-config/log4j-spring-cloud-config-client/index.html).

## Integration with Docker

Applications within a Docker container that log using a Docker logging driver can include special 
attributes in the formatted log event as described at 
[Customize Log Driver Output](https://docs.docker.com/config/containers/logging/log_tags/). Log4j 
provides similar functionality via the [Docker Loookup](lookups.html#DockerLookup). More information on
Log4j's Docker support may also be found at [Log4j-Docker](../log4j-docker/index.html). 

## Appender Performance
The numbers in the table below represent how much time in seceonds was required for the application to 
call logger.debug 100,000 times. These numbers only include the time taken to deliver to the specifcly 
noted endpoint and many not include the actual time required before they are availble for viewing. All 
measurements were performed on a MacBook Pro with a 2.9GHz Intel Core I9 processor with 6 physical and 12 
logical cores, 32GB of 2400 MHz DDR4 RAM, and 1TB of Apple SSD storage. The VM used by Docker was managed 
by VMWare Fusion and had 4 CPUs and 2 GB of RAM. These number should be used for relative perfomance comparisons 
as the results on another system may vary considerably.

The sample application used can be found under the log4j-spring-cloud-config/log4j-spring-cloud-config-samples
directory in the Log4j [source repository](https://github.com/apache/logging-log4j2).

| Test                    | 1 Thread | 2 Threads | 4 Threads | 8 Threads |
|------------------------ |---------:|----------:|----------:|----------:|
|Flume Avro |||||
|- Batch Size 1 - JSON    |49.11     |46.54      |46.70      |44.92      |
|- Batch Size 1 - RFC5424 |48.30     |45.79      |46.31      |45.50      |
|- Batch Size 100 - JSON  | 6.33     |3.87       |3.57       |3.84       | 
|- Batch Size 100 - RFC5424 | 6.08   |3.69       |3.22       |3.11       | 
|- Batch Size 1000 - JSON | 4.83     |3.20       |3.02       |2.11       |
|- Batch Size 1000 - RFC5424 | 4.70  |2.40       |2.37       |2.37       |
|Flume Embedded |||||
| - RFC5424               |3.58      |2.10       |2.10       |2.70       |
| - JSON                  |4.20      |2.49       |3.53       |2.90       |
|Kafka Local JSON |||||
| - sendSync true         |58.46     |38.55      |19.59      |19.01      |
| - sendSync false        |9.8       |10.8       |12.23      |11.36      |
|Console|||||
| - JSON / Kubernetes     |3.03      |3.11       |3.04       |2.51       |
| - JSON                  |2.80      |2.74       |2.54       |2.35       |
| - Docker fluentd driver |10.65     |9.92       |10.42      |10.27      |
|Rolling File|||||
| - RFC5424               |1.65      |0.94       |1.22       |1.55
| - JSON                  |1.90      |0.95       |1.57       |1.94       |
|TCP - Fluent Bit - JSON  |2.34      |2.167      |1.67       |2.50       |
|Async Logger|||||
|- TCP - Fluent Bit - JSON|0.90      |0.58       |0.36       |0.48       |
|- Console - JSON         |0.83      |0.57       |0.55       |0.61       |
|- Flume Avro - 1000 - JSON|0.76     |0.37       |0.45       |0.68       |

Notes:

1. Flume Avro - Buffering is controlled by the batch size. Each send is complete when the remote 
acknowledges the batch was written to its channel. These number seem to indicate Flume Avro could
benefit from using a pool of RPCClients, at least for a batchSize of 1.
1. Flume Embedded - This is essentially asynchronous as it writes to an in-memory buffer. It is
unclear why the performance isn't closer to the AsyncLogger results.
1. Kafka was run in standalone mode on the same laptop as the application. See  sendSync set to true
requires waiting for an ack from Kafka for each log event. 
1. Console - System.out is redirected to a file by Docker. Testing shows that it would be much
slower if it was writing to the terminal screen.
1. Rolling File - Test uses the default buffer size of 8K.
1. TCP to Fluent Bit - The Socket Appender uses a default buffer size of 8K.
1. Async Loggers - These all write to a circular buffer and return to the application. The actual
I/O will take place on a separate thread. If writing the events is performed more slowly than 
events are being created eventually the buffer will fill up and logging will be performed at 
the same pace that log events are written.

## Logging Recommendations

1. Use asynchronous logging unless guaranteed delivery is absolutely required. As 
the performance numbers show, so long as the volume of logging is not high enough to fill up the 
circular buffer the overhead of logging will almost be unnoticeable to the application.
1. If overall performance is a consideration or you require multiline events such as stack traces
be processed properly then log via TCP to a companion container that acts as a log forwarder. Use the 
Log4j Docker Lookup to add the container information to each log event.
1. Whenvever guaranteed delivery is required use Flume Avro with a batch size of 1 or another Appender such 
as the Kafka Appender with syncSend set to true that only return control after the downstream agent 
acknowledges receipt of the event. Beware that using an Appender that writes each event individually should 
be kept to a minimum since it is much slower than sending buffered events. 
1. Logging to files within the container is discouraged. Doing so requires that a volume be declared in 
the Docker configuration and that the file be tailed by a log forwarder. However, it performs 
better than logging to the standard output stream. If logging via TCP is not an option and
proper multiline handling is required then consider this option.