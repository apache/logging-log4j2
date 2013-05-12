package org.apache.logging.log4j.core.appender.db.jpa;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.db.jpa.converter.ContextMapJsonAttributeConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Map;

@Entity
@Table(name = "jpaBasicLogEntry")
@SuppressWarnings("unused")
public class TestBasicEntity extends BasicLogEventEntity {
    private static final long serialVersionUID = 1L;

    private long id = 0L;

    public TestBasicEntity() {
        super();
    }

    public TestBasicEntity(final LogEvent wrapped) {
        super(wrapped);
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

    @Override
    @Convert(converter = ContextMapJsonAttributeConverter.class)
    @Column(name = "contextMapJson")
    public Map<String, String> getContextMap() {
        return super.getContextMap();
    }

    @Override
    @Transient
    public StackTraceElement getSource() {
        return super.getSource();
    }

    @Override
    @Transient
    public Marker getMarker() {
        return super.getMarker();
    }

    @Override
    @Transient
    public String getThreadName() {
        return super.getThreadName();
    }

    @Override
    @Transient
    public ThreadContext.ContextStack getContextStack() {
        return super.getContextStack();
    }

    @Override
    @Transient
    public String getFQCN() {
        return super.getFQCN();
    }
}
