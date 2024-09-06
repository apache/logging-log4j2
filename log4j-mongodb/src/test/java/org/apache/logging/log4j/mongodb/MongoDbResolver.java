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
package org.apache.logging.log4j.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.function.Supplier;
import org.apache.logging.log4j.test.junit.ExtensionContextAnchor;
import org.apache.logging.log4j.test.junit.TypeBasedParameterResolver;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

class MongoDbResolver extends TypeBasedParameterResolver<MongoClient> implements BeforeAllCallback {

    static final String PORT_PROPERTY = "log4j2.mongo.port";

    public MongoDbResolver() {
        super(MongoClient.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        ExtensionContextAnchor.setAttribute(MongoClientHolder.class, new MongoClientHolder(), context);
    }

    @Override
    public MongoClient resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return ExtensionContextAnchor.getAttribute(MongoClientHolder.class, MongoClientHolder.class, extensionContext)
                .get();
    }

    private static final class MongoClientHolder implements CloseableResource, Supplier<MongoClient> {
        private final MongoClient mongoClient;

        public MongoClientHolder() {
            mongoClient = MongoClients.create(String.format(
                    "mongodb://localhost:%d",
                    PropertiesUtil.getProperties().getIntegerProperty(MongoDbTestConstants.PROP_NAME_PORT, 27017)));
        }

        @Override
        public MongoClient get() {
            return mongoClient;
        }

        @Override
        public void close() throws Exception {
            mongoClient.close();
        }
    }
}
