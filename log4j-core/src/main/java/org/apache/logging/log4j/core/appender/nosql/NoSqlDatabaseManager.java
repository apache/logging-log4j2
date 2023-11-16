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

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

/**
 * An {@link AbstractDatabaseManager} implementation for all NoSQL databases.
 *
 * @param <W> A type parameter for reassuring the compiler that all operations are using the same {@link NoSqlObject}.
 */
public final class NoSqlDatabaseManager<W> extends AbstractDatabaseManager {
    /**
     * Encapsulates data that {@link NoSQLDatabaseManagerFactory} uses to create managers.
     */
    private static final class FactoryData extends AbstractDatabaseManager.AbstractFactoryData {
        private final NoSqlProvider<?> provider;
        private final KeyValuePair[] additionalFields;

        protected FactoryData(
                final Configuration configuration,
                final int bufferSize,
                final NoSqlProvider<?> provider,
                final KeyValuePair[] additionalFields) {
            super(configuration, bufferSize, null); // no layout
            this.provider = Objects.requireNonNull(provider, "provider");
            this.additionalFields = additionalFields; // null OK
        }
    }

    /**
     * Creates managers.
     */
    private static final class NoSQLDatabaseManagerFactory
            implements ManagerFactory<NoSqlDatabaseManager<?>, FactoryData> {
        @Override
        @SuppressWarnings("unchecked")
        public NoSqlDatabaseManager<?> createManager(final String name, final FactoryData data) {
            Objects.requireNonNull(data, "data");
            return new NoSqlDatabaseManager(
                    name, data.getBufferSize(), data.provider, data.additionalFields, data.getConfiguration());
        }
    }

    private static final NoSQLDatabaseManagerFactory FACTORY = new NoSQLDatabaseManagerFactory();

    /**
     * Creates a NoSQL manager for use within the {@link NoSqlAppender}, or returns a suitable one if it already exists.
     *
     * @param name The name of the manager, which should include connection details and hashed passwords where possible.
     * @param bufferSize The size of the log event buffer.
     * @param provider A provider instance which will be used to obtain connections to the chosen NoSQL database.
     * @return a new or existing NoSQL manager as applicable.
     * @deprecated Use {@link #getNoSqlDatabaseManager(String, int, NoSqlProvider, KeyValuePair[], Configuration)}.
     */
    @Deprecated
    public static NoSqlDatabaseManager<?> getNoSqlDatabaseManager(
            final String name, final int bufferSize, final NoSqlProvider<?> provider) {
        return AbstractDatabaseManager.getManager(name, new FactoryData(null, bufferSize, provider, null), FACTORY);
    }

    /**
     * Creates a NoSQL manager for use within the {@link NoSqlAppender}, or returns a suitable one if it already exists.
     *
     * @param name The name of the manager, which should include connection details and hashed passwords where possible.
     * @param bufferSize The size of the log event buffer.
     * @param provider A provider instance which will be used to obtain connections to the chosen NoSQL database.
     * @param additionalFields Additional fields.
     * @param configuration TODO
     * @return a new or existing NoSQL manager as applicable.
     */
    public static NoSqlDatabaseManager<?> getNoSqlDatabaseManager(
            final String name,
            final int bufferSize,
            final NoSqlProvider<?> provider,
            final KeyValuePair[] additionalFields,
            final Configuration configuration) {
        return AbstractDatabaseManager.getManager(
                name, new FactoryData(configuration, bufferSize, provider, additionalFields), FACTORY);
    }

    private final NoSqlProvider<NoSqlConnection<W, ? extends NoSqlObject<W>>> provider;

    private NoSqlConnection<W, ? extends NoSqlObject<W>> connection;

    private final KeyValuePair[] additionalFields;

    private NoSqlDatabaseManager(
            final String name,
            final int bufferSize,
            final NoSqlProvider<NoSqlConnection<W, ? extends NoSqlObject<W>>> provider,
            final KeyValuePair[] additionalFields,
            final Configuration configuration) {
        super(name, bufferSize, null, configuration);
        this.provider = provider;
        this.additionalFields = additionalFields;
    }

    private NoSqlObject<W> buildMarkerEntity(final Marker marker) {
        final NoSqlObject<W> entity = this.connection.createObject();
        entity.set("name", marker.getName());

        final Marker[] parents = marker.getParents();
        if (parents != null) {
            @SuppressWarnings("unchecked")
            final NoSqlObject<W>[] parentEntities = new NoSqlObject[parents.length];
            for (int i = 0; i < parents.length; i++) {
                parentEntities[i] = buildMarkerEntity(parents[i]);
            }
            entity.set("parents", parentEntities);
        }
        return entity;
    }

    @Override
    protected boolean commitAndClose() {
        // all NoSQL drivers auto-commit (since NoSQL doesn't generally use the concept of transactions).
        // also, all our NoSQL drivers use internal connection pooling and provide clients, not connections.
        // thus, we should not be closing the client until shutdown as NoSQL is very different from SQL.
        // see LOG4J2-591 and LOG4J2-676
        return true;
    }

    @Override
    protected void connectAndStart() {
        try {
            this.connection = this.provider.getConnection();
        } catch (final Exception e) {
            throw new AppenderLoggingException("Failed to get connection from NoSQL connection provider.", e);
        }
    }

    private NoSqlObject<W>[] convertStackTrace(final StackTraceElement[] stackTrace) {
        final NoSqlObject<W>[] stackTraceEntities = this.connection.createList(stackTrace.length);
        for (int i = 0; i < stackTrace.length; i++) {
            stackTraceEntities[i] = this.convertStackTraceElement(stackTrace[i]);
        }
        return stackTraceEntities;
    }

    private NoSqlObject<W> convertStackTraceElement(final StackTraceElement element) {
        final NoSqlObject<W> elementEntity = this.connection.createObject();
        elementEntity.set("className", element.getClassName());
        elementEntity.set("methodName", element.getMethodName());
        elementEntity.set("fileName", element.getFileName());
        elementEntity.set("lineNumber", element.getLineNumber());
        return elementEntity;
    }

    private void setAdditionalFields(final NoSqlObject<W> entity) {
        if (additionalFields != null) {
            final NoSqlObject<W> object = connection.createObject();
            final StrSubstitutor strSubstitutor = getStrSubstitutor();
            Stream.of(additionalFields)
                    .forEach(f -> object.set(
                            f.getKey(), strSubstitutor != null ? strSubstitutor.replace(f.getValue()) : f.getValue()));
            entity.set("additionalFields", object);
        }
    }

    private void setFields(final LogEvent event, final NoSqlObject<W> entity) {
        entity.set("level", event.getLevel());
        entity.set("loggerName", event.getLoggerName());
        entity.set(
                "message",
                event.getMessage() == null ? null : event.getMessage().getFormattedMessage());

        final StackTraceElement source = event.getSource();
        if (source == null) {
            entity.set("source", (Object) null);
        } else {
            entity.set("source", this.convertStackTraceElement(source));
        }

        final Marker marker = event.getMarker();
        if (marker == null) {
            entity.set("marker", (Object) null);
        } else {
            entity.set("marker", buildMarkerEntity(marker));
        }

        entity.set("threadId", event.getThreadId());
        entity.set("threadName", event.getThreadName());
        entity.set("threadPriority", event.getThreadPriority());
        entity.set("millis", event.getTimeMillis());
        entity.set("date", new java.util.Date(event.getTimeMillis()));

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        Throwable thrown = event.getThrown();
        if (thrown == null) {
            entity.set("thrown", (Object) null);
        } else {
            final NoSqlObject<W> originalExceptionEntity = this.connection.createObject();
            NoSqlObject<W> exceptionEntity = originalExceptionEntity;
            exceptionEntity.set("type", thrown.getClass().getName());
            exceptionEntity.set("message", thrown.getMessage());
            exceptionEntity.set("stackTrace", this.convertStackTrace(thrown.getStackTrace()));
            while (thrown.getCause() != null) {
                thrown = thrown.getCause();
                final NoSqlObject<W> causingExceptionEntity = this.connection.createObject();
                causingExceptionEntity.set("type", thrown.getClass().getName());
                causingExceptionEntity.set("message", thrown.getMessage());
                causingExceptionEntity.set("stackTrace", this.convertStackTrace(thrown.getStackTrace()));
                exceptionEntity.set("cause", causingExceptionEntity);
                exceptionEntity = causingExceptionEntity;
            }

            entity.set("thrown", originalExceptionEntity);
        }

        final ReadOnlyStringMap contextMap = event.getContextData();
        if (contextMap == null) {
            entity.set("contextMap", (Object) null);
        } else {
            final NoSqlObject<W> contextMapEntity = this.connection.createObject();
            contextMap.forEach((key, val) -> contextMapEntity.set(key, val));
            entity.set("contextMap", contextMapEntity);
        }

        final ThreadContext.ContextStack contextStack = event.getContextStack();
        if (contextStack == null) {
            entity.set("contextStack", (Object) null);
        } else {
            entity.set("contextStack", contextStack.asList().toArray());
        }
    }

    private void setFields(final MapMessage<?, ?> mapMessage, final NoSqlObject<W> noSqlObject) {
        // Map without calling org.apache.logging.log4j.message.MapMessage#getData() which makes a copy of the map.
        mapMessage.forEach((key, value) -> noSqlObject.set(key, value));
    }

    @Override
    protected boolean shutdownInternal() {
        // NoSQL doesn't use transactions, so all we need to do here is simply close the client
        return Closer.closeSilently(this.connection);
    }

    @Override
    protected void startupInternal() {
        // nothing to see here
    }

    @Override
    protected void writeInternal(final LogEvent event, final Serializable serializable) {
        if (!this.isRunning() || this.connection == null || this.connection.isClosed()) {
            throw new AppenderLoggingException(
                    "Cannot write logging event; NoSQL manager not connected to the database.");
        }

        final NoSqlObject<W> entity = this.connection.createObject();
        if (serializable instanceof MapMessage) {
            setFields((MapMessage<?, ?>) serializable, entity);
        } else {
            setFields(event, entity);
        }
        setAdditionalFields(entity);
        this.connection.insertObject(entity);
    }
}
