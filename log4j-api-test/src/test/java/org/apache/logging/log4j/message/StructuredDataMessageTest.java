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
package org.apache.logging.log4j.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 *
 */
class StructuredDataMessageTest {

    @Test
    void testMsg() {
        final String testMsg = "Test message {}";
        final StructuredDataMessage msg = new StructuredDataMessage("MsgId@12345", testMsg, "Alert");
        msg.put("message", testMsg);
        msg.put("project", "Log4j");
        msg.put("memo", "This is a very long test memo to prevent regression of LOG4J2-114");
        final String result = msg.getFormattedMessage();
        final String expected =
                "Alert [MsgId@12345 memo=\"This is a very long test memo to prevent regression of LOG4J2-114\" message=\"Test message {}\" project=\"Log4j\"] Test message {}";
        assertEquals(expected, result);
    }

    @Test
    void testMsgNonFull() {
        final String testMsg = "Test message {}";
        final StructuredDataMessage msg = new StructuredDataMessage("MsgId@12345", testMsg, "Alert");
        msg.put("message", testMsg);
        msg.put("project", "Log4j");
        msg.put("memo", "This is a very long test memo to prevent regression of LOG4J2-114");
        final String result = msg.getFormattedMessage(new String[] {"WHATEVER"});
        final String expected =
                "[MsgId@12345 memo=\"This is a very long test memo to prevent regression of LOG4J2-114\" message=\"Test message {}\" project=\"Log4j\"]";
        assertEquals(expected, result);
    }

    @Test
    void testMsgXml() {
        final String testMsg = "Test message {}";
        final StructuredDataMessage msg = new StructuredDataMessage("MsgId@12345", testMsg, "Alert");
        msg.put("message", testMsg);
        msg.put("project", "Log4j");
        msg.put("memo", "This is a very long test memo to prevent regression of LOG4J2-114");
        final String result = msg.getFormattedMessage(new String[] {"XML"});
        final String expected = "<StructuredData>\n"
                + "<type>Alert</type>\n"
                + "<id>MsgId@12345</id>\n"
                + "<message>Test message {}</message>\n"
                + "<Map>\n"
                + "  <Entry key=\"memo\">This is a very long test memo to prevent regression of LOG4J2-114</Entry>\n"
                + "  <Entry key=\"message\">Test message {}</Entry>\n"
                + "  <Entry key=\"project\">Log4j</Entry>\n"
                + "</Map>\n"
                + "</StructuredData>\n";
        assertEquals(expected, result);
    }

    @Test
    void testMsgXmlIncludesConstructorMessage() {
        // #4141: constructor message must appear (and be escaped) even when not put into the map
        final StructuredDataMessage msg = new StructuredDataMessage("anId", "a <msg> & more", "aType");
        final String result = msg.getFormattedMessage(new String[] {"XML"});
        final String expected = "<StructuredData>\n"
                + "<type>aType</type>\n"
                + "<id>anId</id>\n"
                + "<message>a &lt;msg&gt; &amp; more</message>\n"
                + "<Map>\n"
                + "</Map>\n"
                + "</StructuredData>\n";
        assertEquals(expected, result);
    }

    @Test
    void testMsgXmlDistinguishesConstructorMessageFromMapEntry() {
        // #4141: free-form message and a map entry keyed "message" are independent
        final StructuredDataMessage msg = new StructuredDataMessage("anId", "a message", "aType");
        msg.put("message", "foo");
        final String result = msg.getFormattedMessage(new String[] {"XML"});
        final String expected = "<StructuredData>\n"
                + "<type>aType</type>\n"
                + "<id>anId</id>\n"
                + "<message>a message</message>\n"
                + "<Map>\n"
                + "  <Entry key=\"message\">foo</Entry>\n"
                + "</Map>\n"
                + "</StructuredData>\n";
        assertEquals(expected, result);
    }

    @Test
    void testXmlEncodingOfIdAndType1() {
        final String id = "i<&d>" + XmlFixture.TEXT;
        final String type = "t>yp<e&" + XmlFixture.TEXT;
        // null message: keep this test focused on id/type escaping (and covers omission of <message>)
        final String actualXml = new StructuredDataMessage(id, null, type).getFormattedMessage(new String[] {"XML"});
        final String expectedXml = "<StructuredData>\n"
                + "<type>t&gt;yp&lt;e&amp;" + XmlFixture.ENCODED_TEXT
                + "</type>\n"
                + "<id>i&lt;&amp;d&gt;" + XmlFixture.ENCODED_TEXT
                + "</id>\n"
                // Following part is encoded by `MapMessage::asXml`, hence, fuzzed & tested elsewhere
                + "<Map>\n"
                + "</Map>\n"
                + "</StructuredData>\n";
        assertEquals(expectedXml, actualXml);
    }

    @Test
    void testXmlEncodingOfIdAndType2() {
        final String idName = "id&<-name>" + XmlFixture.TEXT;
        final String idEnterpriseNumber = "id&<-enterprise-number>" + XmlFixture.TEXT;
        final String[] idRequired = {"id&<-required>" + XmlFixture.TEXT};
        final String[] idOptional = {"id&<-optional>" + XmlFixture.TEXT};
        final String type = "t>yp<e&" + XmlFixture.TEXT;
        final StructuredDataId id =
                new StructuredDataId(idName, idEnterpriseNumber, idRequired, idOptional, Integer.MAX_VALUE);
        final String actualXml = new StructuredDataMessage(id, null, type).getFormattedMessage(new String[] {"XML"});
        final String expectedXml = "<StructuredData>\n"
                + "<type>t&gt;yp&lt;e&amp;" + XmlFixture.ENCODED_TEXT
                + "</type>\n"
                + "<id>" + "id&amp;&lt;-name&gt;" + XmlFixture.ENCODED_TEXT
                + "@id&amp;&lt;-enterprise-number&gt;" + XmlFixture.ENCODED_TEXT
                + "</id>\n"
                // Following part is encoded by `MapMessage::asXml`, hence, fuzzed & tested elsewhere
                + "<Map>\n"
                + "</Map>\n"
                + "</StructuredData>\n";
        assertEquals(expectedXml, actualXml);
    }

    private enum XmlFixture {
        ;

        private static final String TEXT = "'";

        private static final String ENCODED_TEXT = "&apos;";
    }

    @Test
    void testBuilder() {
        final String testMsg = "Test message {}";
        final StructuredDataMessage msg = new StructuredDataMessage("MsgId@12345", testMsg, "Alert")
                .with("message", testMsg)
                .with("project", "Log4j")
                .with("memo", "This is a very long test memo to prevent regression of LOG4J2-114");
        final String result = msg.getFormattedMessage();
        final String expected =
                "Alert [MsgId@12345 memo=\"This is a very long test memo to prevent regression of LOG4J2-114\" message=\"Test message {}\" project=\"Log4j\"] Test message {}";
        assertEquals(expected, result);
    }

    @Test
    void testMsgWithKeyTooLong() {
        final String testMsg = "Test message {}";
        final StructuredDataMessage msg = new StructuredDataMessage("MsgId@12345", testMsg, "Alert");
        assertThrows(
                IllegalArgumentException.class,
                () -> msg.put("This is a very long key that will violate the key length validation", "Testing"));
    }

    @Test
    void testMutableByDesign() { // LOG4J2-763
        final String testMsg = "Test message {}";
        final StructuredDataMessage msg = new StructuredDataMessage("MsgId@1", testMsg, "Alert");

        // modify parameter before calling msg.getFormattedMessage
        msg.put("message", testMsg);
        msg.put("project", "Log4j");
        final String result = msg.getFormattedMessage();
        final String expected = "Alert [MsgId@1 message=\"Test message {}\" project=\"Log4j\"] Test message {}";
        assertEquals(expected, result);

        // modify parameter after calling msg.getFormattedMessage
        msg.put("memo", "Added later");
        final String result2 = msg.getFormattedMessage();
        final String expected2 =
                "Alert [MsgId@1 memo=\"Added later\" message=\"Test message {}\" project=\"Log4j\"] Test message {}";
        assertEquals(expected2, result2);
    }

    @Test
    void testEnterpriseNoAsOidFragment() {
        final String testMsg = "Test message {}";
        final StructuredDataMessage structuredDataMessage =
                new StructuredDataMessage("XX_DATA@1234.55.6.7", testMsg, "Nothing");
        assertNotNull(structuredDataMessage);
    }

    @Test
    void sdIdAndMsgIdOfMaximumLengthAreAccepted() {
        final String sdId = "abcdefghijklmnopqrstuvwxyz-@1234";
        final String msgId = "!\"#$%&'()*+,-./0123456789:;<=>?@";
        assertEquals(32, sdId.length());
        assertEquals(32, msgId.length());
        final StructuredDataMessage msg = new StructuredDataMessage(sdId, "message", msgId);
        assertEquals("abcdefghijklmnopqrstuvwxyz-", msg.getId().getName());
        assertEquals("1234", msg.getId().getEnterpriseNumber());
        assertEquals(msgId, msg.getType());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "abcdefghijklmnopqrstuvwxyz-@12345",
                "an id",
                "an=id",
                "an]id",
                "an\"id",
                "an\tid",
                "an\nid",
                "an\u00e9id",
                "@12345",
                "anId@",
                "anId@12 34",
                "anId@123@45"
            })
    void invalidSdIdIsRejected(final String sdId) {
        assertThrows(IllegalArgumentException.class, () -> new StructuredDataMessage(sdId, "message", "aType"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new StructuredDataMessage(sdId, "message", "aType", Collections.emptyMap()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abcdefghijklmnopqrstuvwxyz0123456", "a type", "a\ttype", "a\ntype", "a\u00e9type"})
    void invalidMsgIdIsRejected(final String msgId) {
        assertThrows(IllegalArgumentException.class, () -> new StructuredDataMessage("anId", "message", msgId));
        final StructuredDataId sdId = new StructuredDataId("anId");
        assertThrows(IllegalArgumentException.class, () -> new StructuredDataMessage(sdId, "message", msgId));
    }

    @Test
    void nullStructuredDataIdIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StructuredDataMessage((StructuredDataId) null, "message", "aType"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a name", "a=name", "a]name", "a\"name", "a@name", "a\u00e9name"})
    void invalidStructuredDataIdNameIsRejected(final String name) {
        assertThrows(IllegalArgumentException.class, () -> new StructuredDataId(name, "12345", null, null));
        assertThrows(IllegalArgumentException.class, () -> new StructuredDataId("aName", name, null, null));
    }

    @Test
    void structuredDataIdWithoutEnterpriseNumberIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new StructuredDataId("aName", (String) null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new StructuredDataId("aName", "", null, null));
    }

    @Test
    void invalidKeyInConstructorMapIsRejected() {
        final Map<String, String> data = Collections.singletonMap("a key", "value");
        assertThrows(IllegalArgumentException.class, () -> new StructuredDataMessage("anId", "message", "aType", data));
    }

    @Test
    void invalidKeyInPutAllIsRejectedAndNothingIsAdded() {
        final StructuredDataMessage msg = new StructuredDataMessage("anId", "message", "aType");
        final Map<String, String> data = new HashMap<>();
        data.put("valid", "value");
        data.put("a key", "value");
        assertThrows(IllegalArgumentException.class, () -> msg.putAll(data));
        assertTrue(msg.getData().isEmpty());
    }

    @Test
    void onlyDeclaredKeysAreAccepted() {
        final StructuredDataId sdId =
                new StructuredDataId("anId", "12345", new String[] {"required"}, new String[] {"optional"});
        final StructuredDataMessage msg = new StructuredDataMessage(sdId, "message", "aType")
                .with("required", "a")
                .with("optional", "b");
        assertEquals("a", msg.get("required"));
        assertEquals("b", msg.get("optional"));
        assertThrows(IllegalArgumentException.class, () -> msg.with("undeclared", "c"));
        assertThrows(IllegalArgumentException.class, () -> msg.put("undeclared", "c"));
        assertThrows(IllegalArgumentException.class, () -> msg.putAll(Collections.singletonMap("undeclared", "c")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new StructuredDataMessage(sdId, "message", "aType", Collections.singletonMap("undeclared", "c")));
        assertThrows(
                IllegalArgumentException.class, () -> msg.newInstance(Collections.singletonMap("undeclared", "c")));
    }

    @Test
    void registeredStructuredDataIdAcceptsOnlyItsParameters() {
        final StructuredDataMessage msg = new StructuredDataMessage(StructuredDataId.TIME_QUALITY, null, "aType");
        msg.put("tzKnown", "1");
        assertThrows(IllegalArgumentException.class, () -> msg.put("ip", "127.0.0.1"));
    }

    @Test
    void setIdRejectsStructuredDataIdThatDoesNotDeclareExistingKeys() {
        final ChangeableIdMessage msg = new ChangeableIdMessage();
        msg.put("undeclared", "value");
        assertThrows(IllegalArgumentException.class, () -> msg.changeId(StructuredDataId.TIME_QUALITY));
        assertThrows(IllegalArgumentException.class, () -> msg.changeId((StructuredDataId) null));
        assertThrows(IllegalArgumentException.class, () -> msg.changeType("a type"));
        assertEquals("anId", msg.getId().getName());
    }

    @Test
    void newInstanceKeepsMaxLength() {
        final String key = "abcdefghijklmnopqrstuvwxyz0123456789";
        final StructuredDataMessage msg = new StructuredDataMessage("anId", "message", "aType", 64).with(key, "value");
        assertEquals("value", msg.newInstance(msg.getData()).get(key));
    }

    private static final class ChangeableIdMessage extends StructuredDataMessage {

        private static final long serialVersionUID = 1L;

        private ChangeableIdMessage() {
            super("anId", "message", "aType");
        }

        private void changeId(final StructuredDataId sdId) {
            setId(sdId);
        }

        private void changeType(final String msgId) {
            setType(msgId);
        }
    }
}
