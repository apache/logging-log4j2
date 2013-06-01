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
import javax.servlet.jsp.tagext.Tag;

import org.apache.logging.log4j.message.MessageFactory;

/**
 * This class implements the {@code <log:setLogger>} tag.
 *
 * @since 2.0
 */
public class SetLoggerTag extends BodyTagSupport {
    private static final long serialVersionUID = 1L;

    private transient Log4jTaglibLoggerContext loggerContext;

    private transient Object logger;

    private transient MessageFactory factory;

    private String var;

    private int scope;

    public SetLoggerTag() {
        super();
        init();
    }

    private void init() {
        this.logger = null;
        this.var = null;
        this.scope = PageContext.PAGE_SCOPE;
    }

    @Override
    public void release() {
        super.release();
        this.init();
    }

    @Override
    public void setPageContext(final PageContext pageContext) {
        super.setPageContext(pageContext);
        this.loggerContext = Log4jTaglibLoggerContext.getInstance(pageContext.getServletContext());
    }

    public void setLogger(final Object logger) {
        this.logger = logger;
    }

    public void setFactory(final MessageFactory factory) {
        this.factory = factory;
    }

    public void setVar(final String var) {
        this.var = var;
    }

    public void setScope(final String scope) {
        this.scope = TagUtils.getScope(scope);
    }

    @Override
    public int doEndTag() throws JspException {
        final Log4jTaglibLogger logger = TagUtils.resolveLogger(this.loggerContext, this.logger, this.factory);

        if (this.var != null) {
            this.pageContext.setAttribute(this.var, logger, this.scope);
        } else {
            TagUtils.setDefaultLogger(this.pageContext, logger);
        }

        return Tag.EVAL_PAGE;
    }
}
