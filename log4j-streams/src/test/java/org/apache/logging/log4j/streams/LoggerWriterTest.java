package org.apache.logging.log4j.streams;

import java.io.StringWriter;
import java.io.Writer;

public class LoggerWriterTest extends AbstractLoggerWriterTest {

    @Override
    protected StringWriter createWriter() {
        return null;
    }

    @Override
    protected Writer createWriterWrapper() {
        return new LoggerWriter(getLogger(), LEVEL);
    }

}
