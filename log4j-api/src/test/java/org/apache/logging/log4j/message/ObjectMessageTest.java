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
package org.apache.logging.log4j.message;

import java.io.Serializable;
import java.math.BigDecimal;

import org.apache.log4j.util.SerialUtil;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests {@link ObjectMessage}.
 */
public class ObjectMessageTest {

    @Test
    public void testNull() {
        final ObjectMessage msg = new ObjectMessage(null);
        final String result = msg.getFormattedMessage();
        assertEquals("null", result);
    }

    @Test
    public void testNotNull() {
        final String testMsg = "Test message {}";
        final ObjectMessage msg = new ObjectMessage(testMsg);
        final String result = msg.getFormattedMessage();
        assertEquals(testMsg, result);
    }

    @Test
    public void testUnsafeWithMutableParams() { // LOG4J2-763
        final Mutable param = new Mutable().set("abc");
        final ObjectMessage msg = new ObjectMessage(param);

        // modify parameter before calling msg.getFormattedMessage
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertEquals("Expected most recent param value", "XYZ", actual);
    }

    @Test
    public void testSafeAfterGetFormattedMessageIsCalled() { // LOG4J2-763
        final Mutable param = new Mutable().set("abc");
        final ObjectMessage msg = new ObjectMessage(param);

        // modify parameter after calling msg.getFormattedMessage
        msg.getFormattedMessage();
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertEquals("Should use initial param value", "abc", actual);
    }
    
    @Test
    public void testSerializeWithSerializableParam() {
        BigDecimal big = BigDecimal.valueOf(123.456);
        ObjectMessage msg = new ObjectMessage(big);
        ObjectMessage other = SerialUtil.deserialize(SerialUtil.serialize(msg));
        assertEquals(msg, other);
    }
    
    @Test
    public void testDeserializeNonSerializableParamNotEqualIfToStringDiffers() {
        ObjectMessageTest nonSerializable = new ObjectMessageTest();
        assertFalse(nonSerializable instanceof Serializable);
        ObjectMessage msg = new ObjectMessage(nonSerializable);
        ObjectMessage other = SerialUtil.deserialize(SerialUtil.serialize(msg));
        assertNotEquals("Expected different: toString is different", msg, other);
    }
    
    @Ignore
    @Test
    public void testDeserializeNonSerializableParamEqualIfToStringSame() {
        class NonSerializable {
            public boolean equals(Object other) {
                return other instanceof NonSerializable; // a very lenient equals()
            }
        }
        NonSerializable nonSerializable = new NonSerializable();
        assertFalse(nonSerializable instanceof Serializable);
        ObjectMessage msg = new ObjectMessage(nonSerializable);
        ObjectMessage other = SerialUtil.deserialize(SerialUtil.serialize(msg));

// TODO this fails: msg.obj.equals(other.obj) is false...
// TODO our equals() implementation does not match the serialization mechanism
        assertEquals(msg, other);
    }
}
