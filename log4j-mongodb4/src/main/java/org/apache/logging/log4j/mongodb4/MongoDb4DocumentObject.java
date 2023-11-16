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

import java.util.Arrays;
import org.apache.logging.log4j.core.appender.nosql.NoSqlObject;
import org.bson.Document;

/**
 * The MongoDB implementation of {@link NoSqlObject} typed to a BSON {@link Document}.
 */
public final class MongoDb4DocumentObject implements NoSqlObject<Document> {

    private final Document document;

    /**
     * Constructs a new instance.
     */
    public MongoDb4DocumentObject() {
        this.document = new Document();
    }

    @Override
    public void set(final String field, final NoSqlObject<Document> value) {
        this.document.append(field, value != null ? value.unwrap() : null);
    }

    @Override
    public void set(final String field, final NoSqlObject<Document>[] values) {
        this.document.append(field, values != null ? Arrays.asList(values) : null);
    }

    @Override
    public void set(final String field, final Object value) {
        this.document.append(field, value);
    }

    @Override
    public void set(final String field, final Object[] values) {
        this.document.append(field, values != null ? Arrays.asList(values) : null);
    }

    @Override
    public String toString() {
        return String.format("Mongo4DocumentObject [document=%s]", document);
    }

    @Override
    public Document unwrap() {
        return this.document;
    }
}
