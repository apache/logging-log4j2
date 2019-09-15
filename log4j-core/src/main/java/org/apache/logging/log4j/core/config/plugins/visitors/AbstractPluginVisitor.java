package org.apache.logging.log4j.core.config.plugins.visitors;

import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.inject.AbstractConfigurationInjectionBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.function.Function;

abstract class AbstractPluginVisitor<Ann extends Annotation, Cfg> extends AbstractConfigurationInjectionBuilder<Ann, Cfg> {

    AbstractPluginVisitor(final Class<Ann> clazz) {
        super(clazz);
    }

    public AbstractPluginVisitor<Ann, Cfg> setAnnotation(final Annotation annotation) {
        if (clazz.isInstance(annotation)) {
            withAnnotation(clazz.cast(annotation));
        }
        return this;
    }

    public AbstractPluginVisitor<Ann, Cfg> setAliases(final String... aliases) {
        withAliases(aliases);
        return this;
    }

    public AbstractPluginVisitor<Ann, Cfg> setConversionType(final Class<?> conversionType) {
        withConversionType(conversionType);
        return this;
    }

    public AbstractPluginVisitor<Ann, Cfg> setMember(final Member member) {
        withMember(member);
        return this;
    }

    public Object visit(final Cfg configuration, final Node node, final Function<String, String> substitutor, final StringBuilder log) {
        return this.withConfiguration(configuration)
                .withConfigurationNode(node)
                .withStringSubstitutionStrategy(substitutor)
                .withDebugLog(log)
                .build();
    }
}
