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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import org.apache.logging.log4j.util.Constants;

/**
 * Handles messages that contain an Object[].
 * <p>
 * Created for use with the CSV layout. For example:
 * </p>
 * <p>
 * {@code logger.debug(new ObjectArrayMessage(1, 2, "Bob"));}
 * </p>
 *
 * @since 2.4
 */
public final class ObjectArrayMessage implements Message {

    private static final long serialVersionUID = -5903272448334166185L;

    private transient Object[] array;
    private transient String arrayString;

    /**
     * Creates the ObjectMessage.
     *
     * @param obj
     *            The Object to format.
     */
    public ObjectArrayMessage(final Object... obj) {
        this.array = obj == null ? Constants.EMPTY_OBJECT_ARRAY : obj;
    }

    private boolean equalObjectsOrStrings(final Object[] left, final Object[] right) {
        return Arrays.equals(left, right) || Arrays.toString(left).equals(Arrays.toString(right));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ObjectArrayMessage that = (ObjectArrayMessage) o;
        return array == null ? that.array == null : equalObjectsOrStrings(array, that.array);
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
     * Returns the formatted object message.
     *
     * @return the formatted object message.
     */
    @Override
    public String getFormattedMessage() {
        // LOG4J2-763: cache formatted string in case obj changes later
        if (arrayString == null) {
            arrayString = Arrays.toString(array);
        }
        return arrayString;
    }

    /**
     * Returns the object as if it were a parameter.
     *
     * @return The object.
     */
    @Override
    public Object[] getParameters() {
        return array;
    }

    /**
     * Returns null.
     *
     * @return null.
     */
    @Override
    public Throwable getThrowable() {
        return null;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(array);
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        array = (Object[]) in.readObject();
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(array);
    }
}
