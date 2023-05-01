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

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LegacyPropertiesCompatibilityTest {

    public static Object[][] data() {
        return new Object[][]{
            {"log4j2.configurationFile", "Configuration.file"},
            {"log4j2.mergeStrategy", "Configuration.mergeStrategy"},
            {"log4j2.contextSelector", "LoggerContext.selector"},
            {"log4j2.logEventFactory", "Logger.logEventFactory"},
            {"log4j2.configurationFactory", "Configuration.factory"},
            {"log4j2.shutdownCallbackRegistry", "LoggerContext.shutdownCallbackRegistry"},
            {"log4j2.clock", "Configuration.clock"},
            {"log4j2.level", "Configuration.level"},
            {"log4j2.isThreadContextMapInheritable", "ThreadContext.inheritable"},
            {"log4j2.contextDataInjector", "ThreadContext.contextDataInjector"},
            {"log4j2.garbagefreeThreadContextMap", "ThreadContext.garbageFreeMap"},
            {"log4j2.jmxNotifyAsync", "JMX.notifyAsync"},
            {"log4j2.ignoreTCL", "Loader.ignoreTCL"},
            {"log4j2.uuidSequence", "UUID.sequence"},
            {"log4j2.assignedSequences", "UUID.assignedSequences"},
            {"log4j2.simplelogShowContextMap", "SimpleLogger.showContextMap"},
            {"log4j2.simplelogShowlogname", "SimpleLogger.showLogName"},
            {"log4j2.simplelogShowShortLogname", "SimpleLogger.showShortLogName"},
            {"log4j2.simplelogShowdatetime", "SimpleLogger.showDateTime"},
            {"log4j2.simplelogDateTimeFormat", "SimpleLogger.dateTimeFormat"},
            {"log4j2.simplelogLogFile", "SimpleLogger.logFile"},
            {"log4j2.simplelogLevel", "SimpleLogger.level"},
            {"log4j2.simplelogStatusLoggerLevel", "SimpleLogger.statusLoggerLevel"},
            {"log4j2.defaultStatusLevel", "StatusLogger.defaultLevel"},
            {"log4j2.statusLoggerLevel", "StatusLogger.level"},
            {"log4j2.statusEntries", "StatusLogger.entries"},
            {"log4j2.asyncLoggerExceptionHandler", "AsyncLogger.exceptionHandler"},
            {"log4j2.asyncLoggerRingBufferSize", "AsyncLogger.ringBufferSize"},
            {"log4j2.asyncLoggerWaitStrategy", "AsyncLogger.waitStrategy"},
            {"log4j2.asyncLoggerThreadNameStrategy", "AsyncLogger.threadNameStrategy"},
            {"log4j2.asyncLoggerConfigExceptionHandler", "AsyncLoggerConfig.exceptionHandler"},
            {"log4j2.asyncLoggerConfigRingBufferSize", "AsyncLoggerConfig.ringBufferSize"},
            {"log4j2.asyncLoggerConfigWaitStrategy", "AsyncLoggerConfig.waitStrategy"},
            {"log4j2.julLoggerAdapter", "JUL.loggerAdapter"},
            {"log4j2.formatMsgAsync", "AsyncLogger.formatMsg"},
            {"log4j2.asyncQueueFullPolicy", "AsyncLogger.queueFullPolicy"},
            {"log4j2.discardThreshold", "AsyncLogger.discardThreshold"},
            {"log4j2.isWebapp", "Web.isWebApp"},
            {"log4j2.enableThreadlocals", "ThreadLocals.enable"},
            {"log4j2.enableDirectEncoders", "GC.enableDirectEncoders"},
            {"log4j2.initialReusableMsgSize", "GC.initialReusableMsgSize"},
            {"log4j2.maxReusableMsgSize", "GC.maxReusableMsgSize"},
            {"log4j2.layoutStringBuilderMaxSize", "GC.layoutStringBuilderMaxSize"},
            {"log4j2.unboxRingbufferSize", "Unbox.ringBufferSize"},
            {"log4j2.loggerContextStacktraceOnStart", "LoggerContext.stackTraceOnStart"}
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    public void compareNewWithOldName(final String oldName, final String newName) {
        final List<CharSequence> newTokens = PropertySource.Util.tokenize(newName);
        final List<CharSequence> oldTokens = PropertySource.Util.tokenize(oldName);
        assertEquals(newTokens, oldTokens);
    }
}
