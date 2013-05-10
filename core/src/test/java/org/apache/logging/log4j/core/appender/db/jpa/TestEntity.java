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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.Message;

@Entity
@Table(name = "jpaLogEntry")
@SuppressWarnings("unused")
public class TestEntity extends LogEventWrapperEntity {
    private static final long serialVersionUID = 1L;

    private long id = 0L;

    public TestEntity() {
        super(null);
    }

    public TestEntity(final LogEvent wrappedEvent) {
        super(wrappedEvent);
    }

    @Override
    @Transient
    public Map<String, String> getContextMap() {
        return getWrappedEvent().getContextMap();
    }

    @Override
    @Transient
    public ThreadContext.ContextStack getContextStack() {
        return getWrappedEvent().getContextStack();
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "eventDate")
    public Date getEventDate() {
        return new Date(this.getMillis());
    }

    @Basic
    @Column(name = "exception")
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public String getException() {
        if (this.getThrown() != null) {
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            final PrintWriter writer = new PrintWriter(stream);
            this.getThrown().printStackTrace(writer);
            writer.close();
            return stream.toString();
        }
        return null;
    }

    @Override
    @Transient
    public String getFQCN() {
        return getWrappedEvent().getFQCN();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public long getId() {
        return this.id;
    }

    @Override
    @Enumerated(EnumType.STRING)
    @Column(name = "level")
    public Level getLevel() {
        return getWrappedEvent().getLevel();
    }

    @Override
    @Basic
    @Column(name = "logger")
    public String getLoggerName() {
        return getWrappedEvent().getLoggerName();
    }

    @Override
    @Transient
    public Marker getMarker() {
        return getWrappedEvent().getMarker();
    }

    @Override
    @Transient
    public Message getMessage() {
        return getWrappedEvent().getMessage();
    }

    @Basic
    @Column(name = "message")
    public String getMessageString() {
        return this.getMessage().getFormattedMessage();
    }

    @Override
    @Transient
    public long getMillis() {
        return getWrappedEvent().getMillis();
    }

    @Override
    @Transient
    public StackTraceElement getSource() {
        return getWrappedEvent().getSource();
    }

    @Override
    @Transient
    public String getThreadName() {
        return getWrappedEvent().getThreadName();
    }

    @Override
    @Transient
    public Throwable getThrown() {
        return getWrappedEvent().getThrown();
    }

    public void setEventDate(final Date date) {
        // this entity is write-only
    }

    public void setException(final String exception) {
        // this entity is write-only
    }

    public void setId(final long id) {
        this.id = id;
    }

    public void setMessageString(final String messageString) {
        // this entity is write-only
    }
}
