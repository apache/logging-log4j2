/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.internal.util.changelog;

import org.apache.logging.log4j.internal.util.XmlReader;
import org.apache.logging.log4j.internal.util.XmlWriter;
import org.w3c.dom.Element;

import java.nio.file.Path;

import static org.apache.logging.log4j.internal.util.StringUtils.trimNullable;

public final class ChangelogRelease {

    public final String version;

    public final String date;

    public ChangelogRelease(final String version, final String date) {
        this.version = version;
        this.date = date;
    }

    public void writeToXmlFile(final Path path) {
        XmlWriter.toFile(path, document -> {
            final Element releaseElement = document.createElement("release");
            releaseElement.setAttribute("version", version);
            releaseElement.setAttribute("date", date);
            document.appendChild(releaseElement);
        });
    }

    public static ChangelogRelease readFromXmlFile(final Path path) {

        // Read the XML file.
        final Element releaseElement = XmlReader.readXmlFileRootElement(path, "release");

        // Read the `version` attribute.
        final String version = trimNullable(releaseElement.getAttribute("version"));
        if (version == null) {
            throw new IllegalArgumentException("blank or missing attribute: `version`");
        }

        // Read the `date` attribute.
        final String date = trimNullable(releaseElement.getAttribute("date"));
        if (date == null) {
            throw new IllegalArgumentException("blank or missing attribute: `date`");
        }

        // Create the instance.
        return new ChangelogRelease(version, date);

    }

}
