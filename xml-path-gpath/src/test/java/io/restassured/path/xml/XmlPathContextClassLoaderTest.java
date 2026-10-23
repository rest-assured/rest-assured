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

package io.restassured.path.xml;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * XmlPath must evaluate paths even when the thread context class loader can't see Groovy, which is the normal case
 * in an OSGi container where Groovy is its own bundle (#1933).
 */
public class XmlPathContextClassLoaderTest {

    private static final String XML = "<shopping><category type=\"groceries\"><item><name>Chocolate</name><price>10</price></item>" +
            "<item><name>Coffee</name><price>20</price></item></category></shopping>";

    @Test public void
    evaluates_path_when_thread_context_class_loader_cannot_see_groovy() throws Exception {
        String name = withContextClassLoaderThatCannotSeeGroovy(() -> new XmlPath(XML).getString("shopping.category.item[0].name"));

        assertThat(name, equalTo("Chocolate"));
    }

    @Test public void
    evaluates_path_with_closure_and_parameters_when_thread_context_class_loader_cannot_see_groovy() throws Exception {
        List<String> names = withContextClassLoaderThatCannotSeeGroovy(() ->
                new XmlPath(XML).param("minPrice", 15).getList("shopping.category.item.findAll { it.price.toInteger() > minPrice }.name", String.class));

        assertThat(names, contains("Coffee"));
    }

    private static <T> T withContextClassLoaderThatCannotSeeGroovy(Callable<T> callable) throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try (URLClassLoader withoutGroovy = new URLClassLoader(new URL[0], ClassLoader.getPlatformClassLoader())) {
            assertGroovyIsNotVisibleFrom(withoutGroovy);
            thread.setContextClassLoader(withoutGroovy);
            return callable.call();
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    private static void assertGroovyIsNotVisibleFrom(ClassLoader classLoader) {
        try {
            classLoader.loadClass("groovy.lang.Script");
            throw new AssertionError("Expected groovy.lang.Script not to be visible from " + classLoader);
        } catch (ClassNotFoundException expected) {
            // The class loader models the thread context class loader of a bundle in an OSGi container
        }
    }
}
