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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;

/**
 * This class implements the {@code <log:ifEnabled>} tag.
 *
 * @since 2.0
 */
public class IfEnabledTag extends LoggerAwareTagSupport {
    private static final long serialVersionUID = 1L;

    private transient Object level;

    private Marker marker;

    @Override
    protected void init() {
        super.init();
        this.level = null;
        this.marker = null;
    }

    public final void setLevel(final Object level) {
        this.level = level;
    }

    public final void setMarker(final Marker marker) {
        this.marker = marker;
    }

    @Override
    public int doStartTag() throws JspException {
        final Level level = TagUtils.resolveLevel(this.level);
        if (level == null) {
            throw new JspException("Level must be of type String or org.apache.logging.log4j.Level.");
        }

        return TagUtils.isEnabled(this.getLogger(), level, this.marker) ? Tag.EVAL_BODY_INCLUDE : Tag.SKIP_BODY;
    }
}
