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

import org.apache.oro.text.perl.Perl5Util;

public class JunitTestRunnerFilter implements Filter {
    Perl5Util util = new Perl5Util();

    /**
     * Filter out stack trace lines coming from the various JUnit TestRunners.
     */
    @Override
    public String filter(final String in) {
        if (in == null) {
            return null;
        }

        if (util.match("/at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner/", in)) {
            return null;
        } else if (util.match("/at org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner/", in)) {
            return null;
        } else if (util.match("/at com.intellij/", in)) {
            return null;
        } else if (in.indexOf("at junit.") >= 0 && in.indexOf("ui.TestRunner") >= 0) {
            return null;
        } else if (in.indexOf("org.apache.maven") >= 0) {
            return null;
        } else if (in.indexOf("junit.internal") >= 0) {
            return null;
        } else if (in.indexOf("JUnit4TestAdapter") >= 0) {
            return null;
        } else if (util.match("/\\sat /", in)) {
            return "\t" + in.trim();
        } else {
            return in;
        }
    }
}
