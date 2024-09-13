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

    private static final String PATH = "src/test/resources/org/apache/logging/log4j/core/net/ssl/";

    /// Trust store (JKS) /////////////////////////////////////////////////////

    public static final String TRUSTSTORE_LOCATION = PATH + "trustStore.jks";

    public static char[] TRUSTSTORE_PWD() {
        return "aTrustStoreSecret".toCharArray();
    }

    public static final String TRUSTSTORE_TYPE = "JKS";

    /// Trust store #2 (JKS) //////////////////////////////////////////////////

    public static final String TRUSTSTORE2_LOCATION = PATH + "trustStore2.jks";

    public static char[] TRUSTSTORE2_PWD() {
        return "aTrustStoreSecret2".toCharArray();
    }

    public static final String TRUSTSTORE2_TYPE = "JKS";

    /// Key store (JKS) ///////////////////////////////////////////////////////

    public static final String KEYSTORE_LOCATION = PATH + "keyStore.jks";

    public static char[] KEYSTORE_PWD() {
        return "aKeyStoreSecret".toCharArray();
    }

    public static final String KEYSTORE_TYPE = "JKS";

    /// Key store #2 (JKS) ////////////////////////////////////////////////////

    public static final String KEYSTORE2_LOCATION = PATH + "keyStore2.jks";

    public static char[] KEYSTORE2_PWD() {
        return "aKeyStoreSecret2".toCharArray();
    }

    public static final String KEYSTORE2_TYPE = "JKS";

    /// Key store (P12) ///////////////////////////////////////////////////////

    public static final String KEYSTORE_P12_LOCATION = PATH + "keyStore.p12";

    public static char[] KEYSTORE_P12_PWD() {
        return "aKeyStoreSecret".toCharArray();
    }

    public static final String KEYSTORE_P12_TYPE = "PKCS12";

    /// Key store (P12 without password) //////////////////////////////////////

    public static final String KEYSTORE_P12_NOPASS_LOCATION = PATH + "keyStore-nopass.p12";

    public static char[] KEYSTORE_P12_NOPASS_PWD() {
        return new char[0];
    }

    public static final String KEYSTORE_P12_NOPASS_TYPE = "PKCS12";

    /// Other /////////////////////////////////////////////////////////////////

    public static final char[] NULL_PWD = null;

    public static final String WINDOWS_KEYSTORE_TYPE = "Windows-MY";

    public static final String WINDOWS_TRUSTSTORE_TYPE = "Windows-ROOT";
}
