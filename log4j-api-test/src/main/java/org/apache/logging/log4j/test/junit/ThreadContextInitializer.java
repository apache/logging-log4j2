package org.apache.logging.log4j.test.junit;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContextUtilityClass;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

class ThreadContextInitializer implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        ThreadContextUtilityClass.reset();
        // We use `CloseableResource` instead of `afterAll` to reset the ThreadContextFactory
        // *after* the `@SetSystemProperty` extension has restored the properties
        ExtensionContextAnchor.setAttribute(ThreadContext.class, new CloseableResource() {
            @Override
            public void close() throws Throwable {
                ThreadContextUtilityClass.reset();
            }

        }, context);
    }

}
