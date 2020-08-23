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
package org.apache.logging.log4j.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LegacyPropertiesCompatibilityTest {

    public static Object[][] data() {
        return new Object[][]{
            {"log4j2.configurationFile", "log4j.configurationFile"},
            {"log4j2.mergeStrategy", "log4j.mergeStrategy"},
            {"log4j2.contextSelector", "Log4jContextSelector"},
            {"log4j2.logEventFactory", "Log4jLogEventFactory"},
            {"log4j2.configurationFactory", "log4j.configurationFactory"},
            {"log4j2.shutdownCallbackRegistry", "log4j.shutdownCallbackRegistry"},
            {"log4j2.clock", "log4j.Clock"},
            {"log4j2.level", "org.apache.logging.log4j.level"},
            {"log4j2.disableThreadContext", "disableThreadContext"},
            {"log4j2.disableThreadContextStack", "disableThreadContextStack"},
            {"log4j2.disableThreadContextMap", "disableThreadContextMap"},
            {"log4j2.isThreadContextMapInheritable", "isThreadContextMapInheritable"},
            {"log4j2.contextDataInjector", "log4j2.ContextDataInjector"},
            {"log4j2.garbagefreeThreadContextMap", "log4j2.garbagefree.threadContextMap"},
            {"log4j2.disableJmx", "log4j2.disable.jmx"},
            {"log4j2.jmxNotifyAsync", "log4j2.jmx.notify.async"},
            {"log4j2.skipJansi", "log4j.skipJansi"},
            {"log4j2.ignoreTCL", "log4j.ignoreTCL"},
            {"log4j2.ignoreTCL", "LOG4J_IGNORE_TCL"}, // just a sanity check for fun camel case names
            {"log4j2.uuidSequence", "org.apache.logging.log4j.uuidSequence"},
            {"log4j2.assignedSequences", "org.apache.logging.log4j.assignedSequences"},
            {"log4j2.simplelogShowContextMap", "org.apache.logging.log4j.simplelog.showContextMap"},
            {"log4j2.simplelogShowlogname", "org.apache.logging.log4j.simplelog.showlogname"},
            {"log4j2.simplelogShowShortLogname", "org.apache.logging.log4j.simplelog.showShortLogname"},
            {"log4j2.simplelogShowdatetime", "org.apache.logging.log4j.simplelog.showdatetime"},
            {"log4j2.simplelogDateTimeFormat", "org.apache.logging.log4j.simplelog.dateTimeFormat"},
            {"log4j2.simplelogLogFile", "org.apache.logging.log4j.simplelog.logFile"},
            {"log4j2.simplelogLevel", "org.apache.logging.log4j.simplelog.level"},
            {"log4j2.simplelog.com.foo.bar.Thing.level", "org.apache.logging.log4j.simplelog.com.foo.bar.Thing.level"},
            {"log4j2.simplelogComFooBarThingLevel", "org.apache.logging.log4j.simplelog.com.foo.bar.Thing.level"},
            {"log4j2.simplelogStatusLoggerLevel", "org.apache.logging.log4j.simplelog.StatusLogger.level"},
            {"log4j2.defaultStatusLevel", "Log4jDefaultStatusLevel"},
            {"log4j2.statusLoggerLevel", "log4j2.StatusLogger.level"},
            {"log4j2.statusEntries", "log4j2.status.entries"},
            {"log4j2.asyncLoggerExceptionHandler", "AsyncLogger.ExceptionHandler"},
            {"log4j2.asyncLoggerRingBufferSize", "AsyncLogger.RingBufferSize"},
            {"log4j2.asyncLoggerWaitStrategy", "AsyncLogger.WaitStrategy"},
            {"log4j2.asyncLoggerThreadNameStrategy", "AsyncLogger.ThreadNameStrategy"},
            {"log4j2.asyncLoggerConfigExceptionHandler", "AsyncLoggerConfig.ExceptionHandler"},
            {"log4j2.asyncLoggerConfigRingBufferSize", "AsyncLoggerConfig.RingBufferSize"},
            {"log4j2.asyncLoggerConfigWaitStrategy", "AsyncLoggerConfig.WaitStrategy"},
            {"log4j2.julLoggerAdapter", "log4j.jul.LoggerAdapter"},
            {"log4j2.formatMsgAsync", "log4j.format.msg.async"},
            {"log4j2.asyncQueueFullPolicy", "log4j2.AsyncQueueFullPolicy"},
            {"log4j2.discardThreshold", "log4j2.DiscardThreshold"},
            {"log4j2.isWebapp", "log4j2.is.webapp"},
            {"log4j2.enableThreadlocals", "log4j2.enable.threadlocals"},
            {"log4j2.enableDirectEncoders", "log4j2.enable.direct.encoders"},
            {"log4j2.initialReusableMsgSize", "log4j.initialReusableMsgSize"},
            {"log4j2.maxReusableMsgSize", "log4j.maxReusableMsgSize"},
            {"log4j2.layoutStringBuilderMaxSize", "log4j.layoutStringBuilder.maxSize"},
            {"log4j2.unboxRingbufferSize", "log4j.unbox.ringbuffer.size"},
            {"log4j2.loggerContextStacktraceOnStart", "log4j.LoggerContext.stacktrace.on.start"},
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    public void compareNewWithOldName(final String newName, final String oldName) {
        final List<CharSequence> newTokens = PropertySource.Util.tokenize(newName);
        final List<CharSequence> oldTokens = PropertySource.Util.tokenize(oldName);
        assertEquals(oldTokens, newTokens);
    }
}