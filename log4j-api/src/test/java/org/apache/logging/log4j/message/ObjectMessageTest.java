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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.Serializable;
import java.math.BigDecimal;
import org.apache.logging.log4j.junit.Mutable;
import org.apache.logging.log4j.junit.SerialUtil;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ObjectMessage}.
 */
public class ObjectMessageTest {

    @Test
    public void testNull() {
        final ObjectMessage msg = new ObjectMessage(null);
        final String result = msg.getFormattedMessage();
        assertThat(result).isEqualTo("null");
    }

    @Test
    public void testNotNull() {
        final String testMsg = "Test message {}";
        final ObjectMessage msg = new ObjectMessage(testMsg);
        final String result = msg.getFormattedMessage();
        assertThat(result).isEqualTo(testMsg);
    }

    @Test
    public void testUnsafeWithMutableParams() { // LOG4J2-763
        final Mutable param = new Mutable().set("abc");
        final ObjectMessage msg = new ObjectMessage(param);

        // modify parameter before calling msg.getFormattedMessage
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertThat(actual).describedAs("Expected most recent param value").isEqualTo("XYZ");
    }

    @Test
    public void testSafeAfterGetFormattedMessageIsCalled() { // LOG4J2-763
        final Mutable param = new Mutable().set("abc");
        final ObjectMessage msg = new ObjectMessage(param);

        // modify parameter after calling msg.getFormattedMessage
        msg.getFormattedMessage();
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertThat(actual).describedAs("Should use initial param value").isEqualTo("abc");
    }

    @Test
    public void testSerializeWithSerializableParam() {
        final BigDecimal big = BigDecimal.valueOf(123.456);
        final ObjectMessage msg = new ObjectMessage(big);
        final ObjectMessage other = SerialUtil.deserialize(SerialUtil.serialize(msg));
        assertThat(other).isEqualTo(msg);
    }

    @Test
    public void testDeserializeNonSerializableParamEqualIfToStringSame() {
        class NonSerializable {
            @Override
            public boolean equals(final Object other) {
                return other instanceof NonSerializable; // a very lenient equals()
            }

            @Override
            public int hashCode() {
                return NonSerializable.class.hashCode();
            }
        }
        final NonSerializable nonSerializable = new NonSerializable();
        assertThat(nonSerializable instanceof Serializable).isFalse();
        final ObjectMessage msg = new ObjectMessage(nonSerializable);
        final ObjectMessage other = SerialUtil.deserialize(SerialUtil.serialize(msg));

        assertThat(other).isEqualTo(msg);
        assertThat(msg).isEqualTo(other);
    }

    @Test
    public void formatTo_usesCachedMessageString() throws Exception {
        final StringBuilder charSequence = new StringBuilder("initial value");
        final ObjectMessage message = new ObjectMessage(charSequence);
        assertThat(message.getFormattedMessage()).isEqualTo("initial value");

        charSequence.setLength(0);
        charSequence.append("different value");

        final StringBuilder result = new StringBuilder();
        message.formatTo(result);
        assertThat(result.toString()).isEqualTo("initial value");
    }
}
