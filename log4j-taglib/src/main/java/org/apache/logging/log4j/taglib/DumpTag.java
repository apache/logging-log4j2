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

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * This class implements the {@code <log:dump>} tag.
 *
 * @since 2.0
 */
public class DumpTag extends TagSupport {
    private static final long serialVersionUID = 1L;

    private int scope;

    public DumpTag() {
        super();
        init();
    }

    @Override
    public void release() {
        super.release();
        this.init();
    }

    private void init() {
        this.scope = PageContext.PAGE_SCOPE;
    }

    public void setScope(final String scope) {
        this.scope = TagUtils.getScope(scope);
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            final Enumeration<String> names = this.pageContext.getAttributeNamesInScope(this.scope);
            this.pageContext.getOut().write("<dl>");
            while (names != null && names.hasMoreElements()) {
                final String name = names.nextElement();
                final Object value = this.pageContext.getAttribute(name, this.scope);

                this.pageContext.getOut().write("<dt><code>" + name + "</code></dt>");
                this.pageContext.getOut().write("<dd><code>" + value + "</code></dd>");
            }
            this.pageContext.getOut().write("</dl>");
        } catch (final IOException e) {
            throw new JspException("Could not write scope contents. Cause:  " + e.toString(), e);
        }

        return Tag.EVAL_PAGE;
    }
}
