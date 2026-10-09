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

package io.restassured.path.json;

import groovy.lang.GroovyClassLoader;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * JsonPath must evaluate paths even when the thread context class loader can't see Groovy, which is the normal case
 * in an OSGi container where Groovy is its own bundle (#1933), and still resolve classes that only the thread context
 * class loader can see.
 */
public class JsonPathContextClassLoaderTest {

    private static final String JSON = "{ \"lotto\" : { \"lottoId\" : 5, \"winners\" : [ { \"winnerId\" : 23, \"numbers\" : [2, 45] }, { \"winnerId\" : 54, \"numbers\" : [52, 3] } ] } }";

    @Test public void
    evaluates_path_when_thread_context_class_loader_cannot_see_groovy() throws Exception {
        int lottoId = withContextClassLoaderThatCannotSeeGroovy(() -> new JsonPath(JSON).getInt("lotto.lottoId"));

        assertThat(lottoId, equalTo(5));
    }

    @Test public void
    evaluates_path_with_closure_and_parameters_when_thread_context_class_loader_cannot_see_groovy() throws Exception {
        List<Integer> winnerIds = withContextClassLoaderThatCannotSeeGroovy(() ->
                new JsonPath(JSON).param("number", 45).getList("lotto.winners.findAll { it.numbers.contains(number) }.winnerId", Integer.class));

        assertThat(winnerIds, contains(23));
    }

    @Test public void
    resolves_classes_in_path_that_only_the_thread_context_class_loader_can_see() throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try (GroovyClassLoader contextClassLoader = new GroovyClassLoader(original)) {
            contextClassLoader.parseClass("class OnlyInContextClassLoader { static int MIN = 40 }");
            thread.setContextClassLoader(contextClassLoader);

            List<Integer> values = new JsonPath(JSON).getList("lotto.winners.collect { OnlyInContextClassLoader.MIN }", Integer.class);

            assertThat(values, contains(40, 40));
        } finally {
            thread.setContextClassLoader(original);
        }
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
