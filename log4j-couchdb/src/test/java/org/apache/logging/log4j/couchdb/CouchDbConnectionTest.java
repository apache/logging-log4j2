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
    void mapMessageCannotChooseDocumentId() {
        final CouchDbClient client = mock(CouchDbClient.class);
        given(client.save(any())).willReturn(mock(Response.class));
        final NoSqlProvider<CouchDbConnection> provider = () -> new CouchDbConnection(client);
        final MapMessage<?, Object> message = new MapMessage<>();
        message.with("_id", "_design/logs").with("action", "login");
        final LogEvent event = Log4jLogEvent.newBuilder().setMessage(message).build();

        try (final NoSqlDatabaseManager<?> manager =
                NoSqlDatabaseManager.getNoSqlDatabaseManager("couchDbReservedId", 0, provider, null, null)) {
            manager.startup();
            manager.write(event, message);
            manager.write(event, message);

            then(client).should(times(2)).save(Collections.singletonMap("action", "login"));
            assertEquals("_design/logs", message.get("_id"));
        }
    }
}
