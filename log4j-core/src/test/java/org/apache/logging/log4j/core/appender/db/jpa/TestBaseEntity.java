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

import java.util.Date;
import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.db.jpa.converter.LevelAttributeConverter;
import org.apache.logging.log4j.core.appender.db.jpa.converter.MessageAttributeConverter;
import org.apache.logging.log4j.core.appender.db.jpa.converter.ThrowableAttributeConverter;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.Message;

@Entity
@Table(name = "jpaBaseLogEntry")
public class TestBaseEntity extends AbstractLogEventWrapperEntity {
    private static final long serialVersionUID = 1L;

    private long id = 0L;

    public TestBaseEntity() {
        super();
    }

    public TestBaseEntity(final LogEvent wrappedEvent) {
        super(wrappedEvent);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public long getId() {
        return this.id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "eventDate")
    public Date getEventDate() {
        return new Date(this.getTimeMillis());
    }

    public void setEventDate(final Date date) {
        // this entity is write-only
    }

    @Override
    @Convert(converter = LevelAttributeConverter.class)
    @Column(name = "level")
    public Level getLevel() {
        return this.getWrappedEvent().getLevel();
    }

    @Override
    @Basic
    @Column(name = "logger")
    public String getLoggerName() {
        return this.getWrappedEvent().getLoggerName();
    }

    @Override
    @Transient
    public StackTraceElement getSource() {
        return this.getWrappedEvent().getSource();
    }

    @Override
    @Convert(converter = MessageAttributeConverter.class)
    @Column(name = "message")
    public Message getMessage() {
        return this.getWrappedEvent().getMessage();
    }

    @Override
    @Transient
    public Marker getMarker() {
        return this.getWrappedEvent().getMarker();
    }

    @Override
    @Transient
    public long getThreadId() {
        return this.getWrappedEvent().getThreadId();
    }

    @Override
    @Transient
    public String getThreadName() {
        return this.getWrappedEvent().getThreadName();
    }

    @Override
    @Transient
    public int getThreadPriority() {
        return this.getWrappedEvent().getThreadPriority();
    }

    @Override
    @Transient
    public long getTimeMillis() {
        return this.getWrappedEvent().getTimeMillis();
    }

    @Override
    @Transient
    public long getNanoTime() {
        return this.getWrappedEvent().getNanoTime();
    }

    @Override
    @Convert(converter = ThrowableAttributeConverter.class)
    @Column(name = "exception")
    public Throwable getThrown() {
        return this.getWrappedEvent().getThrown();
    }

    @Override
    @Transient
    public ThrowableProxy getThrownProxy() {
        return this.getWrappedEvent().getThrownProxy();
    }

    @SuppressWarnings("deprecation")
    @Override
    @Transient
    public Map<String, String> getContextMap() {
        return this.getWrappedEvent().getContextMap();
    }

    @Override
    @Transient
    public ReadOnlyStringMap getContextData() {
        return this.getWrappedEvent().getContextData();
    }

    @Override
    @Transient
    public ThreadContext.ContextStack getContextStack() {
        return this.getWrappedEvent().getContextStack();
    }

    @Override
    @Transient
    public String getLoggerFqcn() {
        return this.getWrappedEvent().getLoggerFqcn();
    }
}
