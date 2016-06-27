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

import java.util.Locale;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiRenderer;
import org.fusesource.jansi.AnsiRenderer.Code;

/**
 * Uses the JAnsi rendering syntax to render a message into an ANSI escaped string.
 * 
 * The syntax for embedded ANSI codes is:
 *
 * <pre>
 *   &#64;|<em>code</em>(,<em>code</em>)* <em>text</em>|@
 * </pre>
 *
 * Examples:
 *
 * <pre>
 *   &#64;|bold Hello|@
 * </pre>
 *
 * <pre>
 *   &#64;|bold,red Warning!|@
 * </pre>
 * 
 * Note: This class copied and tweaked code from JAnsi's AnsiRenderer (which is licensed as Apache 2.0.) If JAnsi makes
 * AnsiRenderer.render(String, String...) public, we can get rid of our copy. See
 * <a href=" https://github.com/fusesource/jansi/pull/61">PR #61</a>.
 * 
 * @see AnsiRenderer
 */
public final class JAnsiMessageRenderer implements MessageRenderer {

    private static final int BEGIN_TOKEN_LEN = AnsiRenderer.BEGIN_TOKEN.length();
    private static final int END_TOKEN_LEN = AnsiRenderer.END_TOKEN.length();

    private String render(final String text, final String... codes) {
        Ansi ansi = Ansi.ansi();
        for (final String name : codes) {
            final Code code = Code.valueOf(name.toUpperCase(Locale.ENGLISH));

            if (code.isColor()) {
                if (code.isBackground()) {
                    ansi = ansi.bg(code.getColor());
                } else {
                    ansi = ansi.fg(code.getColor());
                }
            } else if (code.isAttribute()) {
                ansi = ansi.a(code.getAttribute());
            }
        }

        return ansi.a(text).reset().toString();
    }

    @Override
    public void render(final StringBuilder input, final StringBuilder target) throws IllegalArgumentException {
        int i = 0;
        int j, k;

        while (true) {
            j = input.indexOf(AnsiRenderer.BEGIN_TOKEN, i);
            if (j == -1) {
                if (i == 0) {
                    target.append(input);
                    return;
                }
                target.append(input.substring(i, input.length()));
                return;
            }
            target.append(input.substring(i, j));
            k = input.indexOf(AnsiRenderer.END_TOKEN, j);

            if (k == -1) {
                target.append(input);
                return;
            }
            j += BEGIN_TOKEN_LEN;
            final String spec = input.substring(j, k);

            final String[] items = spec.split(AnsiRenderer.CODE_TEXT_SEPARATOR, 2);
            if (items.length == 1) {
                target.append(input);
                return;
            }
            final String replacement = render(items[1], items[0].split(AnsiRenderer.CODE_LIST_SEPARATOR));

            target.append(replacement);

            i = k + END_TOKEN_LEN;
        }
    }

}
