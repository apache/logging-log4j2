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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.util.PropertiesUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;

/**
 * ConsoleAppender appends log events to <code>System.out</code> or
 * <code>System.err</code> using a layout specified by the user. The
 * default target is <code>System.out</code>.
 * @doubt accessing System.out or .err as a byte stream instead of a writer
 *    bypasses the JVM's knowledge of the proper encoding. (RG) Encoding
 * is handled within the Layout. Typically, a Layout will generate a String
 * and then call getBytes which may use a configured encoding or the system
 * default. OTOH, a Writer cannot print byte streams.
 */
@Plugin(name = "Console", type = "Core", elementType = "appender", printObject = true)
public final class ConsoleAppender extends AbstractOutputStreamAppender {

    private static ConsoleManagerFactory factory = new ConsoleManagerFactory();

    /**
     * Enumeration of console destinations.
     */
    public enum Target {
        /** Standard output. */
        SYSTEM_OUT,
        /** Standard error output. */
        SYSTEM_ERR
    }

    private ConsoleAppender(final String name, final Layout layout, final Filter filter,
                            final OutputStreamManager manager,
                            final boolean handleExceptions) {
        super(name, layout, filter, handleExceptions, true, manager);
    }

    /**
     * Create a Console Appender.
     * @param layout The layout to use (required).
     * @param filter The Filter or null.
     * @param t The target ("SYSTEM_OUT" or "SYSTEM_ERR"). The default is "SYSTEM_OUT".
     * @param follow If true will follow changes to the underlying output stream.
     * @param name The name of the Appender (required).
     * @param suppress "true" if exceptions should be hidden from the application, "false" otherwise.
     * The default is "true".
     * @return The ConsoleAppender.
     */
    @PluginFactory
    public static ConsoleAppender createAppender(@PluginElement("layout") Layout layout,
                                                 @PluginElement("filters") final Filter filter,
                                                 @PluginAttr("target") final String t,
                                                 @PluginAttr("name") final String name,
                                                 @PluginAttr("follow") final String follow,
                                                 @PluginAttr("suppressExceptions") final String suppress) {
        if (name == null) {
            LOGGER.error("No name provided for ConsoleAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createLayout(null, null, null, null);
        }
        final boolean isFollow = follow == null ? false : Boolean.valueOf(follow);
        final boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);
        final Target target = t == null ? Target.SYSTEM_OUT : Target.valueOf(t);
        return new ConsoleAppender(name, layout, filter, getManager(isFollow, target), handleExceptions);
    }

    private static OutputStreamManager getManager(final boolean follow, final Target target) {
        final String type = target.name();
        final OutputStream os = getOutputStream(follow, target);
        return OutputStreamManager.getManager(target.name() + "." + follow, new FactoryData(os, type), factory);
    }

    private static OutputStream getOutputStream(final boolean follow, final Target target) {
        final PrintStream printStream = target == Target.SYSTEM_OUT ?
            follow ? new PrintStream(new SystemOutStream()) : System.out :
            follow ? new PrintStream(new SystemErrStream()) : System.err;
        PropertiesUtil propsUtil = PropertiesUtil.getProperties();
        if (!propsUtil.getStringProperty("os.name").startsWith("Windows") ||
            propsUtil.getBooleanProperty("log4j.skipJansi")) {
            return printStream;
        } else {
            try {
                final ClassLoader loader = Loader.getClassLoader();
                // We type the parameter as a wildcard to avoid a hard reference to Jansi.
                final Class<?> clazz = loader.loadClass("org.fusesource.jansi.WindowsAnsiOutputStream");
                final Constructor<?> constructor = clazz.getConstructor(OutputStream.class);
                return (OutputStream) constructor.newInstance(printStream);
            } catch (final ClassNotFoundException cnfe) {
                LOGGER.debug("Jansi is not installed");
            } catch (final NoSuchMethodException nsme) {
                LOGGER.warn("WindowsAnsiOutputStream is missing the proper constructor");
            } catch (final Exception ex) {
                LOGGER.warn("Unable to instantiate WindowsAnsiOutputStream");
            }
            return printStream;
        }
    }

    /**
     * An implementation of OutputStream that redirects to the
     * current System.err.
     *
     */
    private static class SystemErrStream extends OutputStream {
        public SystemErrStream() {
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
            System.err.flush();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.err.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len)
            throws IOException {
            System.err.write(b, off, len);
        }

        @Override
        public void write(final int b) {
            System.err.write(b);
        }
    }

    /**
     * An implementation of OutputStream that redirects to the
     * current System.out.
     *
     */
    private static class SystemOutStream extends OutputStream {
        public SystemOutStream() {
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
            System.out.flush();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.out.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len)
            throws IOException {
            System.out.write(b, off, len);
        }

        @Override
        public void write(final int b) throws IOException {
            System.out.write(b);
        }
    }

    /**
     * Data to pass to factory method.
     */
    private static class FactoryData {
        private final OutputStream os;
        private final String type;

        /**
         * Constructor.
         * @param os The OutputStream.
         * @param type The name of the target.
         */
        public FactoryData(final OutputStream os, final String type) {
            this.os = os;
            this.type = type;
        }
    }

    /**
     * Factory to create the Appender.
     */
    private static class ConsoleManagerFactory implements ManagerFactory<OutputStreamManager, FactoryData> {

        /**
         * Create an OutputStreamManager.
         * @param name The name of the entity to manage.
         * @param data The data required to create the entity.
         * @return The OutputStreamManager
         */
        public OutputStreamManager createManager(final String name, final FactoryData data) {
            return new OutputStreamManager(data.os, data.type);
        }
    }

}
