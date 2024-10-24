/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.AbstractLayout;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedFormat;

/**
 * The default configuration writes all output to the Console using the default logging level. You configure default
 * logging level by setting the system property "org.apache.logging.log4j.level" to a level name. If you do not
 * specify the property, Log4j uses the ERROR Level. Log Events will be printed using the basic formatting provided
 * by each Message.
 */
public class DefaultConfiguration extends AbstractConfiguration {

    /**
     * The name of the default configuration.
     */
    public static final String DEFAULT_NAME = "Default";

    /**
     * The System Property used to specify the logging level.
     */
    public static final String DEFAULT_LEVEL = "org.apache.logging.log4j.level";

    /**
     * The default Pattern used for the default Layout.
     */
    public static final String DEFAULT_PATTERN = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";

    /**
     * Constructor to create the default configuration.
     */
    public DefaultConfiguration() {
        super(null, ConfigurationSource.NULL_SOURCE);
        setToDefault();
    }

    @Override
    protected void doConfigure() {}

    static Layout<? extends String> createDefaultLayout() {
        return new DefaultLayout();
    }

    /**
     * A simple layout used only by {@link DefaultConfiguration}
     * <p>
     *   This layout allows to create applications that don't contain {@link org.apache.logging.log4j.core.layout.PatternLayout}
     *   and all its patterns, e.g. GraalVM applications.
     * </p>
     */
    private static final class DefaultLayout extends AbstractLayout<String> {

        private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

        private final FixedDateFormat dateFormat = FixedDateFormat.create(FixedFormat.ABSOLUTE);

        private DefaultLayout() {
            super(null, EMPTY_BYTE_ARRAY, EMPTY_BYTE_ARRAY);
        }

        @Override
        public String toSerializable(LogEvent event) {
            try (Writer sw = new StringBuilderWriter();
                    PrintWriter pw = new PrintWriter(sw)) {
                pw.append(dateFormat.format(event.getTimeMillis()))
                        .append(" [")
                        .append(event.getThreadName())
                        .append("] ")
                        .append(event.getLevel().toString())
                        .append(" ")
                        .append(event.getLoggerName())
                        .append(" - ")
                        .append(event.getMessage().getFormattedMessage())
                        .append("\n");
                Throwable throwable = event.getThrown();
                if (throwable != null) {
                    throwable.printStackTrace(pw);
                }
                return sw.toString();
            } catch (IOException e) {
                throw new LoggingException(e);
            }
        }

        @Override
        public byte[] toByteArray(LogEvent event) {
            return toSerializable(event).getBytes(Charset.defaultCharset());
        }

        @Override
        public String getContentType() {
            return "text/plain";
        }
    }
}
