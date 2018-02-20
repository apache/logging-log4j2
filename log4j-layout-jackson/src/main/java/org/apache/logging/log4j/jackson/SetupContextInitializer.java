package org.apache.logging.log4j.jackson;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ExtendedStackTraceElement;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.time.Instant;

import com.fasterxml.jackson.databind.Module.SetupContext;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Used to set up {@link SetupContext} from different {@link SimpleModule}s.
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 */
public class SetupContextInitializer {

    public void setupModule(final SetupContext context, final boolean includeStacktrace,
            final boolean stacktraceAsString) {
        // JRE classes: we cannot edit those with Jackson annotations
        context.setMixInAnnotations(StackTraceElement.class, StackTraceElementMixIn.class);
        // Log4j API classes: we do not want to edit those with Jackson annotations because the API module should not
        // depend on Jackson.
        context.setMixInAnnotations(Marker.class, MarkerMixIn.class);
        context.setMixInAnnotations(Level.class, LevelMixIn.class);
        context.setMixInAnnotations(Instant.class, InstantMixIn.class);
        context.setMixInAnnotations(LogEvent.class, LogEventWithContextListMixIn.class);
        // Log4j Core classes: we do not want to bring in Jackson at runtime if we do not have to.
        context.setMixInAnnotations(ExtendedStackTraceElement.class, ExtendedStackTraceElementMixIn.class);
        context.setMixInAnnotations(ThrowableProxy.class, includeStacktrace
                ? (stacktraceAsString ? ThrowableProxyWithStacktraceAsStringMixIn.class : ThrowableProxyMixIn.class)
                : ThrowableProxyWithoutStacktraceMixIn.class);
    }

}