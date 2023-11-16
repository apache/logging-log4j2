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
package org.apache.logging.log4j.core.lookup;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Looks up keys from resource bundles.
 */
@Plugin(name = "bundle", category = StrLookup.CATEGORY)
public class ResourceBundleLookup extends AbstractLookup {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Marker LOOKUP = MarkerManager.getMarker("LOOKUP");

    /**
     * Looks up the value for the key in the format "BundleName:BundleKey".
     *
     * For example: "com.domain.messages:MyKey".
     *
     * @param event
     *            The current LogEvent.
     * @param key
     *            the key to be looked up, may be null
     * @return The value associated with the key.
     */
    @Override
    public String lookup(final LogEvent event, final String key) {
        if (key == null) {
            return null;
        }
        final String[] keys = key.split(":");
        final int keyLen = keys.length;
        if (keyLen != 2) {
            LOGGER.warn(LOOKUP, "Bad ResourceBundle key format [{}]. Expected format is BundleName:KeyName.", key);
            return null;
        }
        final String bundleName = keys[0];
        final String bundleKey = keys[1];
        try {
            // The ResourceBundle class caches bundles, no need to cache here.
            return ResourceBundle.getBundle(bundleName).getString(bundleKey);
        } catch (final MissingResourceException e) {
            LOGGER.warn(LOOKUP, "Error looking up ResourceBundle [{}].", bundleName, e);
            return null;
        }
    }
}
