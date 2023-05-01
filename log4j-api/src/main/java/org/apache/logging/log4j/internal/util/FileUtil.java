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
package org.apache.logging.log4j.internal.util;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Internal file utilities.
 */
public class FileUtil {

    private static final String META_INF = "META-INF/";
    public enum FileLocation {
        MODULE, CLASSLOADER, URL, FILE;
    }

    public FileLocation findLocation(Module module, ClassLoader classLoader, String fileName) {
        if (module != null) {
            try {
                URL url = module.getClassLoader().getResource(META_INF + fileName);
                if (url != null) {
                    return FileLocation.MODULE;
                }
            } catch (Exception ex) {
                // Ignore the exception.
            }
        }
        if (classLoader != null) {
            try {
                URL url = classLoader.getResource(META_INF + fileName);
                if (url != null) {
                    return FileLocation.CLASSLOADER;
                }
            } catch (Exception ex) {
                // Ignore the exception.
            }
        }
        try {
            File file = new File(fileName);
            if (file.exists()) {
                return FileLocation.FILE;
            }
        } catch (Exception ex) {
            // Ignore the error.
        }

        return null;
    }


}
