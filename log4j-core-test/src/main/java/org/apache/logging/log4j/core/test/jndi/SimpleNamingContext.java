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
package org.apache.logging.log4j.core.test.jndi;

import java.util.Map;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;

/**
 * Minimal in-memory {@link Context} for unit tests.
 */
@SuppressWarnings("BanJNDI")
final class SimpleNamingContext implements Context {

    private final Map<String, Object> bindings;

    SimpleNamingContext(final Map<String, Object> bindings) {
        this.bindings = bindings;
    }

    @Override
    public Object lookup(final Name name) throws NamingException {
        return lookup(name.toString());
    }

    @Override
    public Object lookup(final String name) throws NamingException {
        if (!bindings.containsKey(name)) {
            throw new NameNotFoundException(name);
        }
        return bindings.get(name);
    }

    @Override
    public void bind(final Name name, final Object obj) throws NamingException {
        bind(name.toString(), obj);
    }

    @Override
    public void bind(final String name, final Object obj) throws NamingException {
        bindings.put(name, obj);
    }

    @Override
    public void rebind(final Name name, final Object obj) throws NamingException {
        rebind(name.toString(), obj);
    }

    @Override
    public void rebind(final String name, final Object obj) throws NamingException {
        bindings.put(name, obj);
    }

    @Override
    public void unbind(final Name name) throws NamingException {
        unbind(name.toString());
    }

    @Override
    public void unbind(final String name) throws NamingException {
        bindings.remove(name);
    }

    @Override
    public void rename(final Name oldName, final Name newName) throws NamingException {
        rename(oldName.toString(), newName.toString());
    }

    @Override
    public void rename(final String oldName, final String newName) throws NamingException {
        final Object value = lookup(oldName);
        unbind(oldName);
        bind(newName, value);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(final Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public NamingEnumeration<NameClassPair> list(final String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public NamingEnumeration<Binding> listBindings(final Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public NamingEnumeration<Binding> listBindings(final String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void destroySubcontext(final Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void destroySubcontext(final String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Context createSubcontext(final Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Context createSubcontext(final String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Object lookupLink(final Name name) throws NamingException {
        return lookup(name);
    }

    @Override
    public Object lookupLink(final String name) throws NamingException {
        return lookup(name);
    }

    @Override
    public NameParser getNameParser(final Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public NameParser getNameParser(final String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Name composeName(final Name name, final Name prefix) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public String composeName(final String name, final String prefix) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Object addToEnvironment(final String propName, final Object propVal) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Object removeFromEnvironment(final String propName) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public java.util.Hashtable<?, ?> getEnvironment() throws NamingException {
        return new java.util.Hashtable<>();
    }

    @Override
    public void close() throws NamingException {}

    @Override
    public String getNameInNamespace() throws NamingException {
        throw new NotContextException();
    }
}
