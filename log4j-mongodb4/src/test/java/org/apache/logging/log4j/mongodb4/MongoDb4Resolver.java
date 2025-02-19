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

class MongoDb4Resolver extends TypeBasedParameterResolver<MongoClient> implements BeforeAllCallback {

    public MongoDb4Resolver() {
        super(MongoClient.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
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
                    PropertiesUtil.getProperties().getIntegerProperty(MongoDb4TestConstants.PROP_NAME_PORT, 27017)));
        }

        @Override
        public MongoClient get() {
            return mongoClient;
        }

        @Override
        public void close() {
            mongoClient.close();
        }
    }
}
