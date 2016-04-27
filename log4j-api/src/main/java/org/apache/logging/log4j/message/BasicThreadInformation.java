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

import org.apache.logging.log4j.util.Chars;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * Generates information about the current Thread. Used internally by ThreadDumpMessage.
 */
class BasicThreadInformation implements ThreadInformation {
    private static final int HASH_SHIFT = 32;
    private static final int HASH_MULTIPLIER = 31;
    private final long id;
    private final String name;
    private final String longName;
    private final Thread.State state;
    private final int priority;
    private final boolean isAlive;
    private final boolean isDaemon;
    private final String threadGroupName;

    /**
     * The Constructor.
     * @param thread The Thread to capture.
     */
    BasicThreadInformation(final Thread thread) {
        this.id = thread.getId();
        this.name = thread.getName();
        this.longName = thread.toString();
        this.state = thread.getState();
        this.priority = thread.getPriority();
        this.isAlive = thread.isAlive();
        this.isDaemon = thread.isDaemon();
        final ThreadGroup group = thread.getThreadGroup();
        threadGroupName = group == null ? null : group.getName();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BasicThreadInformation that = (BasicThreadInformation) o;

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
        int result = (int) (id ^ (id >>> HASH_SHIFT));
        result = HASH_MULTIPLIER * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    /**
     * Print the thread information.
     * @param sb The StringBuilder.
     */
    @Override
    public void printThreadInfo(final StringBuilder sb) {
        StringBuilders.appendDqValue(sb, name).append(Chars.SPACE);
        if (isDaemon) {
            sb.append("daemon ");
        }
        sb.append("prio=").append(priority).append(" tid=").append(id).append(' ');
        if (threadGroupName != null) {
            StringBuilders.appendKeyDqValue(sb, "group", threadGroupName);
        }
        sb.append('\n');
        sb.append("\tThread state: ").append(state.name()).append('\n');
    }

    /**
     * Format the StackTraceElements.
     * @param sb The StringBuilder.
     * @param trace The stack trace element array to format.
     */
    @Override
    public void printStack(final StringBuilder sb, final StackTraceElement[] trace) {
        for (final StackTraceElement element : trace) {
            sb.append("\tat ").append(element).append('\n');
        }
    }
}
