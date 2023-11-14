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
package org.apache.log4j.varia;

import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Simple filter based on string matching.
 *
 * <p>
 * The filter admits two options <b>StringToMatch</b> and <b>AcceptOnMatch</b>. If there is a match between the value of
 * the StringToMatch option and the message of the {@link org.apache.log4j.spi.LoggingEvent}, then the
 * {@link #decide(LoggingEvent)} method returns {@link org.apache.log4j.spi.Filter#ACCEPT} if the <b>AcceptOnMatch</b>
 * option value is true, if it is false then {@link org.apache.log4j.spi.Filter#DENY} is returned. If there is no match,
 * {@link org.apache.log4j.spi.Filter#NEUTRAL} is returned.
 * </p>
 *
 * @since 0.9.0
 */
public class StringMatchFilter extends Filter {

    /**
     * @deprecated Options are now handled using the JavaBeans paradigm. This constant is not longer needed and will be
     *             removed in the <em>near</em> term.
     */
    @Deprecated
    public static final String STRING_TO_MATCH_OPTION = "StringToMatch";

    /**
     * @deprecated Options are now handled using the JavaBeans paradigm. This constant is not longer needed and will be
     *             removed in the <em>near</em> term.
     */
    @Deprecated
    public static final String ACCEPT_ON_MATCH_OPTION = "AcceptOnMatch";

    boolean acceptOnMatch = true;
    String stringToMatch;

    /**
     * Returns {@link Filter#NEUTRAL} is there is no string match.
     */
    @Override
    public int decide(final LoggingEvent event) {
        final String msg = event.getRenderedMessage();
        if (msg == null || stringToMatch == null) {
            return Filter.NEUTRAL;
        }
        if (msg.indexOf(stringToMatch) == -1) {
            return Filter.NEUTRAL;
        }
        return acceptOnMatch ? Filter.ACCEPT : Filter.DENY;
    }

    public boolean getAcceptOnMatch() {
        return acceptOnMatch;
    }

    /**
     * @deprecated We now use JavaBeans introspection to configure components. Options strings are no longer needed.
     */
    @Deprecated
    public String[] getOptionStrings() {
        return new String[] {STRING_TO_MATCH_OPTION, ACCEPT_ON_MATCH_OPTION};
    }

    public String getStringToMatch() {
        return stringToMatch;
    }

    public void setAcceptOnMatch(final boolean acceptOnMatch) {
        this.acceptOnMatch = acceptOnMatch;
    }

    /**
     * @deprecated Use the setter method for the option directly instead of the generic <code>setOption</code> method.
     */
    @Deprecated
    public void setOption(final String key, final String value) {
        if (key.equalsIgnoreCase(STRING_TO_MATCH_OPTION)) {
            stringToMatch = value;
        } else if (key.equalsIgnoreCase(ACCEPT_ON_MATCH_OPTION)) {
            acceptOnMatch = OptionConverter.toBoolean(value, acceptOnMatch);
        }
    }

    public void setStringToMatch(final String s) {
        stringToMatch = s;
    }
}
