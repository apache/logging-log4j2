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

/**
 * Represents a connection to the NoSQL database. Serves as a factory for new (empty) objects and an endpoint for
 * inserted objects.
 * 
 * @param <T>
 *            Specifies which implementation of {@link NoSQLObject} this connection provides.
 * @param <W>
 *            Specifies which type of database object is wrapped by the {@link NoSQLObject} implementation provided.
 */
public interface NoSQLConnection<W, T extends NoSQLObject<W>> {
    void close();

    T[] createList(int length);

    T createObject();

    void insertObject(NoSQLObject<W> object);

    boolean isClosed();
}
