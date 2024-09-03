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
package org.apache.logging.log4j.core.net.ssl;

public final class SslKeyStoreConstants {

    public static final String RESOURCE_ROOT = "org/apache/logging/log4j/core/net/ssl/";

    public static final String PATH = "src/test/resources/" + RESOURCE_ROOT;

    /// Trust store (JKS) /////////////////////////////////////////////////////

    public static final String TRUSTSTORE_FILE_PATH = PATH + "trustStore.jks";

    public static final String TRUSTSTORE_FILE_RESOURCE = RESOURCE_ROOT + "trustStore.jks";

    public static char[] TRUSTSTORE_PWD() {
        return "aTrustStoreSecret".toCharArray();
    }

    public static final String TRUSTSTORE_TYPE = "JKS";

    /// Trust store #2 (JKS) //////////////////////////////////////////////////

    public static final String TRUSTSTORE2_FILE_PATH = PATH + "trustStore2.jks";

    public static final String TRUSTSTORE2_FILE_RESOURCE = RESOURCE_ROOT + "trustStore2.jks";

    public static char[] TRUSTSTORE2_PWD() {
        return "aTrustStoreSecret2".toCharArray();
    }

    public static final String TRUSTSTORE2_TYPE = "JKS";

    /// Key store (JKS) ///////////////////////////////////////////////////////

    public static final String KEYSTORE_FILE_PATH = PATH + "keyStore.jks";

    public static final String KEYSTORE_FILE_RESOURCE = RESOURCE_ROOT + "keyStore.jks";

    public static char[] KEYSTORE_PWD() {
        return "aKeyStoreSecret".toCharArray();
    }

    public static final String KEYSTORE_TYPE = "JKS";

    /// Key store #2 (JKS) ////////////////////////////////////////////////////

    public static final String KEYSTORE2_FILE_PATH = PATH + "keyStore2.jks";

    public static final String KEYSTORE2_FILE_RESOURCE = RESOURCE_ROOT + "keyStore2.jks";

    public static char[] KEYSTORE2_PWD() {
        return "aKeyStoreSecret2".toCharArray();
    }

    public static final String KEYSTORE2_TYPE = "JKS";

    /// Key store (P12) ///////////////////////////////////////////////////////

    public static final String KEYSTORE_P12_FILE_PATH = PATH + "keyStore.p12";

    public static char[] KEYSTORE_P12_PWD() {
        return "aKeyStoreSecret".toCharArray();
    }

    public static final String KEYSTORE_P12_TYPE = "PKCS12";

    /// Key store (P12 without password) //////////////////////////////////////

    public static final String KEYSTORE_P12_NOPASS_FILE_PATH = PATH + "keyStore-nopass.p12";

    public static char[] KEYSTORE_P12_NOPASS_PWD() {
        return new char[0];
    }

    public static final String KEYSTORE_P12_NOPASS_TYPE = "PKCS12";

    /// Other /////////////////////////////////////////////////////////////////

    public static final char[] NULL_PWD = null;
}
