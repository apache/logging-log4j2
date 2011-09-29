package org.apache.logging.log4j.message;

import java.io.Serializable;

/**
 *
 */
interface ThreadInformation {
    void printThreadInfo(StringBuilder sb);

    void printStack(StringBuilder sb, StackTraceElement[] trace);

}
