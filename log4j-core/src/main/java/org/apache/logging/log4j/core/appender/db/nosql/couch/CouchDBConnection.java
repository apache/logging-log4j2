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
package org.apache.logging.log4j.core.appender.db.nosql.couch;

import java.util.Map;

import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.db.nosql.NoSQLConnection;
import org.apache.logging.log4j.core.appender.db.nosql.NoSQLObject;
import org.lightcouch.CouchDbClient;
import org.lightcouch.Response;

/**
 * The Apache CouchDB implementation of {@link NoSQLConnection}.
 */
public final class CouchDBConnection implements NoSQLConnection<Map<String, Object>, CouchDBObject> {
    private final CouchDbClient client;
    private boolean closed = false;

    public CouchDBConnection(final CouchDbClient client) {
        this.client = client;
    }

    @Override
    public CouchDBObject createObject() {
        return new CouchDBObject();
    }

    @Override
    public CouchDBObject[] createList(final int length) {
        return new CouchDBObject[length];
    }

    @Override
    public void insertObject(final NoSQLObject<Map<String, Object>> object) {
        try {
            final Response response = this.client.save(object.unwrap());
            if (response.getError() != null && response.getError().length() > 0) {
                throw new AppenderLoggingException("Failed to write log event to CouchDB due to error: " +
                        response.getError() + ".");
            }
        } catch (final Exception e) {
            throw new AppenderLoggingException("Failed to write log event to CouchDB due to error: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public synchronized void close() {
        this.closed = true;
        this.client.shutdown();
    }

    @Override
    public synchronized boolean isClosed() {
        return this.closed;
    }
}
