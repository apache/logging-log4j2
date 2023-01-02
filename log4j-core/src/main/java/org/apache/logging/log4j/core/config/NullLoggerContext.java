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
package org.apache.logging.log4j.core.config;

import java.util.function.Supplier;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.util.PropertyResolver;

public class NullLoggerContext extends LoggerContext {
    public static final String NULL_NAME = "Null";

    private NullLoggerContext(final Injector injector, final PropertyResolver propertyResolver,
                              final MessageFactory messageFactory, final FlowMessageFactory flowMessageFactory) {
        super(NULL_NAME, NULL_NAME, null, null, injector, propertyResolver,
                messageFactory, flowMessageFactory);
    }

    public static NullLoggerContext getInstance() {
        return new Builder().get();
    }

    private static class Builder extends GenericBuilder<Builder> implements Supplier<NullLoggerContext> {
        @Override
        public NullLoggerContext get() {
            return new NullLoggerContext(getInjector(), getPropertyResolver(), getMessageFactory(), getFlowMessageFactory());
        }
    }
}
