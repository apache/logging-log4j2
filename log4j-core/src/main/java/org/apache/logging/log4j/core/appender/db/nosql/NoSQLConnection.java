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
package org.apache.logging.log4j.core.appender.db.nosql;

import java.io.Closeable;

/**
 * Represents a connection to the NoSQL database. Serves as a factory for new (empty) objects and an endpoint for
 * inserted objects.
 *
 * @param <T> Specifies which implementation of {@link NoSQLObject} this connection provides.
 * @param <W> Specifies which type of database object is wrapped by the {@link NoSQLObject} implementation provided.
 */
public interface NoSQLConnection<W, T extends NoSQLObject<W>> extends Closeable {
    /**
     * Instantiates and returns a {@link NoSQLObject} instance whose properties can be configured before ultimate
     * insertion via {@link #insertObject(NoSQLObject)}.
     *
     * @return a new object.
     * @see NoSQLObject
     */
    T createObject();

    /**
     * Creates an array of the specified length typed to match the {@link NoSQLObject} implementation appropriate for
     * this provider.
     *
     * @param length the length of the array to create.
     * @return a new array.
     * @see NoSQLObject
     */
    T[] createList(int length);

    /**
     * Inserts the given object into the underlying NoSQL database.
     *
     * @param object The object to insert.
     */
    void insertObject(NoSQLObject<W> object);

    /**
     * Closes the underlying connection. This method call should be idempotent. Only the first call should have any
     * effect; all further calls should be ignored. It's possible the underlying connection is stateless (such as an
     * HTTP web service), in which case this method would be a no-op.
     */
    @Override
    void close();

    /**
     * Indicates whether the underlying connection is closed. If the underlying connection is stateless (such as an
     * HTTP web service), this method would likely always return true. Essentially, this method should only return
     * {@code true} if a call to {@link #insertObject(NoSQLObject)} <b>will</b> fail due to the state of this object.
     *
     * @return {@code true} if this object is considered closed.
     */
    boolean isClosed();
}
