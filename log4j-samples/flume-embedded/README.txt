Sample to utilize an embedded Flume agent to connect to a remote Flume Agent via Avro.

This sample uses the classes in the sample flume-common project. It will randomly pick from the events defined there
and send them to Flume. At the same time it will intermix some random non-audit events.

To run this sample:
1. Run "mvn install" on the flume-common project.
2. Download and install Flume.
3. Copy the flume-conf.properties in src/main/resources/flume/conf to the conf directory of where Flume was installed.
4. In a terminal window start flume using "bin/flume-ng agent --conf ./conf/ -f conf/flume-conf.properties -Dflume.root.logger=DEBUG,console -n agent"
5. Verify Flume started and configured an Avro source, a memory channel and a logger sink by reviewing the startup log.
6. In a separate terminal window run "mvn jetty:run" in this project.
7. Verify the Flume appender connected to the Flume agent by finding "Started SelectChannelConnector@0.0.0.0:8080"
   in the jetty log and that there are no exceptions and also by seeing something like
    "/127.0.0.1:53351 => /127.0.0.1:8800] OPEN" in the Flume log.
8. In a separate terminal window in the project directory run "tail -f target/logs/app.log" to see the application
   generate non-audit logs.
9. In the browser go to url "http://localhost:8080/flumeAgent/start.do". A started message should appear on the screen.
10. After verifying logs are being written click on the Stop button in the browser page.

The output from the Flume agent will include the generated Flume events. Since the events are sent by the embedded
Flume agent in a batch the Flume agent will only print a hex dump of the first few bytes of the event.