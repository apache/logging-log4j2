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
package org.apache.logging.log4j.core.pattern;

/**
 *
 */
public enum AnsiEscape {

    PREFIX("\u001b["),
    SUFFIX("m"),
    SEPARATOR(";"),

    NORMAL("0"),
    BRIGHT("1"),
    DIM("2"),
    UNDERLINE("3"),
    BLINK("5"),
    REVERSE("7"),
    HIDDEN("8"),

    BLACK("30"),
    FG_BLACK("30"),
    RED("31"),
    FG_RED("31"),
    GREEN("32"),
    FG_GREEN("32"),
    YELLOW("33"),
    FG_YELLOW("33"),
    BLUE("34"),
    FG_BLUE("34"),
    MAGENTA("35"),
    FG_MAGENTA("35"),
    CYAN("36"),
    FG_CYAN("36"),
    WHITE("37"),
    FG_WHITE("37"),
    DEFAULT("39"),
    FG_DEFAULT("39"),

    BG_BLACK("40"),
    BG_RED("41"),
    BG_GREEN("42"),
    BG_YELLOW("43"),
    BG_BLUE("44"),
    BG_MAGENTA("45"),
    BG_CYAN("46"),
    BG_WHITE("47");

    public static String getDefaultStyle() {
        return PREFIX.getCode() + SUFFIX.getCode();
    }

    private final String code;

    private AnsiEscape(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static String createSequence(String[] values) {
        if (values == null) {
            return getDefaultStyle();
        }
        StringBuilder sb = new StringBuilder(AnsiEscape.PREFIX.getCode());
        boolean first = true;
        for (String value : values) {
            try {
                AnsiEscape escape = AnsiEscape.valueOf(value.trim().toUpperCase());
                if (!first) {
                    sb.append(AnsiEscape.SEPARATOR.getCode());
                }
                first = false;
                sb.append(escape.getCode());
            } catch (Exception ex) {
                // Ignore the error.
            }
        }
        sb.append(AnsiEscape.SUFFIX.getCode());
        return sb.toString();
    }

}
