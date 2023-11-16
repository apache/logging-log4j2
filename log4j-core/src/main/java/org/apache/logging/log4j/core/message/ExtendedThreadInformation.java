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
package org.apache.logging.log4j.core.message;

import static org.apache.logging.log4j.util.Chars.LF;

import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import org.apache.logging.log4j.message.ThreadInformation;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * Provides information on locks and monitors in the thread dump. This class requires Java 1.6 to compile and
 * run.
 */
class ExtendedThreadInformation implements ThreadInformation {

    private final ThreadInfo threadInfo;

    ExtendedThreadInformation(final ThreadInfo thread) {
        this.threadInfo = thread;
    }

    @Override
    public void printThreadInfo(final StringBuilder sb) {
        StringBuilders.appendDqValue(sb, threadInfo.getThreadName());
        sb.append(" Id=").append(threadInfo.getThreadId()).append(' ');
        formatState(sb, threadInfo);
        if (threadInfo.isSuspended()) {
            sb.append(" (suspended)");
        }
        if (threadInfo.isInNative()) {
            sb.append(" (in native)");
        }
        sb.append(LF);
    }

    @Override
    public void printStack(final StringBuilder sb, final StackTraceElement[] stack) {
        int i = 0;
        for (final StackTraceElement element : stack) {
            sb.append("\tat ").append(element.toString());
            sb.append(LF);
            if (i == 0 && threadInfo.getLockInfo() != null) {
                final Thread.State ts = threadInfo.getThreadState();
                switch (ts) {
                    case BLOCKED:
                        sb.append("\t-  blocked on ");
                        formatLock(sb, threadInfo.getLockInfo());
                        sb.append(LF);
                        break;
                    case WAITING:
                        sb.append("\t-  waiting on ");
                        formatLock(sb, threadInfo.getLockInfo());
                        sb.append(LF);
                        break;
                    case TIMED_WAITING:
                        sb.append("\t-  waiting on ");
                        formatLock(sb, threadInfo.getLockInfo());
                        sb.append(LF);
                        break;
                    default:
                }
            }

            for (final MonitorInfo mi : threadInfo.getLockedMonitors()) {
                if (mi.getLockedStackDepth() == i) {
                    sb.append("\t-  locked ");
                    formatLock(sb, mi);
                    sb.append(LF);
                }
            }
            ++i;
        }

        final LockInfo[] locks = threadInfo.getLockedSynchronizers();
        if (locks.length > 0) {
            sb.append("\n\tNumber of locked synchronizers = ")
                    .append(locks.length)
                    .append(LF);
            for (final LockInfo li : locks) {
                sb.append("\t- ");
                formatLock(sb, li);
                sb.append(LF);
            }
        }
    }

    private void formatLock(final StringBuilder sb, final LockInfo lock) {
        sb.append('<').append(lock.getIdentityHashCode()).append("> (a ");
        sb.append(lock.getClassName()).append(')');
    }

    private void formatState(final StringBuilder sb, final ThreadInfo info) {
        final Thread.State state = info.getThreadState();
        sb.append(state);
        switch (state) {
            case BLOCKED: {
                sb.append(" (on object monitor owned by \"");
                sb.append(info.getLockOwnerName())
                        .append("\" Id=")
                        .append(info.getLockOwnerId())
                        .append(')');
                break;
            }
            case WAITING: {
                final StackTraceElement element = info.getStackTrace()[0];
                final String className = element.getClassName();
                final String method = element.getMethodName();
                if (className.equals("java.lang.Object") && method.equals("wait")) {
                    sb.append(" (on object monitor");
                    if (info.getLockOwnerName() != null) {
                        sb.append(" owned by \"");
                        sb.append(info.getLockOwnerName()).append("\" Id=").append(info.getLockOwnerId());
                    }
                    sb.append(')');
                } else if (className.equals("java.lang.Thread") && method.equals("join")) {
                    sb.append(" (on completion of thread ")
                            .append(info.getLockOwnerId())
                            .append(')');
                } else {
                    sb.append(" (parking for lock");
                    if (info.getLockOwnerName() != null) {
                        sb.append(" owned by \"");
                        sb.append(info.getLockOwnerName()).append("\" Id=").append(info.getLockOwnerId());
                    }
                    sb.append(')');
                }
                break;
            }
            case TIMED_WAITING: {
                final StackTraceElement element = info.getStackTrace()[0];
                final String className = element.getClassName();
                final String method = element.getMethodName();
                if (className.equals("java.lang.Object") && method.equals("wait")) {
                    sb.append(" (on object monitor");
                    if (info.getLockOwnerName() != null) {
                        sb.append(" owned by \"");
                        sb.append(info.getLockOwnerName()).append("\" Id=").append(info.getLockOwnerId());
                    }
                    sb.append(')');
                } else if (className.equals("java.lang.Thread") && method.equals("sleep")) {
                    sb.append(" (sleeping)");
                } else if (className.equals("java.lang.Thread") && method.equals("join")) {
                    sb.append(" (on completion of thread ")
                            .append(info.getLockOwnerId())
                            .append(')');
                } else {
                    sb.append(" (parking for lock");
                    if (info.getLockOwnerName() != null) {
                        sb.append(" owned by \"");
                        sb.append(info.getLockOwnerName()).append("\" Id=").append(info.getLockOwnerId());
                    }
                    sb.append(')');
                }
                break;
            }
            default:
                break;
        }
    }
}
