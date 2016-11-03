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

package org.apache.logging.log4j.osgi.tests.junit;

import java.io.FileReader;
import java.io.IOException;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Provides tests with bundle information. Reads the {@code pom.xml} in the current directory to get project settings.
 */
public class BundleTestInfo {

    private final MavenProject project;

    /**
     * Constructs a new helper objects and initializes itself.
     */
    public BundleTestInfo() {
        try (final FileReader reader = new FileReader("pom.xml")) {
            // get a raw POM view, not a fully realized POM object.
            final Model model = new MavenXpp3Reader().read(reader);
            this.project = new MavenProject(model);
        } catch (final IOException | XmlPullParserException e) {
            throw new IllegalStateException("Could not read pom.xml", e);
        }
    }

    /**
     * Gets the Maven artifact ID.
     *
     * @return the Maven artifact ID.
     */
    public String getArtifactId() {
        return project.getArtifactId();
    }

    /**
     * Gets the Maven version String.
     *
     * @return the Maven version String.
     */
    public String getVersion() {
        return project.getVersion();
    }

    @Override
    public String toString() {
        return "BundleTestInfo [project=" + project + "]";
    }

}
