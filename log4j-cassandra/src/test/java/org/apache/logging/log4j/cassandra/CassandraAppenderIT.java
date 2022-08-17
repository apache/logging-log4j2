/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test for CassandraAppender.
 */
@Disabled("https://issues.apache.org/jira/browse/LOG4J2-3384")
public class CassandraAppenderIT {

    private static final String DDL = "CREATE TABLE logs (" +
        "id timeuuid PRIMARY KEY," +
        "timeid timeuuid," +
        "message text," +
        "level text," +
        "marker text," +
        "logger text," +
        "timestamp timestamp," +
        "mdc map<text,text>," +
        "ndc list<text>" +
        ")";

    @Disabled("Doesn't work in Java 11 at this Cassandra version")
    @Test
    @CassandraFixture(keyspace = "test", setup = DDL)
    @LoggerContextSource("CassandraAppenderTest.xml")
    public void appendManyEvents(final LoggerContext context, final Cluster cluster) throws Exception {
        final Logger logger = context.getLogger(getClass());
        ThreadContext.put("test", "mdc");
        ThreadContext.push("ndc");
        for (int i = 0; i < 20; i++) {
            logger.info(MarkerManager.getMarker("MARKER"), "Test log message");
        }
        ThreadContext.clearAll();

        TimeUnit.SECONDS.sleep(3);

        int i = 0;
        try (final Session session = cluster.connect("test")) {
            for (final Row row : session.execute("SELECT * FROM logs")) {
                assertNotNull(row.get("id", UUID.class));
                assertNotNull(row.get("timeid", UUID.class));
                assertNotNull(row.get("timestamp", Date.class));
                assertEquals("Test log message", row.getString("message"));
                assertEquals("MARKER", row.getString("marker"));
                assertEquals("INFO", row.getString("level"));
                assertEquals(getClass().getName(), row.getString("logger"));
                final Map<String, String> mdc = row.getMap("mdc", String.class, String.class);
                assertEquals(1, mdc.size());
                assertEquals("mdc", mdc.get("test"));
                final List<String> ndc = row.getList("ndc", String.class);
                assertEquals(1, ndc.size());
                assertEquals("ndc", ndc.get(0));
                ++i;
            }
        }
        assertEquals(20, i);
    }
}
