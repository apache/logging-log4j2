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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Compare {

    public static boolean compare(final String file1, final String file2) throws IOException {
        try (final BufferedReader in1 = new BufferedReader(new FileReader(file1));
                final BufferedReader in2 = new BufferedReader(new FileReader(file2))) {

            String s1;
            int lineCounter = 0;
            while ((s1 = in1.readLine()) != null) {
                lineCounter++;
                final String s2 = in2.readLine();
                if (!s1.equals(s2)) {
                    System.out.println("Files [" + file1 + "] and [" + file2 + "] differ on line " + lineCounter);
                    System.out.println("One reads:  [" + s1 + "].");
                    System.out.println("Other reads:[" + s2 + "].");
                    return false;
                }
            }

            // the second file is longer
            if (in2.read() != -1) {
                System.out.println("File [" + file2 + "] longer than file [" + file1 + "].");
                return false;
            }

            return true;
        }
    }
}
