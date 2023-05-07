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

/**
 * The sun.reflect.* and java.lang.reflect.* lines are not present in all JDKs.
 */
public class SunReflectFilter implements Filter {
    Perl5Util util = new Perl5Util();

    @Override
    public String filter(final String in) {
        if ((in == null) || util.match("/at sun.reflect/", in) || (in.indexOf("at java.lang.reflect.") >= 0)) {
            return null;
        }
        if (in.indexOf("Compiled Code") >= 0) {
            if (in.indexOf("junit.framework.TestSuite") >= 0) {
                return util.substitute("s/Compiled Code/TestSuite.java:XXX/", in);
            }
        }
        if (util.match("/\\(Method.java:.*\\)/", in)) {
            return util.substitute("s/\\(Method.java:.*\\)/(Native Method)/", in);
        }
        return in;
    }
}
