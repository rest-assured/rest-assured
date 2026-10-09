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

    public static InputStream loadFromClasspath(String path) {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (stream == null) {
            // Kept from the Groovy implementation, where getClass() in this static method returned java.lang.Class.
            // Since Java 9 this only finds resources in the java.base module, which is why the fallback below exists.
            stream = Class.class.getResourceAsStream(path);
        }

        if (stream == null && path.startsWith("/")) {
            // The previous fallback doesn't find resources on Java 9+ so if the path starts with "/" we remove it and try again
            stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path.substring(1));
        }
        return stream;
    }
}
