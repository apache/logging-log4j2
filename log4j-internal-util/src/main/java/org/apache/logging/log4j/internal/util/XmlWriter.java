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

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;

public final class XmlWriter {

    private static final Charset ENCODING = StandardCharsets.UTF_8;

    private XmlWriter() {}

    public static void toFile(final Path filepath, final Consumer<Document> documentConsumer) {
        try {
            final String xml = toString(documentConsumer);
            final byte[] xmlBytes = xml.getBytes(ENCODING);
            Files.createDirectories(filepath.getParent());
            Files.write(filepath, xmlBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (final Exception error) {
            final String message = String.format("failed writing XML to file `%s`", filepath);
            throw new RuntimeException(message, error);
        }
    }

    public static String toString(final Consumer<Document> documentConsumer) {
        try {

            // Create the document.
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            // Append the license comment.
            final Document document = documentBuilder.newDocument();
            document.setXmlStandalone(true);
            final Comment licenseComment = document.createComment("\n" +
                    "   Licensed to the Apache Software Foundation (ASF) under one or more\n" +
                    "   contributor license agreements.  See the NOTICE file distributed with\n" +
                    "   this work for additional information regarding copyright ownership.\n" +
                    "   The ASF licenses this file to You under the Apache License, Version 2.0\n" +
                    "   (the \"License\"); you may not use this file except in compliance with\n" +
                    "   the License.  You may obtain a copy of the License at\n" +
                    "\n" +
                    "       http://www.apache.org/licenses/LICENSE-2.0\n" +
                    "\n" +
                    "   Unless required by applicable law or agreed to in writing, software\n" +
                    "   distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                    "   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                    "   See the License for the specific language governing permissions and\n" +
                    "   limitations under the License." +
                    "\n");
            document.appendChild(licenseComment);

            // Execute request changes.
            documentConsumer.accept(document);

            // Serialize the document.
            return serializeXmlDocument(document);

        } catch (final Exception error) {
            throw new RuntimeException("failed writing XML", error);
        }
    }

    private static String serializeXmlDocument(final Document document) throws Exception {
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        final StreamResult result = new StreamResult(new StringWriter());
        final DOMSource source = new DOMSource(document);
        transformer.setOutputProperty(OutputKeys.ENCODING, ENCODING.name());
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(source, result);
        return result.getWriter().toString()
                // Life is too short to solve DOM transformer issues decently.
                .replace("?><!--", "?>\n<!--")
                .replace("--><", "-->\n<");
    }

}
