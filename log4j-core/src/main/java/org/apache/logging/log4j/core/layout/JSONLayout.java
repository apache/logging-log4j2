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
package org.apache.logging.log4j.core.layout;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.Charsets;
import org.apache.logging.log4j.core.helpers.Throwables;
import org.apache.logging.log4j.core.helpers.Transform;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MultiformatMessage;

/**
 * Appends a series of JSON events as strings serialized as bytes.
 * 
 * <h4>Complete well-formed JSON vs. fragment JSON</h4>
 * <p>
 * If you configure {@code complete="true"}, the appender outputs a well-formed JSON document. 
 * By default, with {@code complete="false"}, you should include the
 * output as an <em>external file</em> in a separate file to form a well-formed JSON document.
 * </p>
 * <p>
 * A well-formed JSON document follows this pattern:
 * </p>
 * 
 * <pre>[
 *   {
 *     "logger":"com.foo.Bar",
 *     "timestamp":"1376681196470",
 *     "level":"INFO",
 *     "thread":"main",
 *     "message":"Message flushed with immediate flush=true"
 *   },
 *   {
 *     "logger":"com.foo.Bar",
 *     "timestamp":"1376681196471",
 *     "level":"ERROR",
 *     "thread":"main",
 *     "message":"Message flushed with immediate flush=true",
 *     "throwable":"java.lang.IllegalArgumentException: badarg\\n\\tat org.apache.logging.log4j.core.appender.JSONCompleteFileAppenderTest.testFlushAtEndOfBatch(JSONCompleteFileAppenderTest.java:54)\\n\\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\\n\\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)\\n\\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\\n\\tat java.lang.reflect.Method.invoke(Method.java:606)\\n\\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:47)\\n\\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)\\n\\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:44)\\n\\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)\\n\\tat org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:271)\\n\\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:70)\\n\\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:50)\\n\\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:238)\\n\\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:63)\\n\\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:236)\\n\\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:53)\\n\\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:229)\\n\\tat org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)\\n\\tat org.junit.runners.ParentRunner.run(ParentRunner.java:309)\\n\\tat org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:50)\\n\\tat org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:38)\\n\\tat org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:467)\\n\\tat org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:683)\\n\\tat org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:390)\\n\\tat org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:197)\\n"
 *   }
 * ]</pre>
 * <p>
 * If {@code complete="false"}, the appender does not write the JSON open array character "[" at the start of the document.
 * and "]" and the end.
 * </p>
 * <p>
 * This approach enforces the independence of the JSONLayout and the appender where you embed it.
 * </p>
 * <h4>Encoding</h4>
 * <p>
 * Appenders using this layout should have their {@code charset} set to {@code UTF-8} or {@code UTF-16}, otherwise
 * events containing non ASCII characters could result in corrupted log files.
 * </p>
 * <h4>Pretty vs. compact XML</h4>
 * <p>
 * By default, the JSON layout is not compact (a.k.a. not "pretty") with {@code compact="false"}, which means the
 * appender uses end-of-line characters and indents lines to format the text. If {@code compact="true"}, then no
 * end-of-line or indentation is used. Message content may contain, of course, escaped end-of-lines.
 * </p>
 */
@Plugin(name = "JSONLayout", category = "Core", elementType = "layout", printObject = true)
public class JSONLayout extends AbstractStringLayout {

    private static final int DEFAULT_SIZE = 256;

    // We yield to \r\n for the default.
    private static final String DEFAULT_EOL = "\r\n";
    private static final String COMPACT_EOL = "";
    private static final String DEFAULT_INDENT = "  ";
    private static final String COMPACT_INDENT = "";

    private static final String[] FORMATS = new String[] { "json" };

    private final boolean locationInfo;
    private final boolean properties;
    private final boolean complete;
    private final String eol;
    private final String indent1;
    private final String indent2;
    private final String indent3;
    private final String indent4;
    private volatile boolean firstLayoutDone;

    protected JSONLayout(final boolean locationInfo, final boolean properties, final boolean complete, boolean compact,
            final Charset charset) {
        super(charset);
        this.locationInfo = locationInfo;
        this.properties = properties;
        this.complete = complete;
        this.eol = compact ? COMPACT_EOL : DEFAULT_EOL;
        this.indent1 = compact ? COMPACT_INDENT : DEFAULT_INDENT;
        this.indent2 = this.indent1 + this.indent1;
        this.indent3 = this.indent2 + this.indent1;
        this.indent4 = this.indent3 + this.indent1;
    }

    /**
     * Formats a {@link org.apache.logging.log4j.core.LogEvent} in conformance with the log4j.dtd.
     * 
     * @param event
     *            The LogEvent.
     * @return The XML representation of the LogEvent.
     */
    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder buf = new StringBuilder(DEFAULT_SIZE);
        // DC locking to avoid synchronizing the whole layout.
        boolean check = this.firstLayoutDone; 
        if (!this.firstLayoutDone) {
            synchronized(this) {
                check = this.firstLayoutDone;
                if (!check) {
                    this.firstLayoutDone = true;
                } else {
                    buf.append(',');
                    buf.append(this.eol);                                
                }
            }
        } else {
            buf.append(',');
            buf.append(this.eol);                                            
        }
        buf.append(this.indent1);
        buf.append('{');
        buf.append(this.eol);
        buf.append(this.indent2);
        buf.append("\"logger\":\"");
        String name = event.getLoggerName();
        if (name.isEmpty()) {
            name = "root";
        }
        buf.append(Transform.escapeJsonControlCharacters(name));
        buf.append("\",");
        buf.append(this.eol);
        buf.append(this.indent2);
        buf.append("\"timestamp\":\"");
        buf.append(event.getMillis());
        buf.append("\",");
        buf.append(this.eol);
        buf.append(this.indent2);
        buf.append("\"level\":\"");
        buf.append(Transform.escapeJsonControlCharacters(String.valueOf(event.getLevel())));
        buf.append("\",");
        buf.append(this.eol);
        buf.append(this.indent2);
        buf.append("\"thread\":\"");
        buf.append(Transform.escapeJsonControlCharacters(event.getThreadName()));
        buf.append("\",");
        buf.append(this.eol);

        final Message msg = event.getMessage();
        if (msg != null) {
            boolean jsonSupported = false;
            if (msg instanceof MultiformatMessage) {
                final String[] formats = ((MultiformatMessage) msg).getFormats();
                for (final String format : formats) {
                    if (format.equalsIgnoreCase("JSON")) {
                        jsonSupported = true;
                        break;
                    }
                }
            }
            buf.append(this.indent2);
            buf.append("\"message\":\"");
            if (jsonSupported) {
                buf.append(((MultiformatMessage) msg).getFormattedMessage(FORMATS));
            } else {
                buf.append(Transform.escapeJsonControlCharacters(event.getMessage().getFormattedMessage()));
            }
            buf.append('\"');
        }

        if (event.getContextStack().getDepth() > 0) {
            buf.append(",");
            buf.append(this.eol);
            buf.append("\"ndc\":");
            buf.append(Transform.escapeJsonControlCharacters(event.getContextStack().toString()));
            buf.append("\"");
        }

        final Throwable throwable = event.getThrown();
        if (throwable != null) {
            buf.append(",");
            buf.append(this.eol);
            buf.append(this.indent2);
            buf.append("\"throwable\":\"");
            final List<String> list = Throwables.toStringList(throwable);
            for (final String str : list) {
                buf.append(Transform.escapeJsonControlCharacters(str));
                buf.append("\\\\n");
            }
            buf.append("\"");
        }

        if (this.locationInfo) {
            final StackTraceElement element = event.getSource();
            buf.append(",");
            buf.append(this.eol);
            buf.append(this.indent2);
            buf.append("\"LocationInfo\":{");
            buf.append(this.eol);
            buf.append(this.indent3);
            buf.append("\"class\":\"");
            buf.append(Transform.escapeJsonControlCharacters(element.getClassName()));
            buf.append("\",");
            buf.append(this.eol);
            buf.append(this.indent3);
            buf.append("\"method\":\"");
            buf.append(Transform.escapeJsonControlCharacters(element.getMethodName()));
            buf.append("\",");
            buf.append(this.eol);
            buf.append(this.indent3);
            buf.append("\"file\":\"");
            buf.append(Transform.escapeJsonControlCharacters(element.getFileName()));
            buf.append("\",");
            buf.append(this.eol);
            buf.append(this.indent3);
            buf.append("\"line\":\"");
            buf.append(element.getLineNumber());
            buf.append("\"");
            buf.append(this.eol);
            buf.append(this.indent2);
            buf.append("}");
        }

        if (this.properties && event.getContextMap().size() > 0) {
            buf.append(",");
            buf.append(this.eol);
            buf.append(this.indent2);
            buf.append("\"Properties\":[");
            buf.append(this.eol);
            final Set<Entry<String, String>> entrySet = event.getContextMap().entrySet();
            int i = 1;
            for (final Map.Entry<String, String> entry : entrySet) {
                buf.append(this.indent3);
                buf.append('{');
                buf.append(this.eol);
                buf.append(this.indent4);
                buf.append("\"name\":\"");
                buf.append(Transform.escapeJsonControlCharacters(entry.getKey()));
                buf.append("\",");
                buf.append(this.eol);
                buf.append(this.indent4);
                buf.append("\"value\":\"");
                buf.append(Transform.escapeJsonControlCharacters(String.valueOf(entry.getValue())));
                buf.append("\"");
                buf.append(this.eol);
                buf.append(this.indent3);
                buf.append("}");
                if (i < entrySet.size()) {
                    buf.append(",");
                }
                buf.append(this.eol);
                i++;
            }
            buf.append(this.indent2);
            buf.append("]");
        }

        buf.append(this.eol);
        buf.append(this.indent1);
        buf.append("}");

        return buf.toString();
    }

    /**
     * Returns appropriate JSON headers.
     * 
     * @return a byte array containing the header, opening the JSON array.
     */
    @Override
    public byte[] getHeader() {
        if (!this.complete) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append('[');
        buf.append(this.eol);
        return buf.toString().getBytes(this.getCharset());
    }

    /**
     * Returns appropriate JSON footer.
     * 
     * @return a byte array containing the footer, closing the JSON array.
     */
    @Override
    public byte[] getFooter() {
        if (!this.complete) {
            return null;
        }
        return (this.eol + "]" + this.eol).getBytes(this.getCharset());
    }

    /**
     * XMLLayout's content format is specified by:
     * <p/>
     * Key: "dtd" Value: "log4j-events.dtd"
     * <p/>
     * Key: "version" Value: "2.0"
     * 
     * @return Map of content format keys supporting XMLLayout
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<String, String>();
        result.put("version", "2.0");
        return result;
    }

    @Override
    /**
     * @return The content type.
     */
    public String getContentType() {
        return "application/json; charset=" + this.getCharset();
    }

    /**
     * Creates an XML Layout.
     * 
     * @param locationInfo
     *            If "true", includes the location information in the generated JSON.
     * @param properties
     *            If "true", includes the thread context in the generated JSON.
     * @param completeStr
     *            If "true", includes the JSON header and footer, defaults to "false".
     * @param compactStr
     *            If "true", does not use end-of-lines and indentation, defaults to "false".
     * @param charsetName
     *            The character set to use, if {@code null}, uses "UTF-8".
     * @return An XML Layout.
     */
    @PluginFactory
    public static JSONLayout createLayout(
            @PluginAttribute("locationInfo") final String locationInfo,
            @PluginAttribute("properties") final String properties, 
            @PluginAttribute("complete") final String completeStr,
            @PluginAttribute("compact") final String compactStr, 
            @PluginAttribute("charset") final String charsetName) {
        final Charset charset = Charsets.getSupportedCharset(charsetName, Charsets.UTF_8);
        final boolean info = Boolean.parseBoolean(locationInfo);
        final boolean props = Boolean.parseBoolean(properties);
        final boolean complete = Boolean.parseBoolean(completeStr);
        final boolean compact = Boolean.parseBoolean(compactStr);
        return new JSONLayout(info, props, complete, compact, charset);
    }
}
