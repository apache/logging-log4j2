package org.apache.logging.log4j.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ConstantsTest {

    @Test
    public void testJdkVersionDetection() {
        assertThat(Constants.getMajorVersion("1.1.2")).isEqualTo(1);
        assertThat(Constants.getMajorVersion("1.8.2")).isEqualTo(8);
        assertThat(Constants.getMajorVersion("9.1.1")).isEqualTo(9);
        assertThat(Constants.getMajorVersion("11.1.1")).isEqualTo(11);
    }
}
