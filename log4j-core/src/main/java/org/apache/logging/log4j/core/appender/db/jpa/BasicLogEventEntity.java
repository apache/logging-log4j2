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
import javax.persistence.Basic;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.db.jpa.converter.ContextMapAttributeConverter;
import org.apache.logging.log4j.core.appender.db.jpa.converter.ContextStackAttributeConverter;
import org.apache.logging.log4j.core.appender.db.jpa.converter.LevelAttributeConverter;
import org.apache.logging.log4j.core.appender.db.jpa.converter.MarkerAttributeConverter;
import org.apache.logging.log4j.core.appender.db.jpa.converter.MessageAttributeConverter;
import org.apache.logging.log4j.core.appender.db.jpa.converter.StackTraceElementAttributeConverter;
import org.apache.logging.log4j.core.appender.db.jpa.converter.ThrowableAttributeConverter;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.Message;

/**
 * Users of the JPA appender may want to extend this class instead of {@link AbstractLogEventWrapperEntity}. This class
 * implements all of the required mutator methods but does not implement a mutable entity ID property. In order to
 * create an entity based on this class, you need only create two constructors matching this class's constructors,
 * annotate the class {@link javax.persistence.Entity @Entity} and {@link javax.persistence.Table @Table}, and implement
 * the fully mutable entity ID property annotated with {@link javax.persistence.Id @Id} and
 * {@link javax.persistence.GeneratedValue @GeneratedValue} to tell the JPA provider how to calculate an ID for new
 * events.<br>
 * <br>
 * The attributes in this entity use the default column names (which, according to the JPA spec, are the property names
 * minus the "get" and "set" from the accessors/mutators). If you want to use different column names for one or more
 * columns, override the necessary accessor methods defined in this class with the same annotations plus the
 * {@link javax.persistence.Column @Column} annotation to specify the column name.<br>
 * <br>
 * The {@link #getContextMap()} and {@link #getContextStack()} attributes in this entity use the
 * {@link ContextMapAttributeConverter} and {@link ContextStackAttributeConverter}, respectively. These convert the
 * properties to simple strings that cannot be converted back to the properties. If you wish to instead convert these to
 * a reversible JSON string, override these attributes with the same annotations but use the
 * {@link org.apache.logging.log4j.core.appender.db.jpa.converter.ContextMapJsonAttributeConverter} and
 * {@link org.apache.logging.log4j.core.appender.db.jpa.converter.ContextStackJsonAttributeConverter} instead.<br>
 * <br>
 * All other attributes in this entity use reversible converters that can be used for both persistence and retrieval. If
 * there are any attributes you do not want persistent, you should override their accessor methods and annotate with
 * {@link javax.persistence.Transient @Transient}.
 *
 * @see AbstractLogEventWrapperEntity
 */
@MappedSuperclass
public abstract class BasicLogEventEntity extends AbstractLogEventWrapperEntity {
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates this base class. All concrete implementations must have a constructor matching this constructor's
     * signature. The no-argument constructor is required for a standards-compliant JPA provider to accept this as an
     * entity.
     */
    public BasicLogEventEntity() {
        super();
    }

    /**
     * Instantiates this base class. All concrete implementations must have a constructor matching this constructor's
     * signature. This constructor is used for wrapping this entity around a logged event.
     *
     * @param wrappedEvent The underlying event from which information is obtained.
     */
    public BasicLogEventEntity(final LogEvent wrappedEvent) {
        super(wrappedEvent);
    }

    /**
     * Gets the level. Annotated with {@code @Basic} and {@code @Enumerated(EnumType.STRING)}.
     *
     * @return the level.
     */
    @Override
    @Convert(converter = LevelAttributeConverter.class)
    public Level getLevel() {
        return this.getWrappedEvent().getLevel();
    }

    /**
     * Gets the logger name. Annotated with {@code @Basic}.
     *
     * @return the logger name.
     */
    @Override
    @Basic
    public String getLoggerName() {
        return this.getWrappedEvent().getLoggerName();
    }

    /**
     * Gets the source location information. Annotated with
     * {@code @Convert(converter = StackTraceElementAttributeConverter.class)}.
     *
     * @return the source location information.
     * @see StackTraceElementAttributeConverter
     */
    @Override
    @Convert(converter = StackTraceElementAttributeConverter.class)
    public StackTraceElement getSource() {
        return this.getWrappedEvent().getSource();
    }

    /**
     * Gets the message. Annotated with {@code @Convert(converter = MessageAttributeConverter.class)}.
     *
     * @return the message.
     * @see MessageAttributeConverter
     */
    @Override
    @Convert(converter = MessageAttributeConverter.class)
    public Message getMessage() {
        return this.getWrappedEvent().getMessage();
    }

    /**
     * Gets the marker. Annotated with {@code @Convert(converter = MarkerAttributeConverter.class)}.
     *
     * @return the marker.
     * @see MarkerAttributeConverter
     */
    @Override
    @Convert(converter = MarkerAttributeConverter.class)
    public Marker getMarker() {
        return this.getWrappedEvent().getMarker();
    }

    /**
     * Gets the thread ID. Annotated with {@code @Basic}.
     *
     * @return the thread ID.
     */
    @Override
    @Basic
    public long getThreadId() {
        return this.getWrappedEvent().getThreadId();
    }

    /**
     * Gets the thread name. Annotated with {@code @Basic}.
     *
     * @return the thread name.
     */
    @Override
    @Basic
    public int getThreadPriority() {
        return this.getWrappedEvent().getThreadPriority();
    }

    /**
     * Gets the thread name. Annotated with {@code @Basic}.
     *
     * @return the thread name.
     */
    @Override
    @Basic
    public String getThreadName() {
        return this.getWrappedEvent().getThreadName();
    }

    /**
     * Gets the number of milliseconds since JVM launch. Annotated with {@code @Basic}.
     *
     * @return the number of milliseconds since JVM launch.
     */
    @Override
    @Basic
    public long getTimeMillis() {
        return this.getWrappedEvent().getTimeMillis();
    }

    /**
     * Returns the value of the running Java Virtual Machine's high-resolution time source when this event was created,
     * or a dummy value if it is known that this value will not be used downstream.
     *
     * @return the JVM nano time
     */
    @Override
    @Basic
    public long getNanoTime() {
        return this.getWrappedEvent().getNanoTime();
    }

    /**
     * Gets the exception logged. Annotated with {@code @Convert(converter = ThrowableAttributeConverter.class)}.
     *
     * @return the exception logged.
     * @see ThrowableAttributeConverter
     */
    @Override
    @Convert(converter = ThrowableAttributeConverter.class)
    public Throwable getThrown() {
        return this.getWrappedEvent().getThrown();
    }

    /**
     * Gets the exception logged. Annotated with {@code @Convert(converter = ThrowableAttributeConverter.class)}.
     *
     * @return the exception logged.
     * @see ThrowableAttributeConverter
     */
    @Override
    @Transient
    public ThrowableProxy getThrownProxy() {
        return this.getWrappedEvent().getThrownProxy();
    }

    /**
     * Gets the context map. Annotated with {@code @Convert(converter = ContextMapAttributeConverter.class)}.
     *
     * @return the context map.
     * @see ContextMapAttributeConverter
     * @see org.apache.logging.log4j.core.appender.db.jpa.converter.ContextMapJsonAttributeConverter
     */
    @SuppressWarnings("deprecation")
    @Override
    @Convert(converter = ContextMapAttributeConverter.class)
    public Map<String, String> getContextMap() {
        return this.getWrappedEvent().getContextMap();
    }

    /**
     * Gets the context stack. Annotated with {@code @Convert(converter = ContextStackAttributeConverter.class)}.
     *
     * @return the context stack.
     * @see ContextStackAttributeConverter
     * @see org.apache.logging.log4j.core.appender.db.jpa.converter.ContextStackJsonAttributeConverter
     */
    @Override
    @Convert(converter = ContextStackAttributeConverter.class)
    public ThreadContext.ContextStack getContextStack() {
        return this.getWrappedEvent().getContextStack();
    }

    /**
     * Gets the fully qualified class name of the caller of the logger API. Annotated with {@code @Basic}.
     *
     * @return the fully qualified class name of the caller of the logger API.
     */
    @Override
    @Basic
    public String getLoggerFqcn() {
        return this.getWrappedEvent().getLoggerFqcn();
    }
}
