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

import org.w3c.dom.Element;

import java.nio.file.Path;

import static org.apache.logging.log4j.internal.util.StringUtils.isBlank;
import static org.apache.logging.log4j.internal.util.StringUtils.trimNullable;

public final class PomUtils {

    private PomUtils() {}

    public static int readRootPomVersionMajor(final Path projectRootDirectory) {
        final String version = readRootPomVersion(projectRootDirectory);
        return versionMajor(version);
    }

    public static String readRootPomVersion(final Path projectRootDirectory) {
        final Path rootPomFile = projectRootDirectory.resolve("pom.xml");
        final Element projectElement = XmlReader.readXmlFileRootElement(rootPomFile, "project");
        final Element versionElement = XmlReader.requireChildElementMatchingName(projectElement, "version");
        final String version = trimNullable(versionElement.getTextContent());
        if (isBlank(version)) {
            throw XmlReader.failureAtXmlNode(versionElement, "blank `version`");
        }
        final String versionPattern = "^\\d+\\.\\d+.\\d+(-SNAPSHOT)?$";
        if (!version.matches(versionPattern)) {
            throw XmlReader.failureAtXmlNode(
                    versionElement, "`version` doesnt' match the expected pattern `%s`: `%s`", versionPattern, version);
        }
        return version;
    }

    public static int versionMajor(final String version) {
        return Integer.parseInt(version.split("\\.", 2)[0]);
    }

}
