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
package org.apache.log4j.builders;

import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.w3c.dom.Element;

/**
 * Parses DOM and properties.
 *
 * @param <T> The type to build.
 */
public interface Parser<T> extends Builder<T> {

    /**
     * Parses a DOM Element.
     *
     * @param element the DOM Element.
     * @param config the XML configuration.
     * @return parse result.
     */
    T parse(Element element, XmlConfiguration config);

    /**
     * Parses a PropertiesConfigurationt.
     *
     * @param element the PropertiesConfiguration.
     * @return parse result.
     */
    T parse(PropertiesConfiguration config);
}
