package org.apache.logging.log4j.core.impl;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContextTest;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.logging.log4j.ThreadContext.getThreadContextMap;
import static org.apache.logging.log4j.core.impl.ContextDataInjectorFactory.createInjector;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(Parameterized.class)
public class ThreadContextDataInjectorTest {
    @Parameters(name = "{0}")
    public static Collection<String[]> threadContextMapClassNames() {
        return asList(new String[][] {
                { "org.apache.logging.log4j.spi.CopyOnWriteSortedArrayThreadContextMap", "org.apache.logging.log4j.spi.CopyOnWriteSortedArrayThreadContextMap" },
                { "org.apache.logging.log4j.spi.GarbageFreeSortedArrayThreadContextMap", "org.apache.logging.log4j.spi.GarbageFreeSortedArrayThreadContextMap" },
                { "org.apache.logging.log4j.spi.DefaultThreadContextMap", null }
        });
    }

    @Parameter
    public String threadContextMapClassName;

    @Parameter(value = 1)
    public String readOnlythreadContextMapClassName;

    @Before
    public void before() {
        System.setProperty("log4j2.threadContextMap", threadContextMapClassName);
    }

    @After
    public void after() {
        ThreadContext.remove("foo");
        ThreadContext.remove("baz");
        System.clearProperty("log4j2.threadContextMap");
        System.clearProperty("log4j2.isThreadContextMapInheritable");
    }

    private void testContextDataInjector() {
        ReadOnlyThreadContextMap readOnlythreadContextMap = getThreadContextMap();
        assertThat("thread context map class name",
                   (readOnlythreadContextMap == null) ? null : readOnlythreadContextMap.getClass().getName(),
                   is(equalTo(readOnlythreadContextMapClassName)));

        ContextDataInjector contextDataInjector = createInjector();
        StringMap stringMap = contextDataInjector.injectContextData(null, new SortedArrayStringMap());

        assertThat("thread context map", ThreadContext.getContext(), allOf(hasEntry("foo", "bar"), not(hasKey("baz"))));
        assertThat("context map", stringMap.toMap(), allOf(hasEntry("foo", "bar"), not(hasKey("baz"))));

        if (!stringMap.isFrozen()) {
            stringMap.clear();
            assertThat("thread context map", ThreadContext.getContext(), allOf(hasEntry("foo", "bar"), not(hasKey("baz"))));
            assertThat("context map", stringMap.toMap().entrySet(), is(empty()));
        }

        ThreadContext.put("foo", "bum");
        ThreadContext.put("baz", "bam");

        assertThat("thread context map", ThreadContext.getContext(), allOf(hasEntry("foo", "bum"), hasEntry("baz", "bam")));
        if (stringMap.isFrozen()) {
            assertThat("context map", stringMap.toMap(), allOf(hasEntry("foo", "bar"), not(hasKey("baz"))));
        } else {
            assertThat("context map", stringMap.toMap().entrySet(), is(empty()));
        }
    }

    private void prepareThreadContext(boolean isThreadContextMapInheritable) {
        System.setProperty("log4j2.isThreadContextMapInheritable", Boolean.toString(isThreadContextMapInheritable));
        PropertiesUtil.getProperties().reload();
        ThreadContextTest.reinitThreadContext();
        ThreadContext.remove("baz");
        ThreadContext.put("foo", "bar");
    }

    @Test
    public void testThreadContextImmutability() {
        prepareThreadContext(false);
        testContextDataInjector();
    }

    @Test
    public void testInheritableThreadContextImmutability() throws Throwable {
        prepareThreadContext(true);
        try {
            newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    testContextDataInjector();
                }
            }).get();
        } catch (ExecutionException ee) {
            throw ee.getCause();
        }
    }
}
