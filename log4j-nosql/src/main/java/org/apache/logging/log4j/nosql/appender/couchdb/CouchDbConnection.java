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
package org.apache.logging.log4j.nosql.appender.couchdb;

import java.util.Map;

import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.nosql.appender.AbstractNoSqlConnection;
import org.apache.logging.log4j.nosql.appender.DefaultNoSqlObject;
import org.apache.logging.log4j.nosql.appender.NoSqlConnection;
import org.apache.logging.log4j.nosql.appender.NoSqlObject;
import org.apache.logging.log4j.util.Strings;
import org.lightcouch.CouchDbClient;
import org.lightcouch.Response;

/**
 * The Apache CouchDB implementation of {@link NoSqlConnection}.
 */
public final class CouchDbConnection extends AbstractNoSqlConnection<Map<String, Object>, DefaultNoSqlObject> {
    private final CouchDbClient client;

    public CouchDbConnection(final CouchDbClient client) {
        this.client = client;
    }

    @Override
    public DefaultNoSqlObject createObject() {
        return new DefaultNoSqlObject();
    }

    @Override
    public DefaultNoSqlObject[] createList(final int length) {
        return new DefaultNoSqlObject[length];
    }

    @Override
    public void insertObject(final NoSqlObject<Map<String, Object>> object) {
        try {
            final Response response = this.client.save(object.unwrap());
            if (Strings.isNotEmpty(response.getError())) {
                throw new AppenderLoggingException(
                        "Failed to write log event to CouchDB due to error: " + response.getError() + '.');
            }
        } catch (final Exception e) {
            throw new AppenderLoggingException("Failed to write log event to CouchDB due to error: " + e.getMessage(),
                    e);
        }
    }

    @Override
    protected void closeImpl() {
        this.client.shutdown();
    }

}
