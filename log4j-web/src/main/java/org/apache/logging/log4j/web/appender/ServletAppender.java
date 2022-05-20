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
package org.apache.logging.log4j.web.appender;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.web.WebLoggerContextUtils;

import javax.servlet.ServletContext;
import java.io.Serializable;

/**
 * Logs using the ServletContext's log method
 */
@Configurable(elementType = Appender.ELEMENT_TYPE, printObject = true)
@Plugin("Servlet")
public class ServletAppender extends AbstractAppender {

	public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
			implements org.apache.logging.log4j.plugins.util.Builder<ServletAppender> {

        @PluginBuilderAttribute
        private boolean logThrowables;

		@Override
		public ServletAppender build() {
			final String name = getName();
			if (name == null) {
				LOGGER.error("No name provided for ServletAppender");
			}
			final ServletContext servletContext = WebLoggerContextUtils.getServletContext();
			if (servletContext == null) {
				LOGGER.error("No servlet context is available");
				return null;
			}
			Layout<? extends Serializable> layout = getLayout();
			if (layout == null) {
				layout = PatternLayout.createDefaultLayout();
			} else if (!(layout instanceof AbstractStringLayout)) {
				LOGGER.error("Layout must be a StringLayout to log to ServletContext");
				return null;
			}
            return new ServletAppender(name, layout, getFilter(), servletContext, isIgnoreExceptions(), logThrowables,
                    getPropertyArray());
		}

        /**
         * Logs with {@link ServletContext#log(String, Throwable)} if true and with {@link ServletContext#log(String)} if false.
         *
         * @return whether to log a Throwable with the servlet context.
         */
        public boolean isLogThrowables() {
            return logThrowables;
        }

        /**
         * Logs with {@link ServletContext#log(String, Throwable)} if true and with {@link ServletContext#log(String)} if false.
         */
        public void setLogThrowables(final boolean logThrowables) {
            this.logThrowables = logThrowables;
        }

	}

    @PluginFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final ServletContext servletContext;
    private final boolean logThrowables;

    private ServletAppender(final String name, final Layout<? extends Serializable> layout, final Filter filter,
            final ServletContext servletContext, final boolean ignoreExceptions, final boolean logThrowables,
            Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.servletContext = servletContext;
        this.logThrowables = logThrowables;
    }

    @Override
    public void append(final LogEvent event) {
        final String serialized = ((AbstractStringLayout) getLayout()).toSerializable(event);
        if (logThrowables) {
            servletContext.log(serialized, event.getThrown());
        } else {
            servletContext.log(serialized);
        }
    }

}
