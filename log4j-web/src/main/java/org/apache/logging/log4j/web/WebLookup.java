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
package org.apache.logging.log4j.web;
// Please note that if you move this class, make sure to update the Interpolator class (if still applicable) or remove
// this comment if no longer relevant

import javax.servlet.ServletContext;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.AbstractLookup;
import org.apache.logging.log4j.util.Strings;

@Plugin(name = "web", category = "Lookup")
public class WebLookup extends AbstractLookup {
    private static final String ATTR_PREFIX = "attr.";
    private static final String INIT_PARAM_PREFIX = "initParam.";

    @Override
    public String lookup(final LogEvent event, final String key) {
        final ServletContext ctx = WebLoggerContextUtils.getServletContext();
        if (ctx == null) {
            return null;
        }

        if (key.startsWith(ATTR_PREFIX)) {
            final String attrName = key.substring(ATTR_PREFIX.length());
            final Object attrValue = ctx.getAttribute(attrName);
            return attrValue == null ? null : attrValue.toString();
        }

        if (key.startsWith(INIT_PARAM_PREFIX)) {
            final String paramName = key.substring(INIT_PARAM_PREFIX.length());
            return ctx.getInitParameter(paramName);
        }

        if ("rootDir".equals(key)) {
            final String root = ctx.getRealPath("/");
            if (root == null) {
                final String msg = "Failed to resolve web:rootDir -- " +
                        "servlet container unable to translate virtual path " +
                        " to real path (probably not deployed as exploded";
                throw new IllegalStateException(msg);
            }
            return root;
        }

        if ("contextPath".equals(key)) {
            return ctx.getContextPath();
        }

        if ("servletContextName".equals(key)) {
            return ctx.getServletContextName();
        }

        if ("serverInfo".equals(key)) {
            return ctx.getServerInfo();
        }

        if ("effectiveMajorVersion".equals(key)) {
            return String.valueOf(ctx.getEffectiveMajorVersion());
        }

        if ("effectiveMinorVersion".equals(key)) {
            return String.valueOf(ctx.getEffectiveMinorVersion());
        }

        if ("majorVersion".equals(key)) {
            return String.valueOf(ctx.getMajorVersion());
        }

        if ("minorVersion".equals(key)) {
            return String.valueOf(ctx.getMinorVersion());
        }

        if (ctx.getAttribute(key) != null) {
            return ctx.getAttribute(key).toString();
        }

        if (ctx.getInitParameter(key) != null) {
            return ctx.getInitParameter(key);
        }

        ctx.log(getClass().getName() + " unable to resolve key " + Strings.quote(key));
        return null;
    }
}
