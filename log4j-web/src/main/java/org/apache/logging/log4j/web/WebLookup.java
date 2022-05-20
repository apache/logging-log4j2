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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.AbstractLookup;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.Strings;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Objects;
import java.util.stream.Stream;

@Namespace("Lookup")
@Plugin("web")
public class WebLookup extends AbstractLookup {
    private static final String SESSION_ATTR_PREFIX = "session.attr.";
    private static final String REQUEST_ATTR_PREFIX = "request.attr.";
    private static final String REQUEST_HEADER_PREFIX = "header.";
    private static final String REQUEST_COOKIE_PREFIX = "cookie.";
    private static final String REQUEST_PARAMETER_PREFIX = "request.parameter.";
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

        if (key.startsWith(REQUEST_ATTR_PREFIX)) {
            final String name = key.substring(REQUEST_ATTR_PREFIX.length());
            final ServletRequest req = getRequest();
            final Object value = req == null ? null : req.getAttribute(name);
            return value == null ? null : String.valueOf(value);
        }

        if (key.startsWith(REQUEST_HEADER_PREFIX)) {
            final String name = key.substring(REQUEST_HEADER_PREFIX.length());
            final ServletRequest req = getRequest();
            return HttpServletRequest.class.isInstance(req) ? HttpServletRequest.class.cast(req).getHeader(name) : null;
        }

        if (key.startsWith(REQUEST_COOKIE_PREFIX)) {
            final String name = key.substring(REQUEST_COOKIE_PREFIX.length());
            final ServletRequest req = getRequest();
            return HttpServletRequest.class.isInstance(req) ?
                    Stream.of(HttpServletRequest.class.cast(req).getCookies())
                        .filter(c -> name.equals(c.getName()))
                        .findFirst()
                        .map(Cookie::getValue)
                        .orElse(null) :
                    null;
        }

        if (key.startsWith(REQUEST_PARAMETER_PREFIX)) {
            final String name = key.substring(REQUEST_PARAMETER_PREFIX.length());
            final ServletRequest req = getRequest();
            return HttpServletRequest.class.isInstance(req) ?
                    HttpServletRequest.class.cast(req).getParameter(name) : null;
        }

        if (key.startsWith(SESSION_ATTR_PREFIX)) {
            final ServletRequest req = getRequest();
            if (HttpServletRequest.class.isInstance(req)) {
                final HttpSession session = HttpServletRequest.class.cast(req).getSession(false);
                if (session != null) {
                    final String name = key.substring(SESSION_ATTR_PREFIX.length());
                    return Objects.toString(session.getAttribute(name), null);
                }
            }
            return null;
        }

        if ("request.method".equals(key)) {
            final ServletRequest req = getRequest();
            return HttpServletRequest.class.isInstance(req) ? HttpServletRequest.class.cast(req).getMethod() : null;
        }

        if ("request.uri".equals(key)) {
            final ServletRequest req = getRequest();
            return HttpServletRequest.class.isInstance(req) ? HttpServletRequest.class.cast(req).getRequestURI() : null;
        }

        if ("request.url".equals(key)) {
            final ServletRequest req = getRequest();
            return HttpServletRequest.class.isInstance(req) ?
                    HttpServletRequest.class.cast(req).getRequestURL().toString() : null;
        }

        if ("request.remoteAddress".equals(key)) {
            final ServletRequest req = getRequest();
            return HttpServletRequest.class.isInstance(req) ?
                    HttpServletRequest.class.cast(req).getRemoteAddr() : null;
        }

        if ("request.remoteHost".equals(key)) {
            final ServletRequest req = getRequest();
            return HttpServletRequest.class.isInstance(req) ?
                    HttpServletRequest.class.cast(req).getRemoteHost() : null;
        }

        if ("request.remotePort".equals(key)) {
            final ServletRequest req = getRequest();
            return HttpServletRequest.class.isInstance(req) ?
                    Integer.toString(HttpServletRequest.class.cast(req).getRemotePort()) : null;
        }

        if ("request.principal".equals(key)) {
            final ServletRequest req = getRequest();
            final Principal pcp = HttpServletRequest.class.isInstance(req) ?
                    HttpServletRequest.class.cast(req).getUserPrincipal() : null;
            return pcp == null ? null : pcp.getName();
        }

        if ("session.id".equals(key)) {
            final ServletRequest req = getRequest();
            final HttpSession session = HttpServletRequest.class.isInstance(req) ?
                    HttpServletRequest.class.cast(req).getSession(false) : null;
            return session == null ? null : session.getId();
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

        if ("contextPathName".equals(key)) {
            String path = ctx.getContextPath();
            if (path.trim().contains("/")) {
                String[] fields = path.split("/");
                for (String field : fields) {
                    if (field.length() > 0) {
                        return field;
                    }
                }
                return null;

            }
            return ctx.getContextPath();
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

    private ServletRequest getRequest() {
        final ServletRequest servletRequest = Log4jServletFilter.CURRENT_REQUEST.get();
        if (servletRequest == null) { // don't leak the thread map
            Log4jServletFilter.CURRENT_REQUEST.remove();
        }
        return servletRequest;
    }
}
