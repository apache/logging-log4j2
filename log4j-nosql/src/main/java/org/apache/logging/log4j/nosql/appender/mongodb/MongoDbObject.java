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
package org.apache.logging.log4j.nosql.appender.mongodb;

import java.util.Collections;

import org.apache.logging.log4j.nosql.appender.NoSqlObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

/**
 * The MongoDB implementation of {@link NoSqlObject}.
 */
public final class MongoDbObject implements NoSqlObject<BasicDBObject> {
    private final BasicDBObject mongoObject;

    public MongoDbObject() {
        this.mongoObject = new BasicDBObject();
    }

    @Override
    public void set(final String field, final Object value) {
        this.mongoObject.append(field, value);
    }

    @Override
    public void set(final String field, final NoSqlObject<BasicDBObject> value) {
        this.mongoObject.append(field, value.unwrap());
    }

    @Override
    public void set(final String field, final Object[] values) {
        final BasicDBList list = new BasicDBList();
        Collections.addAll(list, values);
        this.mongoObject.append(field, list);
    }

    @Override
    public void set(final String field, final NoSqlObject<BasicDBObject>[] values) {
        final BasicDBList list = new BasicDBList();
        for (final NoSqlObject<BasicDBObject> value : values) {
            list.add(value.unwrap());
        }
        this.mongoObject.append(field, list);
    }

    @Override
    public BasicDBObject unwrap() {
        return this.mongoObject;
    }
}
