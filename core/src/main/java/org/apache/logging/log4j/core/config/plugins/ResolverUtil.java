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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

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
 * {@link #find(org.apache.logging.log4j.core..util.ResolverUtil.Test, String...)} ()} method and supplying
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
 * <p>This class was copied from Stripes - http://stripes.mc4j.org/confluence/display/stripes/Home
 * </p>
 *
 * @author Tim Fennell
 * @param <T> The type of the Class that can be returned.
 */
public class ResolverUtil<T> {
    /** An instance of Log to use for logging in this class. */
    private static final Logger LOG = StatusLogger.getLogger();

    /** The set of matches being accumulated. */
    private Set<Class<? extends T>> classMatches = new HashSet<Class<?extends T>>();

    /** The set of matches being accumulated. */
    private Set<URI> resourceMatches = new HashSet<URI>();

    private static final String VFSZIP = "vfszip";

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
    public Set<Class<? extends T>> getClasses() {
        return classMatches;
    }

    /**
     * Return the matching resources.
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
    public void setClassLoader(ClassLoader classloader) { this.classloader = classloader; }

    /**
     * Attempts to discover classes that are assignable to the type provided. In the case
     * that an interface is provided this method will collect implementations. In the case
     * of a non-interface class, subclasses will be collected.  Accumulated classes can be
     * accessed by calling {@link #getClasses()}.
     *
     * @param parent the class of interface to find subclasses or implementations of
     * @param packageNames one or more package names to scan (including subpackages) for classes
     */
    public void findImplementations(Class parent, String... packageNames) {
        if (packageNames == null) {
            return;
        }

        Test test = new IsA(parent);
        for (String pkg : packageNames) {
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
    public void findSuffix(String suffix, String... packageNames) {
        if (packageNames == null) {
            return;
        }

        Test test = new NameEndsWith(suffix);
        for (String pkg : packageNames) {
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
    public void findAnnotated(Class<? extends Annotation> annotation, String... packageNames) {
        if (packageNames == null) {
            return;
        }

        Test test = new AnnotatedWith(annotation);
        for (String pkg : packageNames) {
            findInPackage(test, pkg);
        }
    }

    public void findNamedResource(String name, String... pathNames) {
        if (pathNames == null) {
            return;
        }

        Test test = new NameIs(name);
        for (String pkg : pathNames) {
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
    public void find(Test test, String... packageNames) {
        if (packageNames == null) {
            return;
        }

        for (String pkg : packageNames) {
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
    public void findInPackage(Test test, String packageName) {
        packageName = packageName.replace('.', '/');
        ClassLoader loader = getClassLoader();
        Enumeration<URL> urls;

        try {
            urls = loader.getResources(packageName);
        } catch (IOException ioe) {
            LOG.warn("Could not read package: " + packageName, ioe);
            return;
        }

        while (urls.hasMoreElements()) {
            try {
                URL url = urls.nextElement();
                String urlPath = url.getFile();
                urlPath = URLDecoder.decode(urlPath, "UTF-8");

                // If it's a file in a directory, trim the stupid file: spec
                if (urlPath.startsWith("file:")) {
                    urlPath = urlPath.substring(5);
                }

                // Else it's in a JAR, grab the path to the jar
                if (urlPath.indexOf('!') > 0) {
                    urlPath = urlPath.substring(0, urlPath.indexOf('!'));
                }

                LOG.info("Scanning for classes in [" + urlPath + "] matching criteria: " + test);
                // Check for a jar in a war in JBoss
                if (VFSZIP.equals(url.getProtocol())) {
                    String path = urlPath.substring(0, urlPath.length() - packageName.length() - 2);
                    URL newURL = new URL(url.getProtocol(), url.getHost(), path);
                    JarInputStream stream = new JarInputStream(newURL.openStream());
                    loadImplementationsInJar(test, packageName, path, stream);
                } else {
                    File file = new File(urlPath);
                    if (file.isDirectory()) {
                        loadImplementationsInDirectory(test, packageName, file);
                    } else {
                        loadImplementationsInJar(test, packageName, file);
                    }
                }
            } catch (IOException ioe) {
                LOG.warn("could not read entries", ioe);
            }
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
    private void loadImplementationsInDirectory(Test test, String parent, File location) {
        File[] files = location.listFiles();
        StringBuilder builder;

        for (File file : files) {
            builder = new StringBuilder();
            builder.append(parent).append("/").append(file.getName());
            String packageOrClass = parent == null ? file.getName() : builder.toString();

            if (file.isDirectory()) {
                loadImplementationsInDirectory(test, packageOrClass, file);
            } else if (isTestApplicable(test, file.getName())) {
                addIfMatching(test, packageOrClass);
            }
        }
    }

    private boolean isTestApplicable(Test test, String path) {
        return test.doesMatchResource() || path.endsWith(".class") && test.doesMatchClass();
    }

    /**
     * Finds matching classes within a jar files that contains a folder structure
     * matching the package structure.  If the File is not a JarFile or does not exist a warning
     * will be logged, but no error will be raised.
     *
     * @param test a Test used to filter the classes that are discovered
     * @param parent the parent package under which classes must be in order to be considered
     * @param jarfile the jar file to be examined for classes
     */
    private void loadImplementationsInJar(Test test, String parent, File jarfile) {
        JarInputStream jarStream;
        try {
            jarStream = new JarInputStream(new FileInputStream(jarfile));
            loadImplementationsInJar(test, parent, jarfile.getPath(), jarStream);
        } catch (FileNotFoundException ex) {
            LOG.error("Could not search jar file '" + jarfile + "' for classes matching criteria: " +
                test + " file not found");
        } catch (IOException ioe) {
            LOG.error("Could not search jar file '" + jarfile + "' for classes matching criteria: " +
                test + " due to an IOException", ioe);
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
    private void loadImplementationsInJar(Test test, String parent, String path, JarInputStream stream) {

        try {
            JarEntry entry;

            while ((entry = stream.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith(parent) && isTestApplicable(test, name)) {
                    addIfMatching(test, name);
                }
            }
        } catch (IOException ioe) {
            LOG.error("Could not search jar file '" + path + "' for classes matching criteria: " +
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
    protected void addIfMatching(Test test, String fqn) {
        try {
            ClassLoader loader = getClassLoader();
            if (test.doesMatchClass()) {
                String externalName = fqn.substring(0, fqn.indexOf('.')).replace('/', '.');
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Checking to see if class " + externalName + " matches criteria [" + test + "]");
                }

                Class type = loader.loadClass(externalName);
                if (test.matches(type)) {
                    classMatches.add((Class<T>) type);
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
        } catch (Throwable t) {
            LOG.warn("Could not examine class '" + fqn + "' due to a " +
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
        boolean matches(Class type);

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
        public boolean matches(URI resource) {
            throw new UnsupportedOperationException();
        }

        public boolean doesMatchClass() {
            return true;
        }
        public boolean doesMatchResource() {
            return false;
        }
    }

    /**
     * Test against a resource.
     */
    public abstract static class ResourceTest implements Test {
        public boolean matches(Class cls) {
            throw new UnsupportedOperationException();
        }

        public boolean doesMatchClass() {
            return false;
        }
        public boolean doesMatchResource() {
            return true;
        }
    }

    /**
     * A Test that checks to see if each class is assignable to the provided class. Note
     * that this test will match the parent type itself if it is presented for matching.
     */
    public static class IsA extends ClassTest {
        private Class parent;

        /**
         * Constructs an IsA test using the supplied Class as the parent class/interface.
         * @param parentType The parent class to check for.
         */
        public IsA(Class parentType) { this.parent = parentType; }

        /**
         * Returns true if type is assignable to the parent type supplied in the constructor.
         * @param type The Class to check.
         * @return true if the Class matches.
         */
        public boolean matches(Class type) {
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
        private String suffix;

        /**
         * Constructs a NameEndsWith test using the supplied suffix.
         * @param suffix the String suffix to check for.
         */
        public NameEndsWith(String suffix) { this.suffix = suffix; }

        /**
         * Returns true if type name ends with the suffix supplied in the constructor.
         * @param type The Class to check.
         * @return true if the Class matches.
         */
        public boolean matches(Class type) {
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
        private Class<? extends Annotation> annotation;

        /**
         * Constructs an AnnotatedWith test for the specified annotation type.
         * @param annotation The annotation to check for.
         */
        public AnnotatedWith(Class<? extends Annotation> annotation) {
            this.annotation = annotation;
        }

        /**
         * Returns true if the type is annotated with the class provided to the constructor.
         * @param type the Class to match against.
         * @return true if the Classes match.
         */
        public boolean matches(Class type) {
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
        private String name;

        public NameIs(String name) { this.name = "/" + name; }

        public boolean matches(URI resource) {
            return (resource.getPath().endsWith(name));
        }

        @Override public String toString() {
            return "named " + name;
        }
    }
}
