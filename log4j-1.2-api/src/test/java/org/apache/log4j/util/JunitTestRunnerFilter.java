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
package org.apache.log4j.util;

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public class JunitTestRunnerFilter implements Filter {

    /**
     * Filter out stack trace lines coming from the various JUnit TestRunners.
     */
    @Override
    public @Nullable String filter(final String in) {

        if (in.contains("at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner")) {
            return null;
        } else if (in.contains("at org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner")) {
            return null;
        } else if (in.contains("at com.intellij")) {
            return null;
        } else if (in.contains("at junit.") && in.contains("ui.TestRunner")) {
            return null;
        } else if (in.contains("org.apache.maven")) {
            return null;
        } else if (in.contains("junit.internal")) {
            return null;
        } else if (in.contains("JUnit4TestAdapter")) {
            return null;
        } else if (Pattern.compile("\\sat ").matcher(in).find()) {
            return "\t" + in.trim();
        } else {
            return in;
        }
    }
}
