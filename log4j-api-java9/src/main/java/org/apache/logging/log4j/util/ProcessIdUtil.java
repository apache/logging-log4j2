package org.apache.logging.log4j.util;

public class ProcessIdUtil {

    public static final String DEFAULT_PROCESSID = "-";

    public static String getProcessId() {
        try {
            return Long.toString(ProcessHandle.current().pid());
        } catch(Exception ex) {
            return DEFAULT_PROCESSID;
        }
    }
}
