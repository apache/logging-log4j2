package org.apache.logging.log4j.core.util;

import java.io.IOException;
import java.io.Writer;

public class CloseShieldWriter extends Writer {

    private final Writer delegate;

    public CloseShieldWriter(final Writer delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() throws IOException {
        // do not close delegate
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();

    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        delegate.write(cbuf, off, len);
    }

}
