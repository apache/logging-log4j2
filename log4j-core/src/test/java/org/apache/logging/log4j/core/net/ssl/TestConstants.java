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
package org.apache.logging.log4j.core.net.ssl;

public class TestConstants {

    public static final String SOURCE_FOLDER =  "src/test/resources/";
    public static final String RESOURCE_ROOT =  "org/apache/logging/log4j/core/net/ssl/";

    public static final String PATH =  SOURCE_FOLDER + RESOURCE_ROOT;
    public static final String TRUSTSTORE_PATH = PATH;
    public static final String TRUSTSTORE_RESOURCE = RESOURCE_ROOT;
    public static final String TRUSTSTORE_FILE = TRUSTSTORE_PATH + "truststore.jks";
    public static final String TRUSTSTORE_FILE_RESOURCE = TRUSTSTORE_RESOURCE + "truststore.jks";
    public static final char[] TRUSTSTORE_PWD() { return "changeit".toCharArray(); }
    public static final String TRUSTSTORE_TYPE = "JKS";

    public static final String KEYSTORE_PATH = PATH;
    public static final String KEYSTORE_RESOURCE = RESOURCE_ROOT;
    public static final String KEYSTORE_FILE = KEYSTORE_PATH + "client.log4j2-keystore.jks";
    public static final String KEYSTORE_FILE_RESOURCE = KEYSTORE_RESOURCE + "client.log4j2-keystore.jks";
    public static final char[] KEYSTORE_PWD() { return "changeit".toCharArray(); }
    public static final String KEYSTORE_TYPE = "JKS";

    public static final char[] NULL_PWD = null;
}
