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

import java.text.DecimalFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

/**
 *
 * You have to watch the console to see this work, or not.
 *
 * See:
 * <ul>
 * <li>https://issues.apache.org/jira/browse/LOG4J2-682</li>
 * <li>https://mail-archives.apache.org/mod_mbox/logging-log4j-user/201406.mbox/%3CCAKnbemWoAXryn7UH=qMmwr=ad24La1+qv+
 * cyO9OXxCCCJAGV_g@mail.gmail.com%3E</li>
 * </ul>
 */
public class ProgressConsoleTest {

    private static final Logger LOG = LogManager.getLogger(ProgressConsoleTest.class);

    public static void main(final String[] args) {
        // src/test/resources/log4j2-console-progress.xml
        // target/test-classes/log4j2-progress-console.xml
        try (final LoggerContext ctx = Configurator.initialize(ProgressConsoleTest.class.getName(),
                "target/test-classes/log4j2-progress-console.xml")) {
            for (double i = 0; i <= 1; i = i + 0.05) {
                updateProgress(i);
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private static void updateProgress(final double progressPercentage) {
        final int width = 50; // progress bar width in chars

        String s = "[";
        int i = 0;
        for (; i <= (int) (progressPercentage * width); i++) {
            s += ".";
        }
        for (; i < width; i++) {
            s += " ";
        }
        s += "](" + (new DecimalFormat("#0.00")).format(progressPercentage * 100) + "%)";
        LOG.info(s);
    }
}
