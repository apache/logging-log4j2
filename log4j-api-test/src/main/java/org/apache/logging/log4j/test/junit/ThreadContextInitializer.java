package org.apache.logging.log4j.test.junit;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.test.ThreadContextUtilityClass;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.platform.commons.support.AnnotationSupport;

class ThreadContextInitializer implements BeforeAllCallback, BeforeEachCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (AnnotationSupport.isAnnotated(context.getRequiredTestClass(), InitializesThreadContext.class)) {
            resetThreadContext(context);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (AnnotationSupport.isAnnotated(context.getRequiredTestMethod(), InitializesThreadContext.class)) {
            resetThreadContext(context);
        }
    }

    private void resetThreadContext(ExtensionContext context) {
        ThreadContextUtilityClass.reset();
        // We use `CloseableResource` instead of `afterAll` to reset the
        // ThreadContextFactory
        // *after* the `@SetSystemProperty` extension has restored the properties
        ExtensionContextAnchor.setAttribute(ThreadContext.class, new CloseableResource() {
            @Override
            public void close() throws Throwable {
                ThreadContextUtilityClass.reset();
            }

        }, context);
    }

}
