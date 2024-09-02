/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender.internal;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Plugin
@Namespace(ConsoleAppender.ConsoleStreamSupplier.NAMESPACE)
@Ordered(Ordered.LAST)
@NullMarked
public class DefaultConsoleStreamSupplier implements ConsoleAppender.ConsoleStreamSupplier {

    private static final Logger LOGGER = StatusLogger.getLogger();

    @Override
    @Nullable
    public OutputStream getOutputStream(
            boolean follow, boolean direct, ConsoleAppender.Target target, PropertyEnvironment properties) {
        if (follow && direct) {
            LOGGER.error("Cannot use both `follow` and `direct` on ConsoleAppender.");
            return null;
        }
        return switch (target) {
            case SYSTEM_ERR -> getErr(follow, direct);
            case SYSTEM_OUT -> getOut(follow, direct);
        };
    }

    private OutputStream getErr(final boolean follow, final boolean direct) {
        if (direct) {
            return new CloseShieldOutputStream(new FileOutputStream(FileDescriptor.err));
        }
        if (follow) {
            return new SystemErrStream();
        }
        return new CloseShieldOutputStream(System.err);
    }

    private OutputStream getOut(final boolean follow, final boolean direct) {
        if (direct) {
            return new CloseShieldOutputStream(new FileOutputStream(FileDescriptor.out));
        }
        if (follow) {
            return new SystemOutStream();
        }
        return new CloseShieldOutputStream(System.out);
    }

    /**
     * An implementation of OutputStream that redirects to the current System.err.
     */
    private static class SystemErrStream extends OutputStream {
        public SystemErrStream() {}

        @Override
        public void close() {
            // do not close sys err!
        }

        @Override
        public void flush() {
            System.err.flush();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.err.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) {
            System.err.write(b, off, len);
        }

        @Override
        public void write(final int b) {
            System.err.write(b);
        }
    }

    /**
     * An implementation of OutputStream that redirects to the current System.out.
     */
    private static class SystemOutStream extends OutputStream {
        public SystemOutStream() {}

        @Override
        public void close() {
            // do not close sys out!
        }

        @Override
        public void flush() {
            System.out.flush();
        }

        @Override
        public void write(final byte[] b) throws IOException {
            System.out.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) {
            System.out.write(b, off, len);
        }

        @Override
        public void write(final int b) {
            System.out.write(b);
        }
    }
}
