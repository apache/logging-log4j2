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

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;

/**
 * Users of the JPA appender MUST implement this class, using JPA annotations on the concrete class and all of its
 * accessor methods (as needed) to map them to the proper table and columns. Accessors you do not want persisted should
 * be annotated with {@link Transient @Transient}. All accessors should call {@link #getWrappedEvent()} and delegate the
 * call to the underlying event.<br>
 * <br>
 * The concrete class must have two constructors: a public no-arg constructor to convince the JPA provider that it's a
 * valid entity (this constructor will call {@link #LogEventWrapperEntity(LogEvent) super(null)}), and a public
 * constructor that takes a single {@link LogEvent event} and passes it to the parent class with
 * {@link #LogEventWrapperEntity(LogEvent) super(event)}. The concrete class must also have a mutable
 * {@link javax.persistence.Id @Id} property with fully-functional mutator and accessor methods (usually configured as a
 * {@link javax.persistence.GeneratedValue @GeneratedValue} property, and should be annotated with
 * {@link javax.persistence.Entity @Entity}<br>
 * <br>
 * Many of the return types of {@link LogEvent} methods (e.g., {@link StackTraceElement}, {@link Message},
 * {@link Marker}) will not be recognized by the JPA provider. In these cases, you must either implement custom
 * persistence serializers <em>or</em> (probably easier) mark those methods {@link Transient @Transient} and create
 * similar methods that return the String form of these properties.<br>
 * <br>
 * The mutator methods in this class not specified in {@link LogEvent} are no-op methods, implemented to satisfy the JPA
 * requirement that accessor methods have matching mutator methods. If you create additional accessor methods, you must
 * likewise create matching no-op mutator methods.
 */
@MappedSuperclass
public abstract class LogEventWrapperEntity implements LogEvent {
    /**
     * Generated serial version ID.
     */
    private static final long serialVersionUID = 1L;
    private final LogEvent wrappedEvent;

    /**
     * Instantiates the base class. All concrete implementations must have two constructors: a no-arg constructor that
     * calls this constructor with a null argument, and a constructor matching this constructor's signature.
     * 
     * @param wrappedEvent
     *            The underlying event from which information is obtained.
     */
    protected LogEventWrapperEntity(final LogEvent wrappedEvent) {
        this.wrappedEvent = wrappedEvent;
    }

    /**
     * All eventual accessor methods must call this method and delegate the method call to the underlying wrapped event.
     * 
     * @return The underlying event from which information is obtained.
     */
    @Transient
    protected final LogEvent getWrappedEvent() {
        return this.wrappedEvent;
    }

    @Override
    @Transient
    public final boolean isEndOfBatch() {
        return this.getWrappedEvent().isEndOfBatch();
    }

    @Override
    @Transient
    public final boolean isIncludeLocation() {
        return this.getWrappedEvent().isIncludeLocation();
    }

    @SuppressWarnings("unused")
    public void setContextMap(final Map<String, String> contextMap) {
        // this entity is write-only
    }

    @SuppressWarnings("unused")
    public void setContextStack(final ThreadContext.ContextStack contextStack) {
        // this entity is write-only
    }

    @Override
    @Transient
    public final void setEndOfBatch(final boolean endOfBatch) {
        this.getWrappedEvent().setEndOfBatch(endOfBatch);
    }

    @SuppressWarnings("unused")
    public void setFQCN(final String fqcn) {
        // this entity is write-only
    }

    @Override
    @Transient
    public final void setIncludeLocation(final boolean locationRequired) {
        this.getWrappedEvent().setIncludeLocation(locationRequired);
    }

    @SuppressWarnings("unused")
    public void setLevel(final Level level) {
        // this entity is write-only
    }

    @SuppressWarnings("unused")
    public void setLoggerName(final String name) {
        // this entity is write-only
    }

    @SuppressWarnings("unused")
    public void setMarker(final Marker marker) {
        // this entity is write-only
    }

    @SuppressWarnings("unused")
    public void setMessage(final Message message) {
        // this entity is write-only
    }

    @SuppressWarnings("unused")
    public void setMillis(final long millis) {
        // this entity is write-only
    }

    @SuppressWarnings("unused")
    public void setSource(final StackTraceElement element) {
        // this entity is write-only
    }

    @SuppressWarnings("unused")
    public void setThreadName(final String name) {
        // this entity is write-only
    }

    @SuppressWarnings("unused")
    public void setThrown(final Throwable throwable) {
        // this entity is write-only
    }
}
