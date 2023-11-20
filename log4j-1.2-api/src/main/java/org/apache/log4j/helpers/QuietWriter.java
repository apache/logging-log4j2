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
package org.apache.log4j.helpers;

import java.io.FilterWriter;
import java.io.Writer;
import org.apache.log4j.spi.ErrorCode;
import org.apache.log4j.spi.ErrorHandler;

/**
 * QuietWriter does not throw exceptions when things go
 * wrong. Instead, it delegates error handling to its {@link ErrorHandler}.
 */
public class QuietWriter extends FilterWriter {

    protected ErrorHandler errorHandler;

    public QuietWriter(final Writer writer, final ErrorHandler errorHandler) {
        super(writer);
        setErrorHandler(errorHandler);
    }

    @Override
    public void write(final String string) {
        if (string != null) {
            try {
                out.write(string);
            } catch (Exception e) {
                errorHandler.error("Failed to write [" + string + "].", e, ErrorCode.WRITE_FAILURE);
            }
        }
    }

    @Override
    public void flush() {
        try {
            out.flush();
        } catch (Exception e) {
            errorHandler.error("Failed to flush writer,", e, ErrorCode.FLUSH_FAILURE);
        }
    }

    public void setErrorHandler(final ErrorHandler eh) {
        if (eh == null) {
            // This is a programming error on the part of the enclosing appender.
            throw new IllegalArgumentException("Attempted to set null ErrorHandler.");
        }
        this.errorHandler = eh;
    }
}
