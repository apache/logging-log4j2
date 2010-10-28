/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.Filters;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes the byte output stream. The stream will already have been encoded.
 */
public abstract class OutputStreamAppender extends AppenderBase {

    /**
     * Immediate flush means that the underlying writer or output stream
     * will be flushed at the end of each append operation. Immediate
     * flush is slower but ensures that each append request is actually
     * written. If <code>immediateFlush</code> is set to
     * <code>false</code>, then there is a good chance that the last few
     * logs events are not actually written to persistent media if and
     * when the application crashes.
     * <p/>
     * <p>The <code>immediateFlush</code> variable is set to
     * <code>true</code> by default.
     */
    protected boolean immediateFlush = true;

    /**
     * This is the OutputStream where we will write to.
     */
    protected InternalOutputStream os;

    /**
     * Instantiate a WriterAppender and set the output destination to a
     * new {@link java.io.OutputStreamWriter} initialized with <code>os</code>
     * as its {@link java.io.OutputStream}.
     * @param name The name of the Appender.
     * @param layout The layout to format the message.
     * @param os The OutputStream.
     */
    public OutputStreamAppender(String name, Layout layout, Filters filters, OutputStream os) {
        super(name, filters, layout);
        this.setOutputStream(os);
    }

    /**
     * If the <b>ImmediateFlush</b> option is set to
     * <code>true</code>, the appender will flush at the end of each
     * write. This is the default behavior. If the option is set to
     * <code>false</code>, then the underlying stream can defer writing
     * to physical medium to a later time.
     * <p/>
     * <p>Avoiding the flush operation at the end of each append results in
     * a performance gain of 10 to 20 percent. However, there is safety
     * tradeoff involved in skipping flushing. Indeed, when flushing is
     * skipped, then it is likely that the last few log events will not
     * be recorded on disk when the application exits. This is a high
     * price to pay even for a 20% performance gain.
     */
    public void setImmediateFlush(boolean value) {
        immediateFlush = value;
    }

    /**
     * Returns value of the <b>ImmediateFlush</b> option.
     */
    public boolean getImmediateFlush() {
        return immediateFlush;
    }

    @Override
    public void start() {
        if (getLayout() == null) {
            logger.error("No layout set for the appender named [" + getName() + "].");
        }
        if (this.os == null) {
            logger.error("No OutputStream set for the appender named [" + getName() + "].");
        }
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        os.close();
    }


    /**
     * <p/>
     * <p>If the output stream exists and is writable then write a log
     * statement to the output stream. Otherwise, write a single warning
     * message to <code>System.err</code>.
     * <p/>
     * <p>The format of the output will depend on this appender's
     * layout.
     */
    public void append(LogEvent event) {

        if (!isStarted()) {
            return;
        }
        subAppend(event);
    }



    /**
     * Close this appender instance. The underlying stream or writer is
     * also closed.
     * <p/>
     * <p>Closed appenders cannot be reused.
     *
     * @see #setOutputStream
     */
    public void close() {
        reset();
    }

    /**
     * <p>Sets the OutputStream where the log output will go. The
     * specified OutputStream must be opened by the user and be
     * writable.
     * <p/>
     * <p>The <code>java.io.OutputStream</code> will be closed when the
     * appender instance is closed.
     * <p/>
     * <p/>
     * <p><b>WARNING:</b> Logging to an unopened Writer will fail.
     *
     * @param os An already opened OutputStream.
     */
    public synchronized void setOutputStream(OutputStream os) {
        InternalOutputStream prev = this.os;
        this.os = new InternalOutputStream(os);
        if (prev != null) {
            prev.close();
        }
    }


    /**
     * Actual writing occurs here.
     * <p/>
     * <p>Most subclasses of <code>OutputStreamAppender</code> will need to
     * override this method.
     * @param event The LogEvent.
     */
    protected void subAppend(LogEvent event) {
        this.os.write(getLayout().format(event));

        if (this.immediateFlush) {
            this.os.flush();
        }
    }


    /**
     * The WriterAppender requires a layout. Hence, this method returns
     * <code>true</code>.
     */
    public boolean requiresLayout() {
        return true;
    }

    /**
     * Clear internal references to the writer and other variables.
     * <p/>
     * Subclasses can override this method for an alternate closing
     * behavior.
     */
    protected synchronized void reset() {
        InternalOutputStream os = this.os;
        if (os != null) {
            this.os = null;
            os.close();
        }
    }


    /**
     * Write a footer as produced by the embedded layout's {@link
     * org.apache.logging.log4j.core.Layout#getFooter} method.
     */
    protected void writeFooter(OutputStream os) {
        Layout layout = getLayout();
        if (layout != null) {
            byte[] b = layout.getFooter();
            if (b != null && os != null) {
                try {
                    os.write(b);
                    os.flush();
                } catch (IOException ioe) {
                    logger.error("Failed to write footer for appender " + getName(), ioe);
                }
            }
        }
    }

    /**
     * Write a header as produced by the embedded layout's {@link
     * org.apache.logging.log4j.core.Layout#getHeader} method.
     */
    protected void writeHeader(OutputStream os) {
        Layout layout = getLayout();
        if (layout != null) {
            byte[] b = layout.getHeader();
            if (b != null && os != null) {
                try {
                    os.write(b);
                } catch (IOException ioe) {
                    logger.error("Failed to write footer for appender " + getName(), ioe);
                }
            }
        }
    }

    private class InternalOutputStream extends OutputStream {

        private final OutputStream os;

        public InternalOutputStream(OutputStream os) {
            writeHeader(os);
            this.os = os;
        }

        @Override
        public void close() {
            writeFooter(os);
            try {
                if (os != System.out && os != System.err) {
                    os.close();
                }
            } catch (IOException ioe) {
                logger.error("Error closing writer for " + getName(), ioe);
            }

        }

        @Override
        public void flush() {
            try {
                os.flush();
            } catch (IOException ioe) {
                getHandler().error("Error flushing appender " + getName(), ioe);
            }
        }

        @Override
        public void write(byte[] bytes, int i, int i1) {
            try {
                os.write(bytes, i, i1);
            } catch (IOException ioe) {
                getHandler().error("Error writing to appender " + getName(), ioe);
            }
        }

        @Override
        public void write(byte[] bytes) {
            write(bytes, 0, bytes.length);
        }

        @Override
        public void write(int i) {
            try {
                os.write(i);
            }  catch (IOException ioe) {
                getHandler().error("Error writing to appender " + getName(), ioe);
            }
        }
    }
}
