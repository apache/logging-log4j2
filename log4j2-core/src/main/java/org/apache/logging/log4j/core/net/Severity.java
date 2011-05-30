package org.apache.logging.log4j.core.net;

import org.apache.logging.log4j.Level;

/**
 *  Severity values used by the Syslog system
 *
 *        Numerical   Severity<br>
 *          Code<br>
 *
 *            0       Emergency: system is unusable<br>
 *            1       Alert: action must be taken immediately<br>
 *            2       Critical: critical conditions<br>
 *            3       Error: error conditions<br>
 *            4       Warning: warning conditions<br>
 *            5       Notice: normal but significant condition<br>
 *            6       Informational: informational messages<br>
 *            7       Debug: debug-level messages
 */
public enum Severity {
    EMERG(0),
    ALERT(1),
    CRITICAL(2),
    ERROR(3),
    WARNING(4),
    NOTICE(5),
    INFO(6),
    DEBUG(7);

    private final int code;

    private Severity(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public boolean equals(String name) {
        return this.name().equalsIgnoreCase(name);
    }

    public static Severity getSeverity(Level level) {
        switch (level) {
            case ALL:
                return DEBUG;
            case TRACE:
                return DEBUG;
            case DEBUG:
                return DEBUG;
            case INFO:
                return INFO;
            case WARN:
                return WARNING;
            case ERROR:
                return ERROR;
            case FATAL:
                return ALERT;
            case OFF:
                return EMERG;
        }
        return DEBUG;
    }
}
