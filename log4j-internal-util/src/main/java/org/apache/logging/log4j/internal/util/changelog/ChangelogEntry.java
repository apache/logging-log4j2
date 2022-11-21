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
package org.apache.logging.log4j.internal.util.changelog;

import org.apache.logging.log4j.internal.util.XmlReader;
import org.apache.logging.log4j.internal.util.XmlWriter;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.logging.log4j.internal.util.StringUtils.trimNullable;

public final class ChangelogEntry {

    public final Type type;

    public final List<Issue> issues;

    public final List<Author> authors;

    public final Description description;

    public enum Type {

        ADDED,

        CHANGED,

        DEPRECATED,

        REMOVED,

        FIXED,

        SECURITY;

        private String toXmlAttribute() {
            return toString().toLowerCase(Locale.US);
        }

        private static Type fromXmlAttribute(final String attribute) {
            final String upperCaseAttribute = attribute != null ? attribute.toUpperCase(Locale.US) : null;
            return Type.valueOf(upperCaseAttribute);
        }

    }

    public static final class Issue {

        public final String id;

        public final String link;

        public Issue(final String id, final String link) {
            this.id = id;
            this.link = link;
        }

    }

    public static final class Author {

        public final String id;

        public final String name;

        public Author(final String id, final String name) {
            this.id = id;
            this.name = name;
        }

    }

    public static final class Description {

        public final String format;

        public final String text;

        public Description(final String format, final String text) {
            this.format = format;
            this.text = text;
        }

    }

    public ChangelogEntry(
            final Type type,
            final List<Issue> issues,
            final List<Author> authors,
            final Description description) {
        this.type = type;
        this.issues = issues;
        this.authors = authors;
        this.description = description;
    }

    public void writeToXmlFile(final Path path) {
        XmlWriter.toFile(path, document -> {

            // Create the `entry` root element.
            final Element entryElement = document.createElement("entry");
            entryElement.setAttribute("type", type.toXmlAttribute());
            document.appendChild(entryElement);

            // Create the `issue` elements.
            issues.forEach(issue -> {
                final Element issueElement = document.createElement("issue");
                issueElement.setAttribute("id", issue.id);
                issueElement.setAttribute("link", issue.link);
                entryElement.appendChild(issueElement);
            });

            // Create the `author` elements.
            authors.forEach(author -> {
                final Element authorElement = document.createElement("author");
                if (author.id != null) {
                    authorElement.setAttribute("id", author.id);
                } else {
                    authorElement.setAttribute("name", author.name);
                }
                entryElement.appendChild(authorElement);
            });

            // Create the `description` element.
            final Element descriptionElement = document.createElement("description");
            if (description.format != null) {
                descriptionElement.setAttribute("format", description.format);
            }
            descriptionElement.setTextContent(description.text);
            entryElement.appendChild(descriptionElement);

        });
    }

    public static ChangelogEntry readFromXmlFile(final Path path) {

        // Read the `entry` root element.
        final Element entryElement = XmlReader.readXmlFileRootElement(path, "entry");
        final String typeAttribute = entryElement.getAttribute("type");
        final Type type;
        try {
            type = Type.fromXmlAttribute(typeAttribute);
        } catch (final Exception error) {
            throw XmlReader.failureAtXmlNode(error, entryElement, "`type` attribute read failure");
        }

        // Read the `issue` elements.
        final NodeList issueElements = entryElement.getElementsByTagName("issue");
        final int issueCount = issueElements.getLength();
        final List<Issue> issues = IntStream
                .range(0, issueCount)
                .mapToObj(issueIndex -> {
                    final Element issueElement = (Element) issueElements.item(issueIndex);
                    final String issueId = issueElement.getAttribute("id");
                    final String issueLink = issueElement.getAttribute("link");
                    return new Issue(issueId, issueLink);
                })
                .collect(Collectors.toList());

        // Read the `author` elements.
        final NodeList authorElements = entryElement.getElementsByTagName("author");
        final int authorCount = authorElements.getLength();
        if (authorCount < 1) {
            throw XmlReader.failureAtXmlNode(entryElement, "no `author` elements found");
        }
        final List<Author> authors = IntStream
                .range(0, authorCount)
                .mapToObj(authorIndex -> {
                    final Element authorElement = (Element) authorElements.item(authorIndex);
                    final String authorId = authorElement.getAttribute("id");
                    final String authorName = authorElement.getAttribute("name");
                    return new Author(authorId, authorName);
                })
                .collect(Collectors.toList());

        // Read the `description` element.
        final NodeList descriptionElements = entryElement.getElementsByTagName("description");
        final int descriptionCount = descriptionElements.getLength();
        if (descriptionCount != 1) {
            throw XmlReader.failureAtXmlNode(
                    entryElement, "was expecting a single `description` element, found: %d", descriptionCount);
        }
        final Element descriptionElement = (Element) descriptionElements.item(0);
        final String descriptionFormat = descriptionElement.getAttribute("format");
        final String descriptionText = trimNullable(descriptionElement.getTextContent());
        final Description description = new Description(descriptionFormat, descriptionText);

        // Create the instance.
        return new ChangelogEntry(type, issues, authors, description);

    }

}
