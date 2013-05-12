package org.apache.logging.log4j.core.appender.db.jpa;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.db.jpa.converter.ContextMapAttributeConverter;
import org.apache.logging.log4j.core.appender.db.jpa.converter.ContextStackAttributeConverter;
import org.apache.logging.log4j.core.appender.db.jpa.converter.MarkerAttributeConverter;
import org.apache.logging.log4j.core.appender.db.jpa.converter.MessageAttributeConverter;
import org.apache.logging.log4j.core.appender.db.jpa.converter.StackTraceElementAttributeConverter;
import org.apache.logging.log4j.core.appender.db.jpa.converter.ThrowableAttributeConverter;
import org.apache.logging.log4j.message.Message;

import javax.persistence.Basic;
import javax.persistence.Convert;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import java.util.Map;

/**
 * Users of the JPA appender may want to extend this class instead of {@link LogEventWrapperEntity}. This class
 * implements all of the required mutator methods but does not implement a mutable entity ID property. In order to
 * create an entity based on this class, you need only create two constructors matching this class's
 * constructors, annotate the class {@link javax.persistence.Entity @Entity} and {@link javax.persistence.Table @Table},
 * and implement the fully mutable entity ID property annotated with {@link javax.persistence.Id @Id} and
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
 * @see LogEventWrapperEntity
 */
@MappedSuperclass
public abstract class LogEventEntity extends LogEventWrapperEntity {
    @SuppressWarnings("unused") // JPA requires this
    public LogEventEntity() {
        super();
    }

    public LogEventEntity(final LogEvent wrapped) {
        super(wrapped);
    }

    @Override
    @Basic
    @Enumerated(EnumType.STRING)
    public Level getLevel() {
        return this.getWrappedEvent().getLevel();
    }

    @Override
    @Basic
    public String getLoggerName() {
        return this.getWrappedEvent().getLoggerName();
    }

    @Override
    @Convert(converter = StackTraceElementAttributeConverter.class)
    public StackTraceElement getSource() {
        return this.getWrappedEvent().getSource();
    }

    @Override
    @Convert(converter = MessageAttributeConverter.class)
    public Message getMessage() {
        return this.getWrappedEvent().getMessage();
    }

    @Override
    @Convert(converter = MarkerAttributeConverter.class)
    public Marker getMarker() {
        return this.getWrappedEvent().getMarker();
    }

    @Override
    @Basic
    public String getThreadName() {
        return this.getWrappedEvent().getThreadName();
    }

    @Override
    @Basic
    public long getMillis() {
        return this.getWrappedEvent().getMillis();
    }

    @Override
    @Convert(converter = ThrowableAttributeConverter.class)
    public Throwable getThrown() {
        return this.getWrappedEvent().getThrown();
    }

    @Override
    @Convert(converter = ContextMapAttributeConverter.class)
    public Map<String, String> getContextMap() {
        return this.getWrappedEvent().getContextMap();
    }

    @Override
    @Convert(converter = ContextStackAttributeConverter.class)
    public ThreadContext.ContextStack getContextStack() {
        return this.getWrappedEvent().getContextStack();
    }

    @Override
    @Basic
    public String getFQCN() {
        return this.getWrappedEvent().getFQCN();
    }
}
