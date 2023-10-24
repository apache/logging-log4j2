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

import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * Mutable Message wrapper around an Object message.
 * @since 2.6
 */
@PerformanceSensitive("allocation")
public class ReusableObjectMessage implements ReusableMessage, ParameterVisitable, Clearable {
    private static final long serialVersionUID = 6922476812535519960L;

    private transient Object obj;

    public void set(final Object object) {
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
        StringBuilders.appendValue(buffer, obj);
    }

    /**
     * Returns the object formatted using its toString method.
     *
     * @return the String representation of the object.
     */
    @Override
    public String getFormat() {
        return obj instanceof String ? (String) obj : null;
    }

    /**
     * Returns the object parameter.
     *
     * @return The object.
     * @since 2.7
     */
    public Object getParameter() {
        return obj;
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

    /**
     * This message has exactly one parameter (the object), so returns it as the first parameter in the array.
     * @param emptyReplacement the parameter array to return
     * @return the specified array
     */
    @Override
    public Object[] swapParameters(final Object[] emptyReplacement) {
        // it's unlikely that emptyReplacement is of length 0, but if it is,
        // go ahead and allocate the memory now;
        // this saves an allocation in the future when this buffer is re-used
        if (emptyReplacement.length == 0) {
            final Object[] params = new Object[10]; // Default reusable parameter buffer size
            params[0] = obj;
            return params;
        }
        emptyReplacement[0] = obj;
        return emptyReplacement;
    }

    /**
     * This message has exactly one parameter (the object), so always returns one.
     * @return 1
     */
    @Override
    public short getParameterCount() {
        return 1;
    }

    @Override
    public <S> void forEachParameter(final ParameterConsumer<S> action, final S state) {
        action.accept(obj, 0, state);
    }

    @Override
    public Message memento() {
        return new ObjectMessage(obj);
    }

    @Override
    public void clear() {
        obj = null;
    }

    private Object writeReplace() {
        return memento();
    }
}
