/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.iceberg;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

class IcebergInspectTest {

    @Test
    void createInspectableTable() {
        final String warehouse = "/tmp/iceberg-inspect";
        final IcebergManager manager = new IcebergManager(
                "inspect", "inspect_catalog", "hadoop", null, warehouse, "logs", "app_logs", 500, 3600);
        manager.startup();

        final Level[] levels = {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR};
        for (int i = 0; i < 1000; i++) {
            final LogEvent event = Log4jLogEvent.newBuilder()
                    .setLoggerName("com.example.App")
                    .setLevel(levels[i % levels.length])
                    .setMessage(new SimpleMessage("log message " + i))
                    .setThreadName("main")
                    .setTimeMillis(1700000000000L + i * 1000L)
                    .build();
            manager.write(event.toImmutable());
        }

        manager.stop(10, TimeUnit.SECONDS);
        System.out.println("Iceberg table written to: " + warehouse + "/logs/app_logs");
        System.out.println("Inspect with: find " + warehouse + " -type f");
    }
}
