/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.config.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.helpers.Charsets;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;

/**
 * <p>ResolverUtil is used to locate classes that are available in the/a class path and meet
 * arbitrary conditions. The two most common conditions are that a class implements/extends
 * another class, or that is it annotated with a specific annotation. However, through the use
 * of the {@link Test} class it is possible to search using arbitrary conditions.</p>
 *
 * <p>A ClassLoader is used to locate all locations (directories and jar files) in the class
 * path that contain classes within certain packages, and then to load those classes and
 * check them. By default the ClassLoader returned by
 *  {@code Thread.currentThread().getContextClassLoader()} is used, but this can be overridden
 * by calling {@link #setClassLoader(ClassLoader)} prior to invoking any of the {@code find()}
 * methods.</p>
 *
 * <p>General searches are initiated by calling the
 * {@link #find(ResolverUtil.Test, String...)} method and supplying
 * a package name and a Test instance. This will cause the named package <b>and all sub-packages</b>
 * to be scanned for classes that meet the test. There are also utility methods for the common
 * use cases of scanning multiple packages for extensions of particular classes, or classes
 * annotated with a specific annotation.</p>
 *
 * <p>The standard usage pattern for the ResolverUtil class is as follows:</p>
 *
 *<pre>
 *ResolverUtil&lt;ActionBean&gt; resolver = new ResolverUtil&lt;ActionBean&gt;();
 *resolver.findImplementation(ActionBean.class, pkg1, pkg2);
 *resolver.find(new CustomTest(), pkg1);
 *resolver.find(new CustomTest(), pkg2);
 *Collection&lt;ActionBean&gt; beans = resolver.getClasses();
 *</pre>
 *
 * <p>This class was copied and modified from Stripes - http://stripes.mc4j.org/confluence/display/stripes/Home
 * </p>
 */
public class ResolverUtil {
    /** An instance of Log to use for logging in this class. */
    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final String VFSZIP = "vfszip";

    private static final String BUNDLE_RESOURCE = "bundleresource";

    /** The set of matches being accumulated. */
    private final Set<Class<?>> classMatches = new HashSet<Class<?>>();

    /** The set of matches being accumulated. */
    private final Set<URI> resourceMatches = new HashSet<URI>();

    /**
     * The ClassLoader to use when looking for classes. If null then the ClassLoader returned
     * by Thread.currentThread().getContextClassLoader() will be used.
     */
    private ClassLoader classloader;

    /**
     * Provides access to the classes discovered so far. If no calls have been made to
     * any of the {@code find()} methods, this set will be empty.
     *
     * @return the set of classes that have been discovered.
     */
    public Set<Class<?>> getClasses() {
        return classMatches;
    }

    /**
     * Returns the matching resources.
     * @return A Set of URIs that match the criteria.
     */
    public Set<URI> getResources() {
        return resourceMatches;
    }


    /**
     * Returns the classloader that will be used for scanning for classes. If no explicit
     * ClassLoader has been set by the calling, the context class loader will be used.
     *
     * @return the ClassLoader that will be used to scan for classes
     */
    public ClassLoader getClassLoader() {
        return classloader != null ? classloader : (classloader = Loader.getClassLoader(ResolverUtil.class, null));
    }

    /**
     * Sets an explicit ClassLoader that should be used when scanning for classes. If none
     * is set then the context classloader will be used.
     *
     * @param classloader a ClassLoader to use when scanning for classes
     */
    public void setClassLoader(final ClassLoader classloader) { this.classloader = classloader; }

    /**
     * Attempts to discover classes that are assignable to the type provided. In the case
     * that an interface is provided this method will collect implementations. In the case
     * of a non-interface class, subclasses will be collected.  Accumulated classes can be
     * accessed by calling {@link #getClasses()}.
     *
     * @param parent the class of interface to find subclasses or implementations of
     * @param packageNames one or more package names to scan (including subpackages) for classes
     */
    public void findImplementations(final Class<?> parent, final String... packageNames) {
        if (packageNames == null) {
            return;
        }

        final Test test = new IsA(parent);
        for (final String pkg : packageNames) {
            findInPackage(test, pkg);
        }
    }

    /**
     * Attempts to discover classes who's name ends with the provided suffix. Accumulated classes can be
     * accessed by calling {@link #getClasses()}.
     *
     * @param suffix The class name suffix to match
     * @param packageNames one or more package names to scan (including subpackages) for classes
     */
    public void findSuffix(final String suffix, final String... packageNames) {
        if (packageNames == null) {
            return;
        }

        final Test test = new NameEndsWith(suffix);
        for (final String pkg : packageNames) {
            findInPackage(test, pkg);
        }
    }

    /**
     * Attempts to discover classes that are annotated with to the annotation. Accumulated
     * classes can be accessed by calling {@link #getClasses()}.
     *
     * @param annotation the annotation that should be present on matching classes
     * @param packageNames one or more package names to scan (including subpackages) for classes
     */
    public void findAnnotated(final Class<? extends Annotation> annotation, final String... packageNames) {
        if (packageNames == null) {
            return;
        }

        final Test test = new AnnotatedWith(annotation);
        for (final String pkg : packageNames) {
            findInPackage(test, pkg);
        }
    }

    public void findNamedResource(final String name, final String... pathNames) {
        if (pathNames == null) {
            return;
        }

        final Test test = new NameIs(name);
        for (final String pkg : pathNames) {
            findInPackage(test, pkg);
        }
    }

    /**
     * Attempts to discover classes that pass the test. Accumulated
     * classes can be accessed by calling {@link #getClasses()}.
     *
     * @param test the test to determine matching classes
     * @param packageNames one or more package names to scan (including subpackages) for classes
     */
    public void find(final Test test, final String... packageNames) {
        if (packageNames == null) {
            return;
        }

        for (final String pkg : packageNames) {
            findInPackage(test, pkg);
        }
    }

    /**
     * Scans for classes starting at the package provided and descending into subpackages.
     * Each class is offered up to the Test as it is discovered, and if the Test returns
     * true the class is retained.  Accumulated classes can be fetched by calling
     * {@link #getClasses()}.
     *
     * @param test an instance of {@link Test} that will be used to filter classes
     * @param packageName the name of the package from which to start scanning for
     *        classes, e.g. {@code net.sourceforge.stripes}
     */
    public void findInPackage(final Test test, String packageName) {
        packageName = packageName.replace('.', '/');
        final ClassLoader loader = getClassLoader();
        Enumeration<URL> urls;

        try {
            urls = loader.getResources(packageName);
        } catch (final IOException ioe) {
            LOGGER.warn("Could not read package: " + packageName, ioe);
            return;
        }

        while (urls.hasMoreElements()) {
            try {
                final URL url = urls.nextElement();
                String urlPath = url.getFile();
                urlPath = URLDecoder.decode(urlPath, Charsets.UTF_8.name());

                // If it's a file in a directory, trim the stupid file: spec
                if (urlPath.startsWith("file:")) {
                    urlPath = urlPath.substring(5);
                }

                // Else it's in a JAR, grab the path to the jar
                if (urlPath.indexOf('!') > 0) {
                    urlPath = urlPath.substring(0, urlPath.indexOf('!'));
                }

                LOGGER.info("Scanning for classes in [" + urlPath + "] matching criteria: " + test);
                // Check for a jar in a war in JBoss
                if (VFSZIP.equals(url.getProtocol())) {
                    final String path = urlPath.substring(0, urlPath.length() - packageName.length() - 2);
                    final URL newURL = new URL(url.getProtocol(), url.getHost(), path);
                    @SuppressWarnings("resource")
                    final JarInputStream stream = new JarInputStream(newURL.openStream());
                    try {
                        loadImplementationsInJar(test, packageName, path, stream);
                    } finally {
                        close(stream, newURL);
                    }
                } else if (BUNDLE_RESOURCE.equals(url.getProtocol())) {
                    loadImplementationsInBundle(test, packageName);
                } else {
                    final File file = new File(urlPath);
                    if (file.isDirectory()) {
                        loadImplementationsInDirectory(test, packageName, file);
                    } else {
                        loadImplementationsInJar(test, packageName, file);
                    }
                }
            } catch (final IOException ioe) {
                LOGGER.warn("could not read entries", ioe);
            }
        }
    }

    private void loadImplementationsInBundle(final Test test, final String packageName) {
        //Do not remove the cast on the next line as removing it will cause a compile error on Java 7.
        final BundleWiring wiring = (BundleWiring) FrameworkUtil.getBundle(
                ResolverUtil.class).adapt(BundleWiring.class);
        final Collection<String> list = wiring.listResources(packageName, "*.class",
            BundleWiring.LISTRESOURCES_RECURSE);
        for (final String name : list) {
            addIfMatching(test, name);
        }
    }


    /**
     * Finds matches in a physical directory on a filesystem.  Examines all
     * files within a directory - if the File object is not a directory, and ends with <i>.class</i>
     * the file is loaded and tested to see if it is acceptable according to the Test.  Operates
     * recursively to find classes within a folder structure matching the package structure.
     *
     * @param test a Test used to filter the classes that are discovered
     * @param parent the package name up to this directory in the package hierarchy.  E.g. if
     *        /classes is in the classpath and we wish to examine files in /classes/org/apache then
     *        the values of <i>parent</i> would be <i>org/apache</i>
     * @param location a File object representing a directory
     */
    private void loadImplementationsInDirectory(final Test test, final String parent, final File location) {
        final File[] files = location.listFiles();
        if (files == null) {
            return;
        }

        StringBuilder builder;
        for (final File file : files) {
            builder = new StringBuilder();
            builder.append(parent).append("/").append(file.getName());
            final String packageOrClass = parent == null ? file.getName() : builder.toString();

            if (file.isDirectory()) {
                loadImplementationsInDirectory(test, packageOrClass, file);
            } else if (isTestApplicable(test, file.getName())) {
                addIfMatching(test, packageOrClass);
            }
        }
    }

    private boolean isTestApplicable(final Test test, final String path) {
        return test.doesMatchResource() || path.endsWith(".class") && test.doesMatchClass();
    }

    /**
     * Finds matching classes within a jar files that contains a folder structure
     * matching the package structure.  If the File is not a JarFile or does not exist a warning
     * will be logged, but no error will be raised.
     *
     * @param test a Test used to filter the classes that are discovered
     * @param parent the parent package under which classes must be in order to be considered
     * @param jarFile the jar file to be examined for classes
     */
    private void loadImplementationsInJar(final Test test, final String parent, final File jarFile) {
        @SuppressWarnings("resource")
        JarInputStream jarStream = null;
        try {
            jarStream = new JarInputStream(new FileInputStream(jarFile));
            loadImplementationsInJar(test, parent, jarFile.getPath(), jarStream);
        } catch (final FileNotFoundException ex) {
            LOGGER.error("Could not search jar file '" + jarFile + "' for classes matching criteria: " + test
                    + " file not found");
        } catch (final IOException ioe) {
            LOGGER.error("Could not search jar file '" + jarFile + "' for classes matching criteria: " + test
                    + " due to an IOException", ioe);
        } finally {
            close(jarStream, jarFile);
        }
    }

    /**
     * @param jarStream
     * @param source
     */
    private void close(JarInputStream jarStream, final Object source) {
        if (jarStream != null) {
            try {
                jarStream.close();
            } catch (IOException e) {
                LOGGER.error("Error closing JAR file stream for {}", source, e);
            }
        }
    }

    /**
     * Finds matching classes within a jar files that contains a folder structure
     * matching the package structure.  If the File is not a JarFile or does not exist a warning
     * will be logged, but no error will be raised.
     *
     * @param test a Test used to filter the classes that are discovered
     * @param parent the parent package under which classes must be in order to be considered
     * @param stream The jar InputStream
     */
    private void loadImplementationsInJar(final Test test, final String parent, final String path,
                                          final JarInputStream stream) {

        try {
            JarEntry entry;

            while ((entry = stream.getNextJarEntry()) != null) {
                final String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith(parent) && isTestApplicable(test, name)) {
                    addIfMatching(test, name);
                }
            }
        } catch (final IOException ioe) {
            LOGGER.error("Could not search jar file '" + path + "' for classes matching criteria: " +
                test + " due to an IOException", ioe);
        }
    }

    /**
     * Add the class designated by the fully qualified class name provided to the set of
     * resolved classes if and only if it is approved by the Test supplied.
     *
     * @param test the test used to determine if the class matches
     * @param fqn the fully qualified name of a class
     */
    protected void addIfMatching(final Test test, final String fqn) {
        try {
            final ClassLoader loader = getClassLoader();
            if (test.doesMatchClass()) {
                final String externalName = fqn.substring(0, fqn.indexOf('.')).replace('/', '.');
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Checking to see if class " + externalName + " matches criteria [" + test + "]");
                }

                final Class<?> type = loader.loadClass(externalName);
                if (test.matches(type)) {
                    classMatches.add(type);
                }
            }
            if (test.doesMatchResource()) {
                URL url = loader.getResource(fqn);
                if (url == null) {
                    url = loader.getResource(fqn.substring(1));
                }
                if (url != null && test.matches(url.toURI())) {
                    resourceMatches.add(url.toURI());
                }
            }
        } catch (final Throwable t) {
            LOGGER.warn("Could not examine class '" + fqn + "' due to a " +
                t.getClass().getName() + " with message: " + t.getMessage());
        }
    }

    /**
     * A simple interface that specifies how to test classes to determine if they
     * are to be included in the results produced by the ResolverUtil.
     */
    public interface Test {
        /**
         * Will be called repeatedly with candidate classes. Must return True if a class
         * is to be included in the results, false otherwise.
         * @param type The Class to match against.
         * @return true if the Class matches.
         */
        boolean matches(Class<?> type);

        /**
         * Test for a resource.
         * @param resource The URI to the resource.
         * @return true if the resource matches.
         */
        boolean matches(URI resource);

        boolean doesMatchClass();

        boolean doesMatchResource();
    }

    /**
     * Test against a Class.
     */
    public abstract static class ClassTest implements Test {
        @Override
        public boolean matches(final URI resource) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean doesMatchClass() {
            return true;
        }

        @Override
        public boolean doesMatchResource() {
            return false;
        }
    }

    /**
     * Test against a resource.
     */
    public abstract static class ResourceTest implements Test {
        @Override
        public boolean matches(final Class<?> cls) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean doesMatchClass() {
            return false;
        }

        @Override
        public boolean doesMatchResource() {
            return true;
        }
    }

    /**
     * A Test that checks to see if each class is assignable to the provided class. Note
     * that this test will match the parent type itself if it is presented for matching.
     */
    public static class IsA extends ClassTest {
        private final Class<?> parent;

        /**
         * Constructs an IsA test using the supplied Class as the parent class/interface.
         * @param parentType The parent class to check for.
         */
        public IsA(final Class<?> parentType) { this.parent = parentType; }

        /**
         * Returns true if type is assignable to the parent type supplied in the constructor.
         * @param type The Class to check.
         * @return true if the Class matches.
         */
        @Override
        public boolean matches(final Class<?> type) {
            return type != null && parent.isAssignableFrom(type);
        }

        @Override
        public String toString() {
            return "is assignable to " + parent.getSimpleName();
        }
    }

    /**
     * A Test that checks to see if each class name ends with the provided suffix.
     */
    public static class NameEndsWith extends ClassTest {
        private final String suffix;

        /**
         * Constructs a NameEndsWith test using the supplied suffix.
         * @param suffix the String suffix to check for.
         */
        public NameEndsWith(final String suffix) { this.suffix = suffix; }

        /**
         * Returns true if type name ends with the suffix supplied in the constructor.
         * @param type The Class to check.
         * @return true if the Class matches.
         */
        @Override
        public boolean matches(final Class<?> type) {
            return type != null && type.getName().endsWith(suffix);
        }

        @Override
        public String toString() {
            return "ends with the suffix " + suffix;
        }
    }

    /**
     * A Test that checks to see if each class is annotated with a specific annotation. If it
     * is, then the test returns true, otherwise false.
     */
    public static class AnnotatedWith extends ClassTest {
        private final Class<? extends Annotation> annotation;

        /**
         * Constructs an AnnotatedWith test for the specified annotation type.
         * @param annotation The annotation to check for.
         */
        public AnnotatedWith(final Class<? extends Annotation> annotation) {
            this.annotation = annotation;
        }

        /**
         * Returns true if the type is annotated with the class provided to the constructor.
         * @param type the Class to match against.
         * @return true if the Classes match.
         */
        @Override
        public boolean matches(final Class<?> type) {
            return type != null && type.isAnnotationPresent(annotation);
        }

        @Override
        public String toString() {
            return "annotated with @" + annotation.getSimpleName();
        }
    }

    /**
     * A Test that checks to see if the class name matches.
     */
    public static class NameIs extends ResourceTest {
        private final String name;

        public NameIs(final String name) { this.name = "/" + name; }

        @Override
        public boolean matches(final URI resource) {
            return resource.getPath().endsWith(name);
        }

        @Override public String toString() {
            return "named " + name;
        }
    }
}
