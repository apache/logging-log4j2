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
package org.apache.logging.log4j.flume.test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.SecretKeyProvider;

/**
 *
 */
@Plugin(name = "FlumeKeyProvider", category = "KeyProvider", elementType = "SecretKeyProvider", printObject = true)
public class FlumeKeyProvider implements SecretKeyProvider {

    private static final byte[] key = new byte[] {-7, -21, -118, -25, -79, 73, 72, -64, 0, 127, -93, -13, -38,
        3, -73, -31, -2, -74, 3, 28, 113, -55, -105, 9, -103, 97, -5, -54, 88, -110, 97, -4};

    @Override
    public SecretKey getSecretKey() {
        return new SecretKeySpec(key, "AES");
    }
}
