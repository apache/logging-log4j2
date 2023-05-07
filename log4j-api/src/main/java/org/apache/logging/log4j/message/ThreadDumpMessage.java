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
package org.apache.logging.log4j.message;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.ServiceRegistry;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.Strings;

/**
 * Captures information about all running Threads.
 */
@AsynchronouslyFormattable
public class ThreadDumpMessage implements Message, StringBuilderFormattable {
    private static final Lazy<ThreadInfoFactory> FACTORY = Lazy.lazy(() -> {
        final var services = ServiceRegistry.getInstance()
                .getServices(ThreadInfoFactory.class, MethodHandles.lookup(), null);
        return services.isEmpty() ? new BasicThreadInfoFactory() : services.get(0);
    });

    private final Map<ThreadInformation, StackTraceElement[]> threads;
    private final String title;

    /**
     * Generate a ThreadDumpMessage with a title.
     * @param title The title.
     */
    public ThreadDumpMessage(final String title) {
        this.title = title == null ? Strings.EMPTY : title;
        threads = FACTORY.get().createThreadInfo();
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
     * and the StackTraceElement array as the value;
     * @return the "parameters" to this Message.
     */
    @Override
    public Object[] getParameters() {
        return null;
    }

    /**
     * Factory to create Thread information.
     * <p>
     * Implementations of this class are loaded via the standard java Service Provider interface.
     * </p>
     */
    public interface ThreadInfoFactory {
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
