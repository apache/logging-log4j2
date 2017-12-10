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
package org.apache.logging.log4j.message;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.Strings;

/**
 * Captures information about all running Threads.
 */
@AsynchronouslyFormattable
public class ThreadDumpMessage implements Message, StringBuilderFormattable {
    private static final long serialVersionUID = -1103400781608841088L;
    private static ThreadInfoFactory FACTORY;

    private volatile Map<ThreadInformation, StackTraceElement[]> threads;
    private final String title;
    private String formattedMessage;
    private SourceLocation source;

    /**
     * Generate a ThreadDumpMessage with a title.
     * @param title The title.
     */
    public ThreadDumpMessage(final String title) {
        this((SourceLocation) null, title);
    }

    public ThreadDumpMessage(SourceLocation source, final String title) {
        this.title = title == null ? Strings.EMPTY : title;
        threads = getFactory().createThreadInfo();
        this.source = source;
    }

    private ThreadDumpMessage(final String formattedMsg, final String title) {
        this(null, formattedMsg, title);
    }

    private ThreadDumpMessage(SourceLocation source, final String formattedMsg, final String title) {
        this.source = source;
        this.formattedMessage = formattedMsg;
        this.title = title == null ? Strings.EMPTY : title;
    }

    private static ThreadInfoFactory getFactory() {
        if (FACTORY == null) {
            FACTORY = initFactory(ThreadDumpMessage.class.getClassLoader());
        }
        return FACTORY;
    }

    private static ThreadInfoFactory initFactory(final ClassLoader classLoader) {
        final ServiceLoader<ThreadInfoFactory> serviceLoader = ServiceLoader.load(ThreadInfoFactory.class, classLoader);
        ThreadInfoFactory result = null;
        try {
            final Iterator<ThreadInfoFactory> iterator = serviceLoader.iterator();
            while (result == null && iterator.hasNext()) {
                result = iterator.next();
            }
        } catch (ServiceConfigurationError | LinkageError | Exception unavailable) { // if java management classes not available
            StatusLogger.getLogger().info("ThreadDumpMessage uses BasicThreadInfoFactory: " +
                            "could not load extended ThreadInfoFactory: {}", unavailable.toString());
            result = null;
        }
        return result == null ? new BasicThreadInfoFactory() : result;
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }

    /**
     * Returns the ThreadDump in printable format.
     * @return the ThreadDump suitable for logging.
     */
    @Override
    public String getFormattedMessage() {
        if (formattedMessage != null) {
            return formattedMessage;
        }
        final StringBuilder sb = new StringBuilder(255);
        formatTo(sb);
        return sb.toString();
    }

    @Override
    public void formatTo(final StringBuilder sb) {
        sb.append(title);
        if (title.length() > 0) {
            sb.append('\n');
        }
        for (final Map.Entry<ThreadInformation, StackTraceElement[]> entry : threads.entrySet()) {
            final ThreadInformation info = entry.getKey();
            info.printThreadInfo(sb);
            info.printStack(sb, entry.getValue());
            sb.append('\n');
        }
    }

    /**
     * Returns the title.
     * @return the title.
     */
    @Override
    public String getFormat() {
        return title == null ? Strings.EMPTY : title;
    }

    /**
     * Returns an array with a single element, a Map containing the ThreadInformation as the key.
     * and the SourceLocation array as the value;
     * @return the "parameters" to this Message.
     */
    @Override
    public Object[] getParameters() {
        return null;
    }

        /**
     * Creates a ThreadDumpMessageProxy that can be serialized.
     * @return a ThreadDumpMessageProxy.
     */
    protected Object writeReplace() {
        return new ThreadDumpMessageProxy(this);
    }

    private void readObject(final ObjectInputStream stream)
        throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }

    /**
     * Proxy pattern used to serialize the ThreadDumpMessage.
     */
    private static class ThreadDumpMessageProxy implements Serializable {

        private static final long serialVersionUID = -3476620450287648269L;
        private final String formattedMsg;
        private final String title;

        ThreadDumpMessageProxy(final ThreadDumpMessage msg) {
            this.formattedMsg = msg.getFormattedMessage();
            this.title = msg.title;
        }

        /**
         * Returns a ThreadDumpMessage using the data in the proxy.
         * @return a ThreadDumpMessage.
         */
        protected Object readResolve() {
            return new ThreadDumpMessage(formattedMsg, title);
        }
    }

    /**
     * Factory to create Thread information.
     * <p>
     * Implementations of this class are loaded via the standard java Service Provider interface.
     * </p>
     * @see /log4j-core/src/main/resources/META-INF/services/org.apache.logging.log4j.message.ThreadDumpMessage$ThreadInfoFactory
     */
    public static interface ThreadInfoFactory {
        Map<ThreadInformation, StackTraceElement[]> createThreadInfo();
    }

    /**
     * Factory to create basic thread information.
     */
    private static class BasicThreadInfoFactory implements ThreadInfoFactory {
        @Override
        public Map<ThreadInformation, StackTraceElement[]> createThreadInfo() {
            final Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
            final Map<ThreadInformation, StackTraceElement[]> threads =
                new HashMap<>(map.size());
            for (final Map.Entry<Thread, StackTraceElement[]> entry : map.entrySet()) {
                threads.put(new BasicThreadInformation(entry.getKey()), entry.getValue());
            }
            return threads;
        }
    }

    @Override
    public SourceLocation getSource() {
        return source;
    }

    /**
     * Always returns null.
     *
     * @return null
     */
    @Override
    public Throwable getThrowable() {
        return null;
    }
}
