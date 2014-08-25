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
package org.apache.logging.log4j.samples.util;

import org.apache.logging.log4j.util.Strings;

public class NamingUtils {

    public static String getPackageName(final String className) {
        return className.substring(0, className.lastIndexOf("."));
    }

    public static String getSimpleName(final String className) {
        return className.substring(className.lastIndexOf(".") + 1);
    }

    public static String getMethodShortName(final String name) {
        return name.replaceFirst("(get|set|is|has)", Strings.EMPTY);
    }

    public static String upperFirst(final String name) {
        return String.valueOf(name.charAt(0)).toUpperCase() + name.substring(1);
    }

    public static String lowerFirst(final String name) {
        return String.valueOf(name.charAt(0)).toLowerCase() + name.substring(1);
    }

    public static String getSetterName(final String fieldName) {
        return "set" + upperFirst(fieldName);
    }

    public static String getGetterName(final String fieldName, final String type) {
        return ("boolean".equals(type) ? "is" : "get") + upperFirst(fieldName);
    }

    public static void main(final String[] args) {
        final String blah = "com.test.generator.Classname";
        System.out.println(getSimpleName(blah));
        System.out.println(lowerFirst(getSimpleName(blah)));

        System.out.println(getPackageName(blah));

        System.out.println(getMethodShortName("getName"));
        System.out.println(getMethodShortName("setName"));
    }

    public static String getClassName(final String className) {
        return upperFirst(className.replaceAll("[^a-zA-Z0-9_]+", Strings.EMPTY));
    }

    public static String getFieldName(final String fieldName) {
        return fieldName.replaceAll("[^a-zA-Z0-9_]+", Strings.EMPTY);
    }

    public static String methodCaseName(final String variable) {
        return variable.substring(0, 1).toUpperCase() + variable.substring(1);
    }

    public static String getAccessorName(final String type, final String methodName) {
        String prefix = "get";
        if (type.equals("boolean")) {
            prefix = "is";
        }
        return prefix + methodCaseName(methodName);
    }

    public static String getMutatorName(final String methodName) {
        return "set" + methodCaseName(methodName);
    }
}
