package org.apache.logging.log4j.smtp.appender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SmtpManager}.
 */
class SmtpManagerTest {

    @Test
    void testCreateManagerName() {
        String managerName = SmtpManager.createManagerName("to", "cc", null, "from", null, "LOG4J2-3107",
                "proto", "smtp.log4j.com", 4711, "username", false, "filter");
        assertEquals("SMTP:to:cc::from::LOG4J2-3107:proto:smtp.log4j.com:4711:username::filter", managerName);
    }

}
