/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.commcare.api.persistence;

import org.commcare.api.util.NameHasher;
import org.javarosa.core.util.externalizable.CannotCreateObjectException;
import org.javarosa.core.util.externalizable.Hasher;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.util.Arrays;

/**
 * ProtoType factory for serializing and deserializing persisted classes using
 * their hash codes. To use a non-default hasher, use one of the overriding constructors
 * or call setStaticHasher().
 */

public class NamedPrototypeFactory extends PrototypeFactory{

    static Hasher mStaticHasher;

    public NamedPrototypeFactory() {
        super();
        mStaticHasher = (mStaticHasher == null ? new NameHasher() : mStaticHasher);
    }


    public Class getClass(byte[] hash) {

        String className = new String(hash);

        System.out.println("Got classname: " + className + " from hash: " + Arrays.toString(hash));

        try {
            Class<?> c = Class.forName(className);
            return c;
        } catch(ClassNotFoundException e) {
            return null;
        }
    }

    public Object getInstance(byte[] hash) {
        return getInstance(getClass(hash));
    }

    public static Object getInstance(Class c) {
        try {
            return c.newInstance();
        } catch (IllegalAccessException iae) {
            throw new CannotCreateObjectException(c.getName() + ": not accessible or no empty constructor");
        } catch (InstantiationException e) {
            throw new CannotCreateObjectException(c.getName() + ": not instantiable");
        }
    }
}
