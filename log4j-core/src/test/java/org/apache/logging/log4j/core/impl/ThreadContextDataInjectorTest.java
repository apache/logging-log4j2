package org.apache.logging.log4j.core.impl;

import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.logging.log4j.ThreadContext.getThreadContextMap;
import static org.apache.logging.log4j.core.impl.ContextDataInjectorFactory.createInjector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContextTest;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.assertj.core.api.HamcrestCondition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

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
        assertThat((readOnlythreadContextMap == null) ? null : readOnlythreadContextMap.getClass().getName()).describedAs("thread context map class name").isEqualTo(readOnlythreadContextMapClassName);

        ContextDataInjector contextDataInjector = createInjector();
        StringMap stringMap = contextDataInjector.injectContextData(null, new SortedArrayStringMap());

        assertThat(ThreadContext.getContext()).describedAs("thread context map").is(new HamcrestCondition<>(allOf(hasEntry("foo", "bar"), not(hasKey("baz")))));
        assertThat(stringMap.toMap()).describedAs("context map").is(new HamcrestCondition<>(allOf(hasEntry("foo", "bar"), not(hasKey("baz")))));

        if (!stringMap.isFrozen()) {
            stringMap.clear();
            assertThat(ThreadContext.getContext()).describedAs("thread context map").is(new HamcrestCondition<>(allOf(hasEntry("foo", "bar"), not(hasKey("baz")))));
            assertThat(stringMap.toMap().entrySet()).describedAs("context map").isEmpty();
        }

        ThreadContext.put("foo", "bum");
        ThreadContext.put("baz", "bam");

        assertThat(ThreadContext.getContext()).describedAs("thread context map").is(new HamcrestCondition<>(allOf(hasEntry("foo", "bum"), hasEntry("baz", "bam"))));
        if (stringMap.isFrozen()) {
            assertThat(stringMap.toMap()).describedAs("context map").is(new HamcrestCondition<>(allOf(hasEntry("foo", "bar"), not(hasKey("baz")))));
        } else {
            assertThat(stringMap.toMap().entrySet()).describedAs("context map").isEmpty();
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
