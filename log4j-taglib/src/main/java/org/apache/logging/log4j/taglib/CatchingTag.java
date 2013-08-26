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

/**
 * This class implements the {@code <log:catching>} tag.
 *
 * @since 2.0
 */
public class CatchingTag extends ExceptionAwareTagSupport {
    private static final long serialVersionUID = 1L;

    private static final String FQCN = CatchingTag.class.getName();

    private Level level;

    @Override
    protected void init() {
        super.init();
        this.level = null;
    }

    public void setLevel(final Object level) {
        this.level = TagUtils.resolveLevel(level);
    }

    @Override
    public int doEndTag() throws JspException {
        final Log4jTaglibLogger logger = this.getLogger();

        if (this.level == null) {
            logger.catching(FQCN, Level.ERROR, this.getException());
        } else {
            logger.catching(FQCN, this.level, this.getException());
        }

        return Tag.EVAL_PAGE;
    }
}
