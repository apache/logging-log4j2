package org.apache.logging.log4j.groovy.declarative;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.util.DelegatingScript;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.util.LoaderUtil;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

class DeclarativeConfigurationCompiler {

    static DeclarativeConfigurationCompiler newCompiler(final LoggerContext context) {
        Object externalContext = context.getExternalContext();
        ClassLoader parent = externalContext instanceof ClassLoader ? (ClassLoader) externalContext : LoaderUtil.getThreadContextClassLoader();
        return new DeclarativeConfigurationCompiler(parent);
    }

    private final GroovyClassLoader loader;

    private DeclarativeConfigurationCompiler(final ClassLoader context) {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setDefaultScriptExtension(".groovydsl");
        cc.setScriptBaseClass(DelegatingScript.class.getName());
        loader = new GroovyClassLoader(context, cc);
    }

    DelegatingScript compile(final ConfigurationSource source) throws IOException {
        return (DelegatingScript) InvokerHelper.createScript(loader.parseClass(convert(source)), new Binding());
    }

    private static GroovyCodeSource convert(final ConfigurationSource source) throws IOException {
        File file = source.getFile();
        if (file != null) {
            return new GroovyCodeSource(file);
        }
        URL url = source.getURL();
        if (url != null) {
            return new GroovyCodeSource(url);
        }
        return new GroovyCodeSource(new InputStreamReader(source.getInputStream()), source.toString(), source.toString());
    }

}
