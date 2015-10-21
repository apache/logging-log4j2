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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Compare {
    static final int B1_NULL = -1;
    static final int B2_NULL = -2;

    private static final InputStream open(
        final Class<?> testClass,
        final String fileName) throws IOException {
        final String resourceName = fileName;
        /* if (fileName.startsWith("witness/")) {
           resourceName = fileName.substring(fileName.lastIndexOf('/') + 1);
       } */
        InputStream is = testClass.getResourceAsStream(resourceName);
        if (is == null) {
            is = testClass.getClassLoader().getResourceAsStream(resourceName);
        }
        if (is == null) {
            final File file = new File(fileName);
            if (file.exists()) {
                is = new FileInputStream(file);
            } else {
                throw new FileNotFoundException("Resource "
                    + resourceName + " not found");
            }
        }
        return is;
    }

    public static boolean compare(final Class<?> testClass,
                                  final String file1,
                                  final String file2)
        throws IOException {
        try (final BufferedReader in1 = new BufferedReader(new FileReader(file1));
             final BufferedReader in2 = new BufferedReader(new InputStreamReader(open(testClass, file2)))) {
            return compare(testClass, file1, file2, in1, in2);
        }
    }

    public static boolean compare(
        final Class<?> testClass, final String file1, final String file2, final BufferedReader in1, final BufferedReader in2) throws IOException {

        String s1;
        int lineCounter = 0;

        while ((s1 = in1.readLine()) != null) {
            lineCounter++;

            final String s2 = in2.readLine();

            if (!s1.equals(s2)) {
                System.out.println(
                    "Files [" + file1 + "] and [" + file2 + "] differ on line "
                        + lineCounter);
                System.out.println("One reads:  [" + s1 + "].");
                System.out.println("Other reads:[" + s2 + "].");
                outputFile(testClass, file1);
                outputFile(testClass, file2);

                return false;
            }
        }

        // the second file is longer
        if (in2.read() != -1) {
            System.out.println(
                "File [" + file2 + "] longer than file [" + file1 + "].");
            outputFile(testClass, file1);
            outputFile(testClass, file2);

            return false;
        }

        return true;
    }

    /**
     * Prints file on the console.
     */
    private static void outputFile(final Class<?> testClass, final String file)
        throws IOException {
        final InputStream is = open(testClass, file);
        final BufferedReader in1 = new BufferedReader(new InputStreamReader(is));

        String s1;
        int lineCounter = 0;
        System.out.println("--------------------------------");
        System.out.println("Contents of " + file + ':');

        while ((s1 = in1.readLine()) != null) {
            lineCounter++;
            System.out.print(lineCounter);

            if (lineCounter < 10) {
                System.out.print("   : ");
            } else if (lineCounter < 100) {
                System.out.print("  : ");
            } else if (lineCounter < 1000) {
                System.out.print(" : ");
            } else {
                System.out.print(": ");
            }

            System.out.println(s1);
        }
        in1.close();
    }
}
