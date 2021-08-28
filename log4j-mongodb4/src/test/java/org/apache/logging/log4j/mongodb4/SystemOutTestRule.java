package org.apache.logging.log4j.mongodb4;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class SystemOutTestRule implements TestRule {

    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final PrintStream printStream = System.out;

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                System.setOut(new PrintStream(byteArrayOutputStream));
                base.evaluate();
                System.setOut(printStream);
            }
        };
    }

    @Override
    public String toString() {
        return byteArrayOutputStream.toString();
    }
}