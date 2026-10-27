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
package org.apache.logging.log4j.couchdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.nosql.NoSqlDatabaseManager;
import org.apache.logging.log4j.core.appender.nosql.NoSqlProvider;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.junit.jupiter.api.Test;
import org.lightcouch.CouchDbClient;
import org.lightcouch.Response;

class CouchDbConnectionTest {

    @Test
    void mapMessageCannotSetReservedRootFields() {
        final CouchDbClient client = mock(CouchDbClient.class);
        given(client.save(any())).willReturn(mock(Response.class));
        final NoSqlProvider<CouchDbConnection> provider = () -> new CouchDbConnection(client);
        final Map<String, Object> reservedFields = new HashMap<>();
        reservedFields.put("_id", "_design/logs");
        reservedFields.put("_rev", "1-abc");
        reservedFields.put("_deleted", true);
        reservedFields.put(
                "_attachments", Collections.singletonMap("log.txt", Collections.singletonMap("data", "YQ==")));
        reservedFields.put("_revisions", Collections.singletonMap("start", 1));
        reservedFields.put("_revs_info", Collections.emptyList());
        reservedFields.put("_local_seq", "1");
        reservedFields.put("_conflicts", Collections.emptyList());
        reservedFields.put("_deleted_conflicts", Collections.emptyList());
        reservedFields.put("_access", Collections.emptyMap());
        reservedFields.put("_replication_state", "completed");
        reservedFields.put("_replication_state_time", "2026-10-27T00:00:00Z");
        reservedFields.put("_replication_state_reason", "reason");
        reservedFields.put("_replication_id", "replication-42");
        reservedFields.put("_replication_stats", Collections.emptyMap());
        reservedFields.put("_custom", "value");
        reservedFields.put("_", "value");
        final MapMessage<?, Object> message = new MapMessage<>();
        reservedFields.forEach(message::with);
        message.with("action", "login")
                .with("user_id", "user-7")
                .with("attempts", 2)
                .with("success", true)
                .with("details", new HashMap<>(reservedFields));
        final Map<String, Object> expected = new HashMap<>();
        expected.put("action", "login");
        expected.put("user_id", "user-7");
        expected.put("attempts", 2);
        expected.put("success", true);
        expected.put("details", new HashMap<>(reservedFields));
        final Map<String, Object> original = new HashMap<>(message.getData());
        final LogEvent event = Log4jLogEvent.newBuilder().setMessage(message).build();

        try (final NoSqlDatabaseManager<?> manager =
                NoSqlDatabaseManager.getNoSqlDatabaseManager("couchDbReservedId", 0, provider, null, null)) {
            manager.startup();
            manager.write(event, message);
            manager.write(event, message);

            then(client).should(times(2)).save(expected);
            assertEquals(original, message.getData());
        }
    }
}
