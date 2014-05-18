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
package org.apache.logging.log4j.nosql.appender;

/**
 * Implementations of this class are plugins for configuring the {@link NoSQLAppender} with the proper provider
 * (MongoDB, etc.).
 *
 * @param <C> Specifies which implementation of {@link NoSQLConnection} this provider provides.
 */
public interface NoSQLProvider<C extends NoSQLConnection<?, ? extends NoSQLObject<?>>> {
    /**
     * Obtains a connection from this provider. The concept of a connection in this case is not strictly an active
     * duplex UDP or TCP connection to the underlying database. It can be thought of more as a gateway, a path for
     * inserting objects that may use a persistent connection or may use HTTP web service calls, etc.<br />
     * <br />
     * Where applicable, this method should return a connection from the connection pool as opposed to opening a
     * brand new connection every time.
     *
     * @return a connection that can be used to create and persist objects to this database.
     * @see NoSQLConnection
     */
    C getConnection();

    /**
     * All implementations must override {@link Object#toString()} to provide information about the provider
     * configuration (obscuring passwords with one-way hashes).
     *
     * @return the string representation of this NoSQL provider.
     */
    @Override
    String toString();
}
