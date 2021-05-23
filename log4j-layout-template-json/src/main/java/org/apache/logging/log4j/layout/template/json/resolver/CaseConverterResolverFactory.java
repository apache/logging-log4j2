package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginFactory;

/**
 * {@link CaseConverterResolver} factory.
 */
@Plugin(name = "CaseConverterResolverFactory", category = TemplateResolverFactory.CATEGORY)
public final class CaseConverterResolverFactory implements EventResolverFactory {

    private static final CaseConverterResolverFactory INSTANCE =
            new CaseConverterResolverFactory();

    private CaseConverterResolverFactory() {}

    @PluginFactory
    public static CaseConverterResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return CaseConverterResolver.getName();
    }

    @Override
    public CaseConverterResolver create(
            final EventResolverContext context,
            final TemplateResolverConfig config) {
        return new CaseConverterResolver(context, config);
    }

}
