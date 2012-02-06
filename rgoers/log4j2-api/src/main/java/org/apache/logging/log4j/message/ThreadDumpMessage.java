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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Captures information about all running Threads.
 */
public class ThreadDumpMessage implements Message {

    private static final long serialVersionUID = -1103400781608841088L;

    private static ThreadInfoFactory factory;

    private volatile Map<ThreadInformation, StackTraceElement[]> threads;

    private final String title;

    private String formattedMessage = null;

    static {
        Method[] methods = ThreadInfo.class.getMethods();
        boolean basic = true;
        for (Method method : methods) {
            if (method.getName().equals("getLockInfo")) {
                basic = false;
                break;
            }
        }
        factory = basic ? new BasicThreadInfoFactory() : new ExtendedThreadInfoFactory();
    }

    /**
     * Generate a ThreadDumpMessage with no title.
     */
    public ThreadDumpMessage() {
        this(null);

    }

    /**
     * Generate a ThreadDumpMessage with a title.
     * @param title The title.
     */
    public ThreadDumpMessage(String title) {
        this.title = title == null ? "" : title;
        threads = factory.createThreadInfo();
    }

    private ThreadDumpMessage(String formattedMsg, String title) {
        this.formattedMessage = formattedMsg;
        this.title = title;
    }

    /**
     * Return the ThreadDump in printable format.
     * @return the ThreadDump suitable for logging.
     */
    public String getFormattedMessage() {
        if (formattedMessage != null) {
            return formattedMessage;
        }
        StringBuilder sb = new StringBuilder(title);
        if (title.length() > 0) {
            sb.append("\n");
        }
        for (Map.Entry<ThreadInformation, StackTraceElement[]> entry : threads.entrySet()) {
            ThreadInformation info = entry.getKey();
            info.printThreadInfo(sb);
            info.printStack(sb, entry.getValue());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Returns the title.
     * @return the title.
     */
    public String getMessageFormat() {
        return title == null ? "" : title;
    }

    /**
     * Returns an array with a single element, a Map containing the ThreadInformation as the key.
     * and the StackTraceElement array as the value;
     * @return the "parameters" to this Message.
     */
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

    private void readObject(ObjectInputStream stream)
        throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }

    /**
     * Proxy pattern used to serialize the ThreadDumpMessage.
     */
    private static class ThreadDumpMessageProxy implements Serializable {

        private static final long serialVersionUID = -3476620450287648269L;
        private String formattedMsg;
        private String title;

        public ThreadDumpMessageProxy(ThreadDumpMessage msg) {
            this.formattedMsg = msg.getFormattedMessage();
            this.title = msg.title;
        }

        /**
         * Return a ThreadDumpMessage using the data in the proxy.
         * @return a ThreadDumpMessage.
         */
        protected Object readResolve() {
            return new ThreadDumpMessage(formattedMsg, title);
        }
    }

    /**
     * Factory to create Thread information.
     */
    private interface ThreadInfoFactory {
        Map<ThreadInformation, StackTraceElement[]> createThreadInfo();
    }

    /**
     * Factory to create basic thread information.
     */
    private static class BasicThreadInfoFactory implements ThreadInfoFactory {
        public Map<ThreadInformation, StackTraceElement[]> createThreadInfo() {
            Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
            Map<ThreadInformation, StackTraceElement[]> threads =
                new HashMap<ThreadInformation, StackTraceElement[]>(map.size());
            for (Map.Entry<Thread, StackTraceElement[]> entry : map.entrySet()) {
                threads.put(new BasicThreadInformation(entry.getKey()), entry.getValue());
            }
            return threads;
        }
    }

    /**
     * Factory to create extended thread information.
     */
    private static class ExtendedThreadInfoFactory implements ThreadInfoFactory {
        public Map<ThreadInformation, StackTraceElement[]> createThreadInfo() {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] array = bean.dumpAllThreads(true, true);

            Map<ThreadInformation, StackTraceElement[]>  threads =
                new HashMap<ThreadInformation, StackTraceElement[]>(array.length);
            for (ThreadInfo info : array) {
                threads.put(new ExtendedThreadInformation(info), info.getStackTrace());
            }
            return threads;
        }
    }
}
