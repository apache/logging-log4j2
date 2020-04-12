package org.apache.logging.log4j.core.appender.rolling.action;

import org.apache.logging.log4j.core.appender.rolling.FileSize;
import org.junit.Test;

import java.text.NumberFormat;
import java.util.Locale;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class FileSizeTest {

    @Test
    public void testParse() {
        assertThat(FileSize.parse("5k", 0), is(5L * 1024));
    }

    @Test
    public void testParseInEurope() {
        // Caveat: Breaks the ability for this test to run in parallel with other tests :(
        Locale previousDefault = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("de", "DE"));
            assertThat(FileSize.parse("1,000", 0), is(1000L));
        } finally {
            Locale.setDefault(previousDefault);
        }
    }
}
