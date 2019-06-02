package org.apache.logging.log4j.groovy.declarative;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.util.Builder;
import org.apache.logging.log4j.groovy.api.Component;
import org.apache.logging.log4j.groovy.api.ComponentBuilder;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeclarativeComponentBuilder extends GroovyObjectSupport implements Builder<Component> {

    private final LoggerContext context;
    private final ConfigurationSource source;
    private final ComponentBuilder builder;

    DeclarativeComponentBuilder(final LoggerContext context, final ConfigurationSource source, final ComponentBuilder builder) {
        this.context = context;
        this.source = source;
        this.builder = builder;
    }

    @Override
    public Component build() {
        return builder.build();
    }

    @Override
    public Object invokeMethod(final String name, final Object args) {
        DSL dsl = DSL.fromMethodInvocation(getClass(), name, InvokerHelper.asList(args));
        DeclarativeComponentBuilder builder = new DeclarativeComponentBuilder(context, source, Component.newBuilder()
                .withPluginType(dsl.name)
                .withAttributes(dsl.attributes)
                .withValue(dsl.value));
        dsl.body.setDelegate(builder);
        dsl.body.call();
        // FIXME: this doesn't seem to be working
        this.builder.withComponent(builder);
        return this;
    }

    private static class DSL {
        private final String name;
        private final Map<String, String> attributes;
        private final Closure<?> body;
        private final String value;

        private DSL(final String name, final Map<?, ?> attributes, final Closure<?> body, final String value) {
            this.name = name;
            this.attributes = attributes.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
            this.body = body;
            this.value = value != null ? value : this.attributes.get("value");
        }

        private static DSL fromMethodInvocation(final Class<?> builderClass, final String name, final List<?> args) {
            switch (args.size()) {
                case 0:
                    // name()
                    return new DSL(name, Collections.emptyMap(), Closure.IDENTITY, null);

                case 1: {
                    Object arg = args.get(0);
                    if (arg instanceof Map) {
                        // name(arg1: val1, arg2: val2)
                        return new DSL(name, (Map<?, ?>) arg, Closure.IDENTITY, null);
                    } else if (arg instanceof Closure) {
                        // name { ... }
                        return new DSL(name, Collections.emptyMap(), (Closure<?>) arg, null);
                    } else if (arg != null) {
                        // name(arg)
                        return new DSL(name, Collections.emptyMap(), Closure.IDENTITY, arg.toString());
                    } else {
                        // name(null)
                        return new DSL(name, Collections.emptyMap(), Closure.IDENTITY, null);
                    }
                }

                case 2: {
                    Object arg1 = args.get(0);
                    Object arg2 = args.get(1);
                    if (arg1 instanceof Map) {
                        Map<?, ?> attributes = (Map<?, ?>) arg1;
                        if (arg2 instanceof Closure) {
                            // name(arg1a: val1a, arg1b: val1b) { ... }
                            return new DSL(name, attributes, (Closure<?>) arg2, null);
                        } else if (arg2 != null) {
                            // no syntax sugar here
                            return new DSL(name, attributes, Closure.IDENTITY, arg2.toString());
                        } else {
                            // name(arg1a: val1a, arg1b: val1b)
                            return new DSL(name, attributes, Closure.IDENTITY, null);
                        }
                    } else {
                        String innerText = arg1 == null ? null : arg1.toString();
                        if (arg2 instanceof Map) {
                            // similar to above
                            return new DSL(name, (Map<?, ?>) arg2, Closure.IDENTITY, innerText);
                        } else if (arg2 instanceof Closure) {
                            // name(arg1) { ... }
                            return new DSL(name, Collections.emptyMap(), (Closure<?>) arg2, innerText);
                        } else {
                            throw new MissingMethodException(name, builderClass, args.toArray());
                        }
                    }
                }

                case 3: {
                    Object arg1 = args.get(0);
                    Object arg2 = args.get(1);
                    Object arg3 = args.get(2);
                    if (arg3 instanceof Closure) {
                        Closure<?> closure = (Closure<?>) arg3;
                        if (arg1 instanceof Map) {
                            return new DSL(name, (Map<?, ?>) arg1, closure, arg2 == null ? null : arg2.toString());
                        } else if (arg2 instanceof Map) {
                            return new DSL(name, (Map<?, ?>) arg2, closure, arg1 == null ? null : arg1.toString());
                        }
                    }
                    throw new MissingMethodException(name, builderClass, args.toArray());
                }

                default:
                    throw new MissingMethodException(name, builderClass, args.toArray());

            }
        }
    }
}
