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
package org.apache.logging.log4j.mongodb4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.UUID;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.nosql.NoSqlDatabaseManager;
import org.apache.logging.log4j.core.appender.nosql.NoSqlProvider;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.MessageLayout;
import org.apache.logging.log4j.message.MapMessage;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MongoDb4MapMessageTest {

    @Test
    void ordinaryFieldsKeepTheirNamesAndValueTypes() {
        final MapMessage<?, Object> message = new MapMessage<>();
        message.with("action", "login").with("attempts", 2).with("success", true);

        final Document expected =
                new Document("action", "login").append("attempts", 2).append("success", true);

        assertEquals(expected, write(message));
    }

    @Test
    void currentBehaviorCopiesTheTopLevelId() {
        final Document expected = new Document("_id", "event-42")
                .append("action", "login")
                .append("user_id", "user-7");

        assertEquals(expected, write(messageWithReservedId()));
    }

    @Test
    void proposedBehaviorSkipsTheTopLevelIdWithoutRenamingOtherFields() {
        final MapMessage<?, Object> message = messageWithReservedId();
        final Document actual = write(message);
        final Document expected = new Document("action", "login").append("user_id", "user-7");

        assertEquals("event-42", message.get("_id"), "The original message must remain unchanged");
        assertEquals(expected, actual, "The document passed to MongoDB should omit the reserved root _id field");
    }

    @Test
    void nestedIdRemainsApplicationData() {
        final Document details = new Document("_id", "customer-7");
        final MapMessage<?, Object> message = new MapMessage<>();
        message.with("action", "login").with("details", details);

        final Document expected = new Document("action", "login").append("details", details);

        assertEquals(expected, write(message));
    }

    private static MapMessage<?, Object> messageWithReservedId() {
        final MapMessage<?, Object> message = new MapMessage<>();
        message.with("_id", "event-42").with("action", "login").with("user_id", "user-7");
        return message;
    }

    private static Document write(final MapMessage<?, Object> message) {
        final MongoClient client = mock(MongoClient.class);
        final MongoDatabase database = mock(MongoDatabase.class);
        @SuppressWarnings("unchecked")
        final MongoCollection<Document> collection = mock(MongoCollection.class);
        when(database.getCollection("logs")).thenReturn(collection);

        final MongoDb4Connection connection = new MongoDb4Connection(
                new ConnectionString("mongodb://localhost/test.logs"), client, database, "logs", false, null);
        final NoSqlProvider<MongoDb4Connection> provider = () -> connection;
        try (final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager(
                "mapMessage-" + UUID.randomUUID(), 0, provider, null, null)) {
            manager.startup();
            final LogEvent event =
                    Log4jLogEvent.newBuilder().setMessage(message).build();
            manager.write(event, new MessageLayout().toSerializable(event));
        }

        final ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(collection).insertOne(captor.capture());
        return captor.getValue();
    }
}
