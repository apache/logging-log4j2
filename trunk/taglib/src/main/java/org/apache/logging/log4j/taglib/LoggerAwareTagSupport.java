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
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * An abstract class for all tags that are logger-aware.
 *
 * @since 2.0
 */
abstract class LoggerAwareTagSupport extends BodyTagSupport {
    private static final long serialVersionUID = 1L;

    private transient Log4jTaglibLoggerContext loggerContext;

    private transient Object logger;

    protected LoggerAwareTagSupport() {
        this.init();
    }

    protected void init() {
        this.logger = null;
    }

    @Override
    public final void release() {
        super.release();
        this.init();
    }

    @Override
    public final void setPageContext(final PageContext pageContext) {
        super.setPageContext(pageContext);
        this.loggerContext = Log4jTaglibLoggerContext.getInstance(pageContext.getServletContext());
    }

    protected final Log4jTaglibLogger getLogger() throws JspException {
        if (this.logger != null) {
            return TagUtils.resolveLogger(this.loggerContext, this.logger, null);
        }
        Log4jTaglibLogger logger = TagUtils.getDefaultLogger(this.pageContext);
        if (logger == null) {
            final String name = this.pageContext.getPage().getClass().getName();
            logger = TagUtils.resolveLogger(this.loggerContext, name, null);
            TagUtils.setDefaultLogger(this.pageContext, logger);
        }
        return logger;
    }

    public final void setLogger(final Object logger) {
        this.logger = logger;
    }
}
