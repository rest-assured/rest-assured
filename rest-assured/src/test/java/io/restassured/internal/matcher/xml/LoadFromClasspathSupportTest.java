/*
 * Copyright 2026 the original author or authors.
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

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LoadFromClasspathSupportTest {

    @Test public void
    loads_resource_without_leading_slash() throws Exception {
        assertThat(read(LoadFromClasspathSupport.loadFromClasspath("xml-matchers/greeting.dtd"))).contains("<!ELEMENT greeting (name)>");
    }

    @Test public void
    loads_resource_with_leading_slash() throws Exception {
        assertThat(read(LoadFromClasspathSupport.loadFromClasspath("/xml-matchers/greeting.dtd"))).contains("<!ELEMENT greeting (name)>");
    }

    @Test public void
    returns_null_when_resource_is_not_found() {
        assertThat(LoadFromClasspathSupport.loadFromClasspath("xml-matchers/missing.dtd")).isNull();
        assertThat(LoadFromClasspathSupport.loadFromClasspath("/xml-matchers/missing.dtd")).isNull();
    }

    @Test public void
    throws_npe_when_path_is_null() {
        assertThatThrownBy(() -> LoadFromClasspathSupport.loadFromClasspath(null)).isInstanceOf(NullPointerException.class);
    }

    @Test public void
    falls_back_to_rest_assureds_class_loader_when_context_class_loader_cannot_find_the_resource() throws Exception {
        ClassLoader emptyClassLoader = new URLClassLoader(new URL[0], null);

        assertThat(read(withContextClassLoader(emptyClassLoader, () -> LoadFromClasspathSupport.loadFromClasspath("xml-matchers/greeting.dtd")))).contains("<!ELEMENT greeting (name)>");
        assertThat(read(withContextClassLoader(emptyClassLoader, () -> LoadFromClasspathSupport.loadFromClasspath("/xml-matchers/greeting.dtd")))).contains("<!ELEMENT greeting (name)>");
        assertThat(withContextClassLoader(emptyClassLoader, () -> LoadFromClasspathSupport.loadFromClasspath("xml-matchers/missing.dtd"))).isNull();
    }

    @Test public void
    loads_resource_when_context_class_loader_is_null() throws Exception {
        assertThat(read(withContextClassLoader(null, () -> LoadFromClasspathSupport.loadFromClasspath("xml-matchers/greeting.dtd")))).contains("<!ELEMENT greeting (name)>");
        assertThat(read(withContextClassLoader(null, () -> LoadFromClasspathSupport.loadFromClasspath("/xml-matchers/greeting.dtd")))).contains("<!ELEMENT greeting (name)>");
        assertThat(withContextClassLoader(null, () -> LoadFromClasspathSupport.loadFromClasspath("xml-matchers/missing.dtd"))).isNull();
    }

    @Test public void
    resolves_paths_from_the_classpath_root_and_not_relative_to_a_package() {
        assertThat(LoadFromClasspathSupport.loadFromClasspath("LoadFromClasspathSupport.class")).isNull();
    }

    private static <T> T withContextClassLoader(ClassLoader classLoader, Supplier<T> supplier) {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try {
            return supplier.get();
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    private static String read(InputStream stream) throws Exception {
        assertThat(stream).isNotNull();
        try (InputStream s = stream) {
            return new String(s.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
