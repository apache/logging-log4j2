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
package org.apache.logging.log4j.core.selector;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * ClassLoader that loads class in this package (or sub-package) by hand, otherwise delegating to the TCCL.
 *
 * @since 2.1
 */
public class TestClassLoader extends ClassLoader {

    public TestClassLoader() {
        super(LoaderUtil.getThreadContextClassLoader());
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        if (name.startsWith(getClass().getPackage().getName())) {
            return findClass(name);
        }
        return super.loadClass(name);
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        final String path = name.replace('.', '/').concat(".class");
        final URL resource = super.getResource(path);
        if (resource == null) {
            throw new ClassNotFoundException(name);
        }
        try {
            final URLConnection uc = resource.openConnection();
            final int len = uc.getContentLength();
            final InputStream in = new BufferedInputStream(uc.getInputStream());
            final byte[] bytecode = new byte[len];
            try {
                IOUtils.readFully(in, bytecode);
            } finally {
                Closer.closeSilently(in);
            }
            return defineClass(name, bytecode, 0, bytecode.length);
        } catch (final IOException e) {
            Throwables.rethrow(e);
            return null; // unreachable
        }
    }
}
