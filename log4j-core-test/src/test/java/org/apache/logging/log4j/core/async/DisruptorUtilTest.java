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
package org.apache.logging.log4j.core.async;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmax.disruptor.ExceptionHandler;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

@UsingStatusListener
class DisruptorUtilTest {

    private static final String LINKAGE_ERROR_MESSAGE =
            "Log4j2 detected a linkage error while initializing LMAX Disruptor";

    @Test
    @SetTestProperty(
            key = DisruptorUtil.LOGGER_EXCEPTION_HANDLER_PROPERTY,
            value = "org.apache.logging.log4j.core.async.DisruptorUtilTest$BrokenAsyncLoggerExceptionHandler")
    void getAsyncLoggerExceptionHandler_logsLinkageErrors(final ListStatusListener statusListener) {
        final ExceptionHandler<RingBufferLogEvent> exceptionHandler = DisruptorUtil.getAsyncLoggerExceptionHandler();

        assertThat(exceptionHandler).isInstanceOf(AsyncLoggerDefaultExceptionHandler.class);

        final List<StatusData> statusData =
                statusListener.findStatusData(Level.ERROR).collect(Collectors.toList());
        assertThat(statusData).anySatisfy(data -> {
            assertThat(data.getMessage().getFormattedMessage()).contains(LINKAGE_ERROR_MESSAGE);
            assertThat(data.getThrowable()).isInstanceOf(NoClassDefFoundError.class);
            assertThat(data.getThrowable().getMessage()).contains("broken AsyncLogger handler");
        });
    }

    @Test
    @SetTestProperty(
            key = DisruptorUtil.LOGGER_CONFIG_EXCEPTION_HANDLER_PROPERTY,
            value = "org.apache.logging.log4j.core.async.DisruptorUtilTest$BrokenAsyncLoggerConfigExceptionHandler")
    void getAsyncLoggerConfigExceptionHandler_logsLinkageErrors(final ListStatusListener statusListener) {
        final ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper> exceptionHandler =
                DisruptorUtil.getAsyncLoggerConfigExceptionHandler();

        assertThat(exceptionHandler).isInstanceOf(AsyncLoggerConfigDefaultExceptionHandler.class);

        final List<StatusData> statusData =
                statusListener.findStatusData(Level.ERROR).collect(Collectors.toList());
        assertThat(statusData).anySatisfy(data -> {
            assertThat(data.getMessage().getFormattedMessage()).contains(LINKAGE_ERROR_MESSAGE);
            assertThat(data.getThrowable()).isInstanceOf(NoClassDefFoundError.class);
            assertThat(data.getThrowable().getMessage()).contains("broken AsyncLoggerConfig handler");
        });
    }

    public static class BrokenAsyncLoggerExceptionHandler implements ExceptionHandler<RingBufferLogEvent> {

        static {
            if (System.nanoTime() >= 0) {
                throw new NoClassDefFoundError("broken AsyncLogger handler");
            }
        }

        @Override
        public void handleEventException(final Throwable ex, final long sequence, final RingBufferLogEvent event) {}

        @Override
        public void handleOnStartException(final Throwable ex) {}

        @Override
        public void handleOnShutdownException(final Throwable ex) {}
    }

    public static class BrokenAsyncLoggerConfigExceptionHandler
            implements ExceptionHandler<AsyncLoggerConfigDisruptor.Log4jEventWrapper> {

        static {
            if (System.nanoTime() >= 0) {
                throw new NoClassDefFoundError("broken AsyncLoggerConfig handler");
            }
        }

        @Override
        public void handleEventException(
                final Throwable ex, final long sequence, final AsyncLoggerConfigDisruptor.Log4jEventWrapper event) {}

        @Override
        public void handleOnStartException(final Throwable ex) {}

        @Override
        public void handleOnShutdownException(final Throwable ex) {}
    }
}
