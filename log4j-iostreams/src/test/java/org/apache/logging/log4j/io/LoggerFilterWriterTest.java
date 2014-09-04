package org.apache.logging.log4j.io;

import java.io.StringWriter;
import java.io.Writer;

public class LoggerFilterWriterTest extends AbstractLoggerWriterTest {

    @Override
    protected StringWriter createWriter() {
        return new StringWriter();
    }

    @Override
    protected Writer createWriterWrapper() {
        return LoggerStreams.forLogger(getExtendedLogger())
            .filter(this.wrapped)
            .setLevel(LEVEL)
            .buildWriter();
    }

}
