package org.apache.logging.log4j.core.config.plugins.validation;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

@Plugin(name = "PluginWithGenericSubclassFoo1Builder", category = "Test")
public class PluginWithGenericSubclassFoo1Builder extends AbstractPluginWithGenericBuilder {

    public static class Builder<B extends Builder<B>> extends AbstractPluginWithGenericBuilder.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<PluginWithGenericSubclassFoo1Builder> {

        @PluginBuilderFactory
        public static <B extends Builder<B>> B newBuilder() {
            return new Builder<B>().asBuilder();
        }

        @PluginBuilderAttribute
        @Required(message = "The foo1 given by the builder is null")
        private String foo1;

        @Override
        public PluginWithGenericSubclassFoo1Builder build() {
            return new PluginWithGenericSubclassFoo1Builder(getThing(), getFoo1());
        }

        public String getFoo1() {
            return foo1;
        }

        public B withFoo1(final String foo1) {
            this.foo1 = foo1;
            return asBuilder();
        }

    }
    
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final String foo1;

    public PluginWithGenericSubclassFoo1Builder(final String thing, final String foo1) {
        super(thing);
        this.foo1 = foo1;
    }

    public String getFoo1() {
        return foo1;
    }

}
