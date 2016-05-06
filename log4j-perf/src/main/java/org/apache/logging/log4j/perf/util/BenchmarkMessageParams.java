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
package org.apache.logging.log4j.perf.util;

import java.util.Arrays;

/**
 * Created by remko on 5/5/2016.
 */
public class BenchmarkMessageParams {
    final static char[] CHARS = new char[16];
    static {
        Arrays.fill(CHARS, 'a');
    }
    public final static String TEST = new String(CHARS);

    public static volatile String one = "1";
    public static volatile String two = "2";
    public static volatile String three = "3";
    public static volatile String four = "4";
    public static volatile String five = "5";
    public static volatile String six = "6";
    public static volatile String seven = "7";
    public static volatile String eight = "8";
    public static volatile String nine = "9";
    public static volatile String ten = "10";
    public static volatile String eleven = "11";
}
