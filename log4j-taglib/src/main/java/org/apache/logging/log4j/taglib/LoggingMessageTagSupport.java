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
package org.apache.logging.log4j.taglib;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.Tag;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;

/**
 * Implements common methods for logging tags that accept messages and markers.
 *
 * @since 2.0
 */
abstract class LoggingMessageTagSupport extends ExceptionAwareTagSupport implements DynamicAttributes {
    private static final long serialVersionUID = 1L;

    private static final String FQCN = LoggingMessageTagSupport.class.getName();

    private transient Object message;

    private Marker marker;

    private List<Object> attributes;

    @Override
    protected void init() {
        super.init();
        this.message = null;
        this.marker = null;
        if (this.attributes == null) {
            this.attributes = new ArrayList<>();
        } else {
            this.attributes.clear();
        }
    }

    protected final Object getMessage() throws JspException {
        if (this.message == null) {
            if (this.getBodyContent() == null) {
                throw new JspException("Either message attribute or body content must be specified.");
            }
            return this.getBodyContent().getString();
        }
        return this.message;
    }

    public final void setMessage(final Object message) {
        this.message = message;
    }

    protected final Marker getMarker() {
        return this.marker;
    }

    public final void setMarker(final Marker marker) {
        this.marker = marker;
    }

    protected abstract Level getLevel();

    @Override
    public final void setDynamicAttribute(final String uri, final String name, final Object value) {
        this.attributes.add(value);
    }

    @Override
    public final int doStartTag() {
        return BodyTag.EVAL_BODY_BUFFERED;
    }

    @Override
    public final int doEndTag() throws JspException {
        final Log4jTaglibLogger logger = this.getLogger();
        final Level level = this.getLevel();
        final Marker marker = this.getMarker();

        if (TagUtils.isEnabled(logger, level, marker)) {
            final Object message = this.getMessage();
            final Throwable exception = this.getException();
            if (message instanceof Message) {
                logger.logIfEnabled(FQCN, level, marker, (Message) message, exception);
            } else if (message instanceof String) {
                Message data;
                if (this.attributes.size() > 0) {
                    data = logger.getMessageFactory().newMessage((String) message, this.attributes.toArray());
                } else {
                    data = logger.getMessageFactory().newMessage((String) message);
                }
                logger.logIfEnabled(FQCN, level, marker, data, exception);
            } else {
                logger.logIfEnabled(FQCN, level, marker, logger.getMessageFactory().newMessage(message), exception);
            }
        }

        return Tag.EVAL_PAGE;
    }
}
