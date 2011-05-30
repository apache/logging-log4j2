package org.apache.logging.log4j.core.net;

/**
 *  The facility codes used by the Syslog system.
 *
 *        Numerical          Facility<br>
 *           Code<br>
 *
 *             0             kernel messages<br>
 *             1             user-level messages<br>
 *             2             mail system<br>
 *             3             system daemons<br>
 *             4             security/authorization messages<br>
 *             5             messages generated internally by syslogd<br>
 *             6             line printer subsystem<br>
 *             7             network news subsystem<br>
 *             8             UUCP subsystem<br>
 *             9             clock daemon<br>
 *            10             security/authorization messages<br>
 *            11             FTP daemon<br>
 *            12             NTP subsystem<br>
 *            13             log audit<br>
 *            14             log alert<br>
 *            15             clock daemon (note 2)<br>
 *            16             local use 0  (local0)<br>
 *            17             local use 1  (local1)<br>
 *            18             local use 2  (local2)<br>
 *            19             local use 3  (local3)<br>
 *            20             local use 4  (local4)<br>
 *            21             local use 5  (local5)<br>
 *            22             local use 6  (local6)<br>
 *            23             local use 7  (local7)<br>
 */
public enum Facility {
    KERN(0),
    USER(1),
    MAIL(2),
    DAEMON(3),
    AUTH(4),
    SYSLOG(5),
    LPR(6),
    NEWS(7),
    UUCP(8),
    CRON(9),
    AUTHPRIV(10),
    FTP(11),
    NTP(12),
    LOG_AUDIT(13),
    LOG_ALERT(14),
    CLOCK(15),
    LOCAL0(16),
    LOCAL1(17),
    LOCAL2(18),
    LOCAL3(19),
    LOCAL4(20),
    LOCAL5(21),
    LOCAL6(22),
    LOCAL7(23);

    private final int code;

    private Facility(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public boolean equals(String name) {
        return this.name().equalsIgnoreCase(name);
    }

}
