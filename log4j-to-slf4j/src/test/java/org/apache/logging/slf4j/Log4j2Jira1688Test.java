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
package org.apache.logging.slf4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests LOG4J2-1688 Multiple loggings of arguments are setting these arguments to null.
 */
class Log4j2Jira1688Test {

    @Test
    void testLog4j2() {

        // Argument-array creation
        final int limit = 37;
        final Object[] args = createArray(limit);
        final Object[] originalArgs = Arrays.copyOf(args, args.length);

        System.out.println("args " + Arrays.toString(args));

        // Logger definition
        final String someFormat = "test {}";
        final Logger logger = LoggerFactory.getLogger(this.getClass());

        // First logging of args
        logger.error(someFormat, args); // Only the first element (args[0]) of args will be logged - why?
        // GG: because the pattern {} picks up the 1 st argument, not the whole array
        assertThat(args).containsExactly(originalArgs);

        // Bug: The second logging of args sets all elements of args to null
        logger.error(someFormat, args);
        // GG: All is well args is still intact
        System.out.println("args " + Arrays.toString(args));
        assertThat(args).containsExactly(originalArgs);
    }

    /**
     * @param size
     * @return
     */
    private static Object[] createArray(final int size) {
        final Object[] args = new Object[size];
        for (int i = 0; i < args.length; i++) {
            args[i] = i;
        }
        return args;
    }
}
