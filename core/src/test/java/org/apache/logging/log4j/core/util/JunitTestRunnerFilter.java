/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

package org.apache.logging.log4j.core.util;

import org.apache.oro.text.perl.Perl5Util;


public class JunitTestRunnerFilter implements Filter {
  Perl5Util util = new Perl5Util();

  private static final String[] patterns = {
          "/at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner/",
          "/at org.apache.tools.ant/",
          "/at junit.textui.TestRunner/",
          "/at com.intellij.rt.execution.junit/",
          "/at java.lang.reflect.Method.invoke/",
          "/at org.apache.maven.surefire./"
  };

  /**
   * Filter out stack trace lines coming from the various JUnit TestRunners.
   */
  public String filter(final String in) {
    if (in == null) {
      return null;
    }

      //
      //  restore the one instance of Method.invoke that we actually want
      //
    if (util.match("/at junit.framework.TestCase.runTest/", in)) {
        return "\tat java.lang.reflect.Method.invoke(X)\n" + in;
    }

    for (final String pattern : patterns) {
        if(util.match(pattern, in)) {
            return null;
        }
    }
    return in;
  }
}
