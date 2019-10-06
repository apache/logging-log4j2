package org.apache.logging.log4j.plugins.inject;

import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.bind.OptionBinder;
import org.apache.logging.log4j.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;

public interface ConfigurationInjector<Ann extends Annotation, Cfg> {

    static <Cfg> Optional<ConfigurationInjector<Annotation, Cfg>> forAnnotatedElement(final AnnotatedElement element) {
        for (final Annotation annotation : element.getAnnotations()) {
            final InjectorStrategy strategy = annotation.annotationType().getAnnotation(InjectorStrategy.class);
            if (strategy != null) {
                @SuppressWarnings("unchecked") final ConfigurationInjector<Annotation, Cfg> injector =
                        (ConfigurationInjector<Annotation, Cfg>) ReflectionUtil.instantiate(strategy.value());
                return Optional.of(injector.withAnnotatedElement(element).withAnnotation(annotation));
            }
        }
        return Optional.empty();
    }

    ConfigurationInjector<Ann, Cfg> withAnnotation(final Ann annotation);

    ConfigurationInjector<Ann, Cfg> withAnnotatedElement(final AnnotatedElement element);

    ConfigurationInjector<Ann, Cfg> withConversionType(final Type type);

    ConfigurationInjector<Ann, Cfg> withName(final String name);

    ConfigurationInjector<Ann, Cfg> withAliases(final String... aliases);

    ConfigurationInjector<Ann, Cfg> withOptionBinder(final OptionBinder binder);

    ConfigurationInjector<Ann, Cfg> withDebugLog(final StringBuilder debugLog);

    ConfigurationInjector<Ann, Cfg> withStringSubstitutionStrategy(final Function<String, String> strategy);

    ConfigurationInjector<Ann, Cfg> withConfiguration(final Cfg configuration);

    ConfigurationInjector<Ann, Cfg> withNode(final Node node);

    Object inject(final Object target);
}
