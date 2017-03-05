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
package org.apache.logging.log4j.core.appender.db.jpa;

import java.util.Map;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.AbstractLogEvent;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.db.jpa.converter.ContextDataAttributeConverter;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;

/**
 * <p>
 * Users of the JPA appender MUST extend this class, using JPA annotations on the concrete class and all of its
 * accessor methods (as needed) to map them to the proper table and columns. Accessors you do not want persisted should
 * be annotated with {@link Transient @Transient}. All accessors should call {@link #getWrappedEvent()} and delegate the
 * call to the underlying event. Users may want to instead extend {@link BasicLogEventEntity}, which takes care of all
 * of this for you.
 * </p>
 * <p>
 * The concrete class must have two constructors: a public no-arg constructor to convince the JPA provider that it's a
 * valid entity, and a public constructor that takes a single {@link LogEvent event} and passes it to the parent class
 * with {@link #AbstractLogEventWrapperEntity(LogEvent) super(event)}. Furthermore, the concrete class must be annotated
 * {@link javax.persistence.Entity @Entity} and {@link javax.persistence.Table @Table} and must implement a fully
 * mutable ID property annotated with {@link javax.persistence.Id @Id} and
 * {@link javax.persistence.GeneratedValue @GeneratedValue} to tell the JPA provider how to calculate an ID for new
 * events.
 * </p>
 * <p>
 * Many of the return types of {@link LogEvent} methods (e.g., {@link StackTraceElement}, {@link Message},
 * {@link Marker}, {@link Throwable},
 * {@link org.apache.logging.log4j.ThreadContext.ContextStack ThreadContext.ContextStack}, and
 * {@link Map Map&lt;String, String&gt;}) will not be recognized by the JPA provider. In conjunction with
 * {@link javax.persistence.Convert @Convert}, you can use the converters in the
 * {@link org.apache.logging.log4j.core.appender.db.jpa.converter} package to convert these types to database columns.
 * If you want to retrieve log events from the database, you can create a true POJO entity and also use these
 * converters for extracting persisted values.<br>
 * </p>
 * <p>
 * The mutator methods in this class not specified in {@link LogEvent} are no-op methods, implemented to satisfy the JPA
 * requirement that accessor methods have matching mutator methods. If you create additional accessor methods, you must
 * likewise create matching no-op mutator methods.
 * </p>
 *
 * @see BasicLogEventEntity
 */
@MappedSuperclass
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class AbstractLogEventWrapperEntity implements LogEvent {
    private static final long serialVersionUID = 1L;

    private final LogEvent wrappedEvent;

    /**
     * Instantiates this base class. All concrete implementations must have a constructor matching this constructor's
     * signature. The no-argument constructor is required for a standards-compliant JPA provider to accept this as an
     * entity.
     */
    @SuppressWarnings("unused")
    protected AbstractLogEventWrapperEntity() {
        this(new NullLogEvent());
    }

    /**
     * Instantiates this base class. All concrete implementations must have a constructor matching this constructor's
     * signature. This constructor is used for wrapping this entity around a logged event.
     *
     * @param wrappedEvent The underlying event from which information is obtained.
     */
    protected AbstractLogEventWrapperEntity(final LogEvent wrappedEvent) {
        if (wrappedEvent == null) {
            throw new IllegalArgumentException("The wrapped event cannot be null.");
        }
        this.wrappedEvent = wrappedEvent;
    }

    @Override
    public LogEvent toImmutable() {
        return Log4jLogEvent.createMemento(this);
    }

    /**
     * All eventual accessor methods must call this method and delegate the method call to the underlying wrapped event.
     * Annotated {@link Transient} so as not to be included in the persisted entity.
     *
     * @return The underlying event from which information is obtained.
     */
    @Transient
    protected final LogEvent getWrappedEvent() {
        return this.wrappedEvent;
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param level Ignored.
     */
    @SuppressWarnings("unused")
    public void setLevel(final Level level) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param loggerName Ignored.
     */
    @SuppressWarnings("unused")
    public void setLoggerName(final String loggerName) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param source Ignored.
     */
    @SuppressWarnings("unused")
    public void setSource(final StackTraceElement source) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param message Ignored.
     */
    @SuppressWarnings("unused")
    public void setMessage(final Message message) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param marker Ignored.
     */
    @SuppressWarnings("unused")
    public void setMarker(final Marker marker) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param threadId Ignored.
     */
    @SuppressWarnings("unused")
    public void setThreadId(final long threadId) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param threadName Ignored.
     */
    @SuppressWarnings("unused")
    public void setThreadName(final String threadName) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param threadPriority Ignored.
     */
    @SuppressWarnings("unused")
    public void setThreadPriority(final int threadPriority) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param nanoTime Ignored.
     */
    @SuppressWarnings("unused")
    public void setNanoTime(final long nanoTime) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param millis Ignored.
     */
    @SuppressWarnings("unused")
    public void setTimeMillis(final long millis) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param throwable Ignored.
     */
    @SuppressWarnings("unused")
    public void setThrown(final Throwable throwable) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param contextData Ignored.
     */
    @SuppressWarnings("unused")
    public void setContextData(final ReadOnlyStringMap contextData) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param map Ignored.
     */
    @SuppressWarnings("unused")
    public void setContextMap(final Map<String, String> map) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param contextStack Ignored.
     */
    @SuppressWarnings("unused")
    public void setContextStack(final ThreadContext.ContextStack contextStack) {
        // this entity is write-only
    }

    /**
     * A no-op mutator to satisfy JPA requirements, as this entity is write-only.
     *
     * @param fqcn Ignored.
     */
    @SuppressWarnings("unused")
    public void setLoggerFqcn(final String fqcn) {
        // this entity is write-only
    }

    /**
     * Indicates whether the source of the logging request is required downstream. Annotated
     * {@link Transient @Transient} so as to not be included in the persisted entity.
     *
     * @return whether the source of the logging request is required downstream.
     */
    @Override
    @Transient
    public final boolean isIncludeLocation() {
        return this.getWrappedEvent().isIncludeLocation();
    }

    @Override
    public final void setIncludeLocation(final boolean locationRequired) {
        this.getWrappedEvent().setIncludeLocation(locationRequired);
    }

    /**
     * Indicates whether this event is the last one in a batch. Annotated {@link Transient @Transient} so as to not be
     * included in the persisted entity.
     *
     * @return whether this event is the last one in a batch.
     */
    @Override
    @Transient
    public final boolean isEndOfBatch() {
        return this.getWrappedEvent().isEndOfBatch();
    }

    @Override
    public final void setEndOfBatch(final boolean endOfBatch) {
        this.getWrappedEvent().setEndOfBatch(endOfBatch);
    }

    /**
     * Gets the context map. Transient, since the String version of the data is obtained via ReadOnlyStringMap.
     *
     * @return the context data.
     * @see ContextDataAttributeConverter
     * @see org.apache.logging.log4j.core.appender.db.jpa.converter.ContextDataAttributeConverter
     */
    @Override
    @Transient
    //@Convert(converter = ContextDataAttributeConverter.class)
    public ReadOnlyStringMap getContextData() {
        return this.getWrappedEvent().getContextData();
    }

    /**
     * A no-op log event class to prevent {@code NullPointerException}s. O/RMs tend to create instances of entities in
     * order to "play around" with them.
     */
    private static class NullLogEvent extends AbstractLogEvent {

        private static final long serialVersionUID = 1L;
        // Inherits everything
    }
}
