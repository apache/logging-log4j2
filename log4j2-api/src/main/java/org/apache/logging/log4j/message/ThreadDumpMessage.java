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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Captures information about all running Threads.
 */
public class ThreadDumpMessage implements Message {

    private final String title;

    private final Map<ThreadInfo, StackTraceElement[]> threads;

    private String formattedMessage = null;

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
        Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
        threads = new HashMap<ThreadInfo, StackTraceElement[]>(map.size());
        for (Map.Entry<Thread, StackTraceElement[]> entry : map.entrySet()) {
            threads.put(new ThreadInfo(entry.getKey()), entry.getValue());
        }
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
        for (Map.Entry<ThreadInfo, StackTraceElement[]> entry : threads.entrySet()) {
            printThreadInfo(sb, entry.getKey());
            printStack(sb, entry.getValue());
            sb.append("\n");
        }
        return sb.toString();
    }

    private void printThreadInfo(StringBuilder sb, ThreadInfo info) {
        sb.append("\"").append(info.name).append("\" ");
        if (info.isDaemon) {
            sb.append("daemon ");
        }
        sb.append("prio=").append(info.priority).append(" tid=").append(info.id).append(" ");
        if (info.threadGroupName != null) {
            sb.append("group=\"").append(info.threadGroupName).append("\"");
        }
        sb.append("\n");
        sb.append("\tThread state: ").append(info.state.name()).append("\n");
    }

    private void printStack(StringBuilder sb, StackTraceElement[] trace) {
        for (StackTraceElement element : trace) {
            sb.append("\tat ").append(element).append("\n");
        }
    }

    /**
     * Returns the title.
     * @return the title.
     */
    public String getMessageFormat() {
        return title == null ? "" : title;
    }

    /**
     * Returns an array with a single element, a Map containing the ThreadInformation as the key
     * and the StackTraceElement array as the value;
     * @return the "parameters" to this Message.
     */
    public Object[] getParameters() {
        return new Object[] {threads};
    }

    /**
     * Information describing each thread.
     */
    public class ThreadInfo implements Serializable {

        private static final long serialVersionUID = 6899550135289181860L;
        private final long id;
        private final String name;
        private final String longName;
        private final Thread.State state;
        private final int priority;
        private final boolean isAlive;
        private final boolean isDaemon;
        private final String threadGroupName;

        public ThreadInfo(Thread thread) {
            this.id = thread.getId();
            this.name = thread.getName();
            this.longName = thread.toString();
            this.state = thread.getState();
            this.priority = thread.getPriority();
            this.isAlive = thread.isAlive();
            this.isDaemon = thread.isDaemon();
            ThreadGroup group = thread.getThreadGroup();
            threadGroupName = group == null ? null : group.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ThreadInfo that = (ThreadInfo) o;

            if (id != that.id) {
                return false;
            }
            if (name != null ? !name.equals(that.name) : that.name != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }
}
