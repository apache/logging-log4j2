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
package org.apache.logging.log4j;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.message.FormattedMessage;
import org.apache.logging.log4j.message.JsonMessage;
import org.apache.logging.log4j.message.LocalizedMessage;
import org.apache.logging.log4j.message.MessageFormatMessage;
import org.apache.logging.log4j.message.ObjectArrayMessage;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.apache.logging.log4j.message.ThreadDumpMessage;
import org.apache.logging.log4j.util.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests Logger APIs with {@link Supplier}.
 */
public class LoggerSupplierTest {

    private final TestLogger logger = (TestLogger) LogManager.getLogger("LoggerTest");

    private final List<String> results = logger.getEntries();

    Locale defaultLocale;

    @Test
    public void flowTracing_SupplierOfFormattedMessage() {
        logger.traceEntry(new Supplier<FormattedMessage>() {
            @Override
            public FormattedMessage get() {
                return new FormattedMessage("int foo={}", 1234567890);
            }
        });
        assertEquals(1, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTER[ FLOW ] TRACE Enter"));
        assertThat("Missing entry data", results.get(0), containsString("(int foo=1234567890)"));
        assertThat("Bad toString()", results.get(0), not(containsString("FormattedMessage")));
    }

    @Test
    public void flowTracing_SupplierOfJsonMessage() {
        logger.traceEntry(new Supplier<JsonMessage>() {
            @Override
            public JsonMessage get() {
                return new JsonMessage(System.getProperties());
            }
        });
        assertEquals(1, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTER[ FLOW ] TRACE Enter"));
        assertThat("Missing entry data", results.get(0), containsString("\"java.runtime.name\":"));
        assertThat("Bad toString()", results.get(0), not(containsString("JsonMessage")));
    }

    @Test
    public void flowTracing_SupplierOfLocalizedMessage() {
        logger.traceEntry(new Supplier<LocalizedMessage>() {
            @Override
            public LocalizedMessage get() {
                return new LocalizedMessage("int foo={}", 1234567890);
            }
        });
        assertEquals(1, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTER[ FLOW ] TRACE Enter"));
        assertThat("Missing entry data", results.get(0), containsString("(int foo=1234567890)"));
        assertThat("Bad toString()", results.get(0), not(containsString("LocalizedMessage")));
    }

    @Test
    public void flowTracing_SupplierOfLong() {
        logger.traceEntry(new Supplier<Long>() {
            @Override
            public Long get() {
                return Long.valueOf(1234567890);
            }
        });
        assertEquals(1, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTER[ FLOW ] TRACE Enter"));
        assertThat("Missing entry data", results.get(0), containsString("(1234567890)"));
        assertThat("Bad toString()", results.get(0), not(containsString("SimpleMessage")));
    }

    @Test
    public void flowTracing_SupplierOfMessageFormatMessage() {
        logger.traceEntry(new Supplier<MessageFormatMessage>() {
            @Override
            public MessageFormatMessage get() {
                return new MessageFormatMessage("int foo={0}", 1234567890);
            }
        });
        assertEquals(1, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTER[ FLOW ] TRACE Enter"));
        assertThat("Missing entry data", results.get(0), containsString("(int foo=1,234,567,890)"));
        assertThat("Bad toString()", results.get(0), not(containsString("MessageFormatMessage")));
    }

    @Test
    public void flowTracing_SupplierOfObjectArrayMessage() {
        logger.traceEntry(new Supplier<ObjectArrayMessage>() {
            @Override
            public ObjectArrayMessage get() {
                return new ObjectArrayMessage(1234567890);
            }
        });
        assertEquals(1, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTER[ FLOW ] TRACE Enter"));
        assertThat("Missing Enter data", results.get(0), containsString("([1234567890])"));
        assertThat("Bad toString()", results.get(0), not(containsString("ObjectArrayMessage")));
    }

    @Test
    public void flowTracing_SupplierOfObjectMessage() {
        logger.traceEntry(new Supplier<ObjectMessage>() {
            @Override
            public ObjectMessage get() {
                return new ObjectMessage(1234567890);
            }
        });
        assertEquals(1, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTER[ FLOW ] TRACE Enter"));
        assertThat("Missing entry data", results.get(0), containsString("(1234567890)"));
        assertThat("Bad toString()", results.get(0), not(containsString("ObjectMessage")));
    }

    @Test
    public void flowTracing_SupplierOfParameterizedMessage() {
        logger.traceEntry(new Supplier<ParameterizedMessage>() {
            @Override
            public ParameterizedMessage get() {
                return new ParameterizedMessage("int foo={}", 1234567890);
            }
        });
        assertEquals(1, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTER[ FLOW ] TRACE Enter"));
        assertThat("Missing entry data", results.get(0), containsString("(int foo=1234567890)"));
        assertThat("Bad toString()", results.get(0), not(containsString("ParameterizedMessage")));
    }

    @Test
    public void flowTracing_SupplierOfSimpleMessage() {
        logger.traceEntry(new Supplier<SimpleMessage>() {
            @Override
            public SimpleMessage get() {
                return new SimpleMessage("1234567890");
            }
        });
        assertEquals(1, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTER[ FLOW ] TRACE Enter"));
        assertThat("Missing entry data", results.get(0), containsString("(1234567890)"));
        assertThat("Bad toString()", results.get(0), not(containsString("SimpleMessage")));
    }

    @Test
    public void flowTracing_SupplierOfString() {
        logger.traceEntry(new Supplier<String>() {
            @Override
            public String get() {
                return "1234567890";
            }
        });
        assertEquals(1, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTER[ FLOW ] TRACE Enter"));
        assertThat("Missing entry data", results.get(0), containsString("(1234567890)"));
        assertThat("Bad toString()", results.get(0), not(containsString("SimpleMessage")));
    }

    @Test
    public void flowTracing_SupplierOfStringFormattedMessage() {
        logger.traceEntry(new Supplier<StringFormattedMessage>() {
            @Override
            public StringFormattedMessage get() {
                return new StringFormattedMessage("int foo=%,d", 1234567890);
            }
        });
        assertEquals(1, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTER[ FLOW ] TRACE Enter"));
        assertThat("Missing entry data", results.get(0), containsString("(int foo=1,234,567,890)"));
        assertThat("Bad toString()", results.get(0), not(containsString("StringFormattedMessage")));
    }

    @Test
    public void flowTracing_SupplierOfThreadDumpMessage() {
        logger.traceEntry(new Supplier<ThreadDumpMessage>() {
            @Override
            public ThreadDumpMessage get() {
                return new ThreadDumpMessage("Title of ...");
            }
        });
        assertEquals(1, results.size());
        assertThat("Incorrect Entry", results.get(0), startsWith("ENTER[ FLOW ] TRACE Enter"));
        assertThat("Missing entry data", results.get(0), containsString("RUNNABLE"));
        assertThat("Missing entry data", results.get(0), containsString("Title of ..."));
        assertThat("Missing entry data", results.get(0), containsString(getClass().getName()));
    }
    
    @Before
    public void setup() {
        results.clear();
        defaultLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, java.util.Locale.US);
    }
    
    @After
    public void tearDown() {
        Locale.setDefault(Locale.Category.FORMAT, defaultLocale);
    }

}
