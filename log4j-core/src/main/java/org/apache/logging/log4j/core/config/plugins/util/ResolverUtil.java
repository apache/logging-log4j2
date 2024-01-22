/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.config.plugins.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;

/**
 * <p>
 * ResolverUtil is used to locate classes that are available in the/a class path and meet arbitrary conditions. The two
 * most common conditions are that a class implements/extends another class, or that is it annotated with a specific
 * annotation. However, through the use of the {@link Test} class it is possible to search using arbitrary conditions.
 * </p>
 *
 * <p>
 * A ClassLoader is used to locate all locations (directories and jar files) in the class path that contain classes
 * within certain packages, and then to load those classes and check them. By default the ClassLoader returned by
 * {@code Thread.currentThread().getContextClassLoader()} is used, but this can be overridden by calling
 * {@link #setClassLoader(ClassLoader)} prior to invoking any of the {@code find()} methods.
 * </p>
 *
 * <p>
 * General searches are initiated by calling the {@link #find(ResolverUtil.Test, String...)} method and supplying a
 * package name and a Test instance. This will cause the named package <b>and all sub-packages</b> to be scanned for
 * classes that meet the test. There are also utility methods for the common use cases of scanning multiple packages for
 * extensions of particular classes, or classes annotated with a specific annotation.
 * </p>
 *
 * <p>
 * The standard usage pattern for the ResolverUtil class is as follows:
 * </p>
 *
 * <pre>
 * ResolverUtil resolver = new ResolverUtil();
 * resolver.findInPackage(new CustomTest(), pkg1);
 * resolver.find(new CustomTest(), pkg1);
 * resolver.find(new CustomTest(), pkg1, pkg2);
 * Set&lt;Class&lt;?&gt;&gt; beans = resolver.getClasses();
 * </pre>
 *
 * <p>
 * This class was copied and modified from Stripes - http://stripes.mc4j.org/confluence/display/stripes/Home
 * </p>
 */
public class ResolverUtil {
    /** An instance of Log to use for logging in this class. */
    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final String VFSZIP = "vfszip";

    private static final String VFS = "vfs";

    private static final String JAR = "jar";

    private static final String BUNDLE_RESOURCE = "bundleresource";

    /** The set of matches being accumulated. */
    private final Set<Class<?>> classMatches = new HashSet<>();

    /** The set of matches being accumulated. */
    private final Set<URI> resourceMatches = new HashSet<>();

    /**
     * The ClassLoader to use when looking for classes. If null then the ClassLoader returned by
     * Thread.currentThread().getContextClassLoader() will be used.
     */
    private ClassLoader classloader;

    /**
     * Provides access to the classes discovered so far. If no calls have been made to any of the {@code find()}
     * methods, this set will be empty.
     *
     * @return the set of classes that have been discovered.
     */
    public Set<Class<?>> getClasses() {
        return classMatches;
    }

    /**
     * Returns the matching resources.
     *
     * @return A Set of URIs that match the criteria.
     */
    public Set<URI> getResources() {
        return resourceMatches;
    }

    /**
     * Returns the ClassLoader that will be used for scanning for classes. If no explicit ClassLoader has been set by
     * the calling, the context class loader will be used.
     *
     * @return the ClassLoader that will be used to scan for classes
     */
    public ClassLoader getClassLoader() {
        return classloader != null ? classloader : (classloader = Loader.getClassLoader(ResolverUtil.class, null));
    }

    /**
     * Sets an explicit ClassLoader that should be used when scanning for classes. If none is set then the context
     * ClassLoader will be used.
     *
     * @param aClassloader
     *        a ClassLoader to use when scanning for classes
     */
    public void setClassLoader(final ClassLoader aClassloader) {
        this.classloader = aClassloader;
    }

    /**
     * Attempts to discover classes that pass the test. Accumulated classes can be accessed by calling
     * {@link #getClasses()}.
     *
     * @param test
     *        the test to determine matching classes
     * @param packageNames
     *        one or more package names to scan (including subpackages) for classes
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
     * Scans for classes starting at the package provided and descending into subpackages. Each class is offered up to
     * the Test as it is discovered, and if the Test returns true the class is retained. Accumulated classes can be
     * fetched by calling {@link #getClasses()}.
     *
     * @param test
     *        an instance of {@link Test} that will be used to filter classes
     * @param packageName
     *        the name of the package from which to start scanning for classes, e.g. {@code net.sourceforge.stripes}
     */
    @SuppressFBWarnings(
            value = {"URLCONNECTION_SSRF_FD", "PATH_TRAVERSAL_IN"},
            justification = "The URLs used come from the classloader.")
    public void findInPackage(final Test test, String packageName) {
        packageName = packageName.replace('.', '/');
        final ClassLoader loader = getClassLoader();
        Enumeration<URL> urls;

        try {
            urls = loader.getResources(packageName);
        } catch (final IOException ioe) {
            LOGGER.warn("Could not read package: {}", packageName, ioe);
            return;
        }

        while (urls.hasMoreElements()) {
            try {
                final URL url = urls.nextElement();
                final String urlPath = extractPath(url);

                LOGGER.debug("Scanning for classes in '{}' matching criteria {}", urlPath, test);
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
                } else if (VFS.equals(url.getProtocol())) {
                    final String containerPath = urlPath.substring(1, urlPath.length() - packageName.length() - 2);
                    final File containerFile = new File(containerPath);
                    if (containerFile.exists()) {
                        if (containerFile.isDirectory()) {
                            loadImplementationsInDirectory(test, packageName, new File(containerFile, packageName));
                        } else {
                            loadImplementationsInJar(test, packageName, containerFile);
                        }
                    } else {
                        // fallback code for Jboss/Wildfly, if the file couldn't be found
                        // by loading the path as a file, try to read the jar as a stream
                        final String path = urlPath.substring(0, urlPath.length() - packageName.length() - 2);
                        final URL newURL = new URL(url.getProtocol(), url.getHost(), path);

                        try (final InputStream is = newURL.openStream()) {
                            final JarInputStream jarStream;
                            if (is instanceof JarInputStream) {
                                jarStream = (JarInputStream) is;
                            } else {
                                jarStream = new JarInputStream(is);
                            }
                            loadImplementationsInJar(test, packageName, path, jarStream);
                        }
                    }
                } else if (BUNDLE_RESOURCE.equals(url.getProtocol())) {
                    loadImplementationsInBundle(test, packageName);
                } else if (JAR.equals(url.getProtocol())) {
                    loadImplementationsInJar(test, packageName, url);
                } else {
                    final File file = new File(urlPath);
                    if (file.isDirectory()) {
                        loadImplementationsInDirectory(test, packageName, file);
                    } else {
                        loadImplementationsInJar(test, packageName, file);
                    }
                }
            } catch (final IOException | URISyntaxException ioe) {
                LOGGER.warn("Could not read entries", ioe);
            }
        }
    }

    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "The URLs used come from the classloader.")
    String extractPath(final URL url) throws UnsupportedEncodingException, URISyntaxException {
        String urlPath = url.getPath(); // same as getFile but without the Query portion
        // System.out.println(url.getProtocol() + "->" + urlPath);

        // I would be surprised if URL.getPath() ever starts with "jar:" but no harm in checking
        if (urlPath.startsWith("jar:")) {
            urlPath = urlPath.substring(4);
        }
        // For jar: URLs, the path part starts with "file:"
        if (urlPath.startsWith("file:")) {
            urlPath = urlPath.substring(5);
        }
        // If it was in a JAR, grab the path to the jar
        final int bangIndex = urlPath.indexOf('!');
        if (bangIndex > 0) {
            urlPath = urlPath.substring(0, bangIndex);
        }

        // LOG4J2-445
        // Finally, decide whether to URL-decode the file name or not...
        final String protocol = url.getProtocol();
        final List<String> neverDecode = Arrays.asList(VFS, VFSZIP, BUNDLE_RESOURCE);
        if (neverDecode.contains(protocol)) {
            return urlPath;
        }
        final String cleanPath = new URI(urlPath).getPath();
        if (new File(cleanPath).exists()) {
            // if URL-encoded file exists, don't decode it
            return cleanPath;
        }
        return URLDecoder.decode(urlPath, StandardCharsets.UTF_8.name());
    }

    private void loadImplementationsInBundle(final Test test, final String packageName) {
        final BundleWiring wiring = FrameworkUtil.getBundle(ResolverUtil.class).adapt(BundleWiring.class);
        final Collection<String> list =
                wiring.listResources(packageName, "*.class", BundleWiring.LISTRESOURCES_RECURSE);
        for (final String name : list) {
            addIfMatching(test, name);
        }
    }

    /**
     * Finds matches in a physical directory on a file system. Examines all files within a directory - if the File object
     * is not a directory, and ends with <i>.class</i> the file is loaded and tested to see if it is acceptable
     * according to the Test. Operates recursively to find classes within a folder structure matching the package
     * structure.
     *
     * @param test
     *        a Test used to filter the classes that are discovered
     * @param parent
     *        the package name up to this directory in the package hierarchy. E.g. if /classes is in the classpath and
     *        we wish to examine files in /classes/org/apache then the values of <i>parent</i> would be
     *        <i>org/apache</i>
     * @param location
     *        a File object representing a directory
     */
    private void loadImplementationsInDirectory(final Test test, final String parent, final File location) {
        final File[] files = location.listFiles();
        if (files == null) {
            return;
        }

        StringBuilder builder;
        for (final File file : files) {
            builder = new StringBuilder();
            builder.append(parent).append('/').append(file.getName());
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
     * Finds matching classes within a jar files that contains a folder structure matching the package structure. If the
     * File is not a JarFile or does not exist a warning will be logged, but no error will be raised.
     *
     * @param test
     *        a Test used to filter the classes that are discovered
     * @param parent
     *        the parent package under which classes must be in order to be considered
     * @param url
     *        the url that identifies the jar containing the resource.
     */
    private void loadImplementationsInJar(final Test test, final String parent, final URL url) {
        JarURLConnection connection = null;
        try {
            connection = (JarURLConnection) url.openConnection();
            if (connection != null) {
                // A "jar:" URL file remains open after the stream is closed, so do not cache it.
                connection.setUseCaches(false);
                try (final JarFile jarFile = connection.getJarFile()) {
                    final Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        final JarEntry entry = entries.nextElement();
                        final String name = entry.getName();
                        if (!entry.isDirectory() && name.startsWith(parent) && isTestApplicable(test, name)) {
                            addIfMatching(test, name);
                        }
                    }
                }
            } else {
                LOGGER.error("Could not establish connection to {}", url.toString());
            }
        } catch (final IOException ex) {
            LOGGER.error(
                    "Could not search JAR file '{}' for classes matching criteria {}, file not found",
                    url.toString(),
                    test,
                    ex);
        }
    }

    /**
     * Finds matching classes within a jar files that contains a folder structure matching the package structure. If the
     * File is not a JarFile or does not exist a warning will be logged, but no error will be raised.
     *
     * @param test
     *        a Test used to filter the classes that are discovered
     * @param parent
     *        the parent package under which classes must be in order to be considered
     * @param jarFile
     *        the jar file to be examined for classes
     */
    private void loadImplementationsInJar(final Test test, final String parent, final File jarFile) {
        JarInputStream jarStream = null;
        try {
            jarStream = new JarInputStream(new FileInputStream(jarFile));
            loadImplementationsInJar(test, parent, jarFile.getPath(), jarStream);
        } catch (final IOException ex) {
            LOGGER.error(
                    "Could not search JAR file '{}' for classes matching criteria {}, file not found",
                    jarFile,
                    test,
                    ex);
        } finally {
            close(jarStream, jarFile);
        }
    }

    /**
     * @param jarStream
     * @param source
     */
    private void close(final JarInputStream jarStream, final Object source) {
        if (jarStream != null) {
            try {
                jarStream.close();
            } catch (final IOException e) {
                LOGGER.error("Error closing JAR file stream for {}", source, e);
            }
        }
    }

    /**
     * Finds matching classes within a jar files that contains a folder structure matching the package structure. If the
     * File is not a JarFile or does not exist a warning will be logged, but no error will be raised.
     *
     * @param test
     *        a Test used to filter the classes that are discovered
     * @param parent
     *        the parent package under which classes must be in order to be considered
     * @param stream
     *        The jar InputStream
     */
    private void loadImplementationsInJar(
            final Test test, final String parent, final String path, final JarInputStream stream) {

        try {
            JarEntry entry;

            while ((entry = stream.getNextJarEntry()) != null) {
                final String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith(parent) && isTestApplicable(test, name)) {
                    addIfMatching(test, name);
                }
            }
        } catch (final IOException ioe) {
            LOGGER.error(
                    "Could not search JAR file '{}' for classes matching criteria {} due to an IOException",
                    path,
                    test,
                    ioe);
        }
    }

    /**
     * Add the class designated by the fully qualified class name provided to the set of resolved classes if and only if
     * it is approved by the Test supplied.
     *
     * @param test
     *        the test used to determine if the class matches
     * @param fqn
     *        the fully qualified name of a class
     */
    protected void addIfMatching(final Test test, final String fqn) {
        try {
            final ClassLoader loader = getClassLoader();
            if (test.doesMatchClass()) {
                final String externalName = fqn.substring(0, fqn.indexOf('.')).replace('/', '.');
                LOGGER.debug("Checking to see if class {} matches criteria {}", externalName, test);

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
            LOGGER.warn("Could not examine class {}", fqn, t);
        }
    }

    /**
     * A simple interface that specifies how to test classes to determine if they are to be included in the results
     * produced by the ResolverUtil.
     */
    public interface Test {
        /**
         * Will be called repeatedly with candidate classes. Must return True if a class is to be included in the
         * results, false otherwise.
         *
         * @param type
         *        The Class to match against.
         * @return true if the Class matches.
         */
        boolean matches(Class<?> type);

        /**
         * Test for a resource.
         *
         * @param resource
         *        The URI to the resource.
         * @return true if the resource matches.
         */
        boolean matches(URI resource);

        boolean doesMatchClass();

        boolean doesMatchResource();
    }
}
