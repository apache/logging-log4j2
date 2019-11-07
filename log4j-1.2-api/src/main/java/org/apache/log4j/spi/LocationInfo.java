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
package org.apache.log4j.spi;

/**
 The internal representation of caller location information.

 @since 0.8.3
 */
public class LocationInfo implements java.io.Serializable {

    private final StackTraceElement element;

    public String fullInfo;

    public LocationInfo(StackTraceElement element) {
        this.element = element;
    }

    /**
     When location information is not available the constant
     <code>NA</code> is returned. Current value of this string
     constant is <b>?</b>.  */
    public final static String NA = "?";

    static final long serialVersionUID = -1325822038990805636L;


    /**
     Return the fully qualified class name of the caller making the
     logging request.
     */
    public
    String getClassName() {
        return element.getClassName();
    }

    /**
     Return the file name of the caller.
     */
    public
    String getFileName() {
        return element.getFileName();
    }

    /**
     Returns the line number of the caller.
     */
    public
    String getLineNumber() {
        return Integer.toString(element.getLineNumber());
    }

    /**
     Returns the method name of the caller.
     */
    public
    String getMethodName() {
        return element.getMethodName();
    }
}
