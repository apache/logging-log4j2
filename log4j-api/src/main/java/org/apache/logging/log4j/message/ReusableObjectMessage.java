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

import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 * Mutable Message wrapper around an Object message.
 * @since 2.6
 */
@PerformanceSensitive("allocation")
public class ReusableObjectMessage implements ReusableMessage {
    private static final long serialVersionUID = 6922476812535519960L;

    private transient Object obj;
    private transient String objectString;

    public void set(Object object) {
        this.obj = object;
    }

    /**
     * Returns the formatted object message.
     *
     * @return the formatted object message.
     */
    @Override
    public String getFormattedMessage() {
        return String.valueOf(obj);
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        if (obj == null || obj instanceof String) {
            buffer.append((String) obj);
        } else if (obj instanceof StringBuilderFormattable) {
            ((StringBuilderFormattable) obj).formatTo(buffer);
        } else if (obj instanceof CharSequence) {
            buffer.append((CharSequence) obj);
        } else {
            buffer.append(obj);
        }
    }

    @Override
    public CharSequence getFormattedCharSequence() {
        if (obj instanceof CharSequence) {
            return (CharSequence) obj;
        } else {
            return getFormattedMessage();
        }
    }

    /**
     * Returns the object formatted using its toString method.
     *
     * @return the String representation of the object.
     */
    @Override
    public String getFormat() {
        return getFormattedMessage();
    }

    /**
     * Returns the object as if it were a parameter.
     *
     * @return The object.
     */
    @Override
    public Object[] getParameters() {
        return new Object[] {obj};
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }

    /**
     * Gets the message if it is a throwable.
     *
     * @return the message if it is a throwable.
     */
    @Override
    public Throwable getThrowable() {
        return obj instanceof Throwable ? (Throwable) obj : null;
    }
}
