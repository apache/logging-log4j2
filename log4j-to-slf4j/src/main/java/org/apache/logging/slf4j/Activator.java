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
package org.apache.logging.slf4j;

import org.apache.logging.log4j.util.ProviderActivator;
import org.osgi.annotation.bundle.Header;

@Header(name = org.osgi.framework.Constants.BUNDLE_ACTIVATOR, value = "${@class}")
@Header(
        name = org.osgi.framework.Constants.BUNDLE_ACTIVATIONPOLICY,
        value = org.osgi.framework.Constants.ACTIVATION_LAZY)
public class Activator extends ProviderActivator {

    public Activator() {
        super(new SLF4JProvider());
    }
}
