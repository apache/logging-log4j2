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
package org.apache.logging.log4j.kit.logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.internal.recycler.DummyRecyclerFactoryProvider;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory;
import org.apache.logging.log4j.spi.recycler.RecyclerFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class TestListLogger extends AbstractLogger {

    private static final MessageFactory MESSAGE_FACTORY = ParameterizedNoReferenceMessageFactory.INSTANCE;
    private static final FlowMessageFactory FLOW_MESSAGE_FACTORY = new DefaultFlowMessageFactory();
    private static final RecyclerFactory RECYCLER_FACTORY =
            new DummyRecyclerFactoryProvider().createForEnvironment(null);

    private final List<String> messages = new ArrayList<>();

    public TestListLogger(final String name) {
        super(name, MESSAGE_FACTORY, FLOW_MESSAGE_FACTORY, RECYCLER_FACTORY, StatusLogger.getLogger());
    }

    @Override
    public Level getLevel() {
        return Level.DEBUG;
    }

    @Override
    public boolean isEnabled(final Level level, @Nullable final Marker marker) {
        return Level.DEBUG.isLessSpecificThan(level);
    }

    @Override
    protected void doLog(
            final String fqcn,
            final @Nullable StackTraceElement location,
            final Level level,
            final @Nullable Marker marker,
            final @Nullable Message message,
            final @Nullable Throwable throwable) {
        messages.add(message != null ? message.getFormattedMessage() : "");
    }

    public List<? extends String> getMessages() {
        return Collections.unmodifiableList(messages);
    }
}
