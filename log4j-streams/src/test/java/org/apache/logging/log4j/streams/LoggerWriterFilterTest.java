package org.apache.logging.log4j.streams;

import java.io.StringWriter;
import java.io.Writer;

public class LoggerWriterFilterTest extends AbstractLoggerWriterTest {

    @Override
    protected StringWriter createWriter() {
        return new StringWriter();
    }

    @Override
    protected Writer createWriterWrapper() {
        return new LoggerWriterFilter(this.wrapped, getExtendedLogger(), LEVEL);
    }

}
