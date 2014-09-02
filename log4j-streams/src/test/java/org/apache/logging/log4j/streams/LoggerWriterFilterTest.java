package org.apache.logging.log4j.streams;

import java.io.StringWriter;
import java.io.Writer;

public class LoggerWriterFilterTest extends AbstractLoggerWriterTest {

    protected StringWriter createWriter() {
        return new StringWriter();
    }

    protected Writer createWriterWrapper() {
        return new LoggerWriterFilter(wrapped, getLogger(), LEVEL);
    }

}
