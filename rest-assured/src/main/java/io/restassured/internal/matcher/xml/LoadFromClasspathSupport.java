/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.restassured.internal.matcher.xml;

import java.io.InputStream;

public class LoadFromClasspathSupport {

    /**
     * Loads a resource from the thread context class loader, falling back to the class loader that loaded REST Assured.
     * A leading "/" in the path is optional.
     *
     * @return the resource stream, or <code>null</code> if the resource couldn't be found.
     */
    public static InputStream loadFromClasspath(String path) {
        InputStream stream = loadFromClassLoaders(path);
        if (stream == null && path.startsWith("/")) {
            // Class loaders don't accept a leading "/" in resource names
            stream = loadFromClassLoaders(path.substring(1));
        }
        return stream;
    }

    private static InputStream loadFromClassLoaders(String path) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        InputStream stream = contextClassLoader == null ? null : contextClassLoader.getResourceAsStream(path);
        if (stream == null) {
            stream = LoadFromClasspathSupport.class.getClassLoader().getResourceAsStream(path);
        }
        return stream;
    }
}
