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
package org.apache.logging.log4j.core.appender.nosql;

/**
 * Represents a simple POJO object inserted into a NoSQL object.
 *
 * @param <W> Specifies what type of underlying object (such as a MongoDB BasicDBObject) this NoSqlObject wraps.
 */
public interface NoSqlObject<W> {

    /**
     * Sets the value of a property on this object to a String or primitive.
     *
     * @param field The name of the property
     * @param value The value of the property
     */
    void set(String field, Object value);

    /**
     * Sets the value of a property on this object to a nested complex object.
     *
     * @param field The name of the property
     * @param value The value of the property
     */
    void set(String field, NoSqlObject<W> value);

    /**
     * Sets the value of a property on this object to an array of Strings or primitives.
     *
     * @param field The name of the property
     * @param values The values for the property
     */
    void set(String field, Object[] values);

    /**
     * Sets the value of a property on this object to an array of nested complex objects.
     *
     * @param field The name of the property
     * @param values The values for the property
     */
    void set(String field, NoSqlObject<W>[] values);

    /**
     * Obtains the underlying NoSQL library-specific object that this object wraps.
     *
     * @return the wrapped object.
     */
    W unwrap();
}
