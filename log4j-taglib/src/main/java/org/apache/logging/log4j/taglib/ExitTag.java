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
 * This class implements the {@code <log:exit>} tag.
 *
 * @since 2.0
 */
public class ExitTag extends LoggerAwareTagSupport {
    private static final long serialVersionUID = 1L;

    private static final String FQCN = ExitTag.class.getName();

    private transient Object result;

    @Override
    protected void init() {
        super.init();
        this.result = null;
    }

    public void setResult(final Object result) {
        this.result = result;
    }

    @Override
    public int doEndTag() throws JspException {
        final Log4jTaglibLogger logger = this.getLogger();

        if (TagUtils.isEnabled(logger, Level.TRACE, null)) {
            if (this.result == null) {
                logger.exit(FQCN, null);
            } else {
                logger.exit(FQCN, this.result);
            }
        }

        return Tag.EVAL_PAGE;
    }
}
