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
package org.apache.log4j.spi;

import junit.framework.TestCase;

/**
 * Tests for LocationInfo.
 */
public class LocationInfoTest extends TestCase {

    /**
     * Tests four parameter constructor.
     */
    public void testFourParamConstructor() {
        final String className = LocationInfoTest.class.getName();
        final String methodName = "testFourParamConstructor";
        final String fileName = "LocationInfoTest.java";
        final String lineNumber = "41";
        final LocationInfo li = new LocationInfo(fileName, className, methodName, lineNumber);
        assertEquals(className, li.getClassName());
        assertEquals(methodName, li.getMethodName());
        assertEquals(fileName, li.getFileName());
        assertEquals(lineNumber, li.getLineNumber());
        assertEquals(className + "." + methodName + "(" + fileName + ":" + lineNumber + ")", li.fullInfo);
    }

    /**
     * Class with name that is a substring of its caller.
     */
    private static class NameSubstring {
        /**
         * Construct a LocationInfo. Location should be immediate caller of this method.
         *
         * @return location info.
         */
        public static LocationInfo getInfo() {
            return new LocationInfo(new Throwable(), NameSubstring.class.getName());
        }
    }

    /**
     * Class whose name is contains the name of the class that obtains the LocationInfo.
     */
    private static class NameSubstringCaller {
        /**
         * Construct a locationInfo. Location should be this location.
         *
         * @return location info.
         */
        public static LocationInfo getInfo() {
            return NameSubstring.getInfo();
        }
    }

    /**
     * Tests creation of location info when the logger class name is a substring of one of the other classes in the stack
     * trace. See bug 44888.
     */
    public void testLocationInfo() {
        final LocationInfo li = NameSubstringCaller.getInfo();
        assertEquals(NameSubstringCaller.class.getName(), li.getClassName());
        assertEquals("getInfo", li.getMethodName());
    }
}
