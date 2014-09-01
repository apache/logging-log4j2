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
package org.apache.logging.log4j.jdk;

import java.util.logging.Level;

/**
 * Custom JUL Level for unit tests.
 */
public class CustomJdkLevel extends Level {

    private static final long serialVersionUID = 4681718777617726164L;

    protected CustomJdkLevel(final String name, final int value) {
        super(name, value);
    }

    // inside CONFIG range; should map to INFO
    public static final Level TEST = new CustomJdkLevel("TEST", 600);

    // just 1 below Level.SEVERE; should map to ERROR
    public static final Level DEFCON_2 = new CustomJdkLevel("DEFCON_2", 999);

    // above Level.SEVERE; should map to FATAL
    public static final Level DEFCON_1 = new CustomJdkLevel("DEFCON_1", 10000);
}
