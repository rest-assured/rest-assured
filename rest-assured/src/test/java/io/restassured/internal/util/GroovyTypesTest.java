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

package io.restassured.internal.util;

import groovy.lang.Closure;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.lang.MissingPropertyException;
import org.codehaus.groovy.runtime.GStringImpl;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.codehaus.groovy.runtime.MethodClosure;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GroovyTypesTest {

    @Test
    void recognizes_gstrings_and_their_subclasses() {
        assertThat(GroovyTypes.isGString(new GStringImpl(new Object[]{"b"}, new String[]{"a", "c"}))).isTrue();
        assertThat(GroovyTypes.isGString(new GStringImpl(new Object[]{"b"}, new String[]{"a", "c"}) {
        })).isTrue();
        assertThat(GroovyTypes.isGString(new GroovyShell().evaluate("def b = 'b'; return \"a${b}c\""))).isTrue();
    }

    @Test
    void other_char_sequences_are_not_gstrings() {
        assertThat(GroovyTypes.isGString("abc")).isFalse();
        assertThat(GroovyTypes.isGString(new StringBuilder("abc"))).isFalse();
        assertThat(GroovyTypes.isGString(null)).isFalse();
    }

    @Test
    void recognizes_closures() {
        assertThat(GroovyTypes.isClosure(new GroovyShell().evaluate("return { it }"))).isTrue();
        assertThat(GroovyTypes.isClosure(new MethodClosure("abc", "length"))).isTrue();
        assertThat(GroovyTypes.isClosure(Closure.IDENTITY)).isTrue();
    }

    @Test
    void other_objects_are_not_closures() {
        assertThat(GroovyTypes.isClosure((Runnable) () -> {
        })).isFalse();
        assertThat(GroovyTypes.isClosure("abc")).isFalse();
        assertThat(GroovyTypes.isClosure(null)).isFalse();
    }

    @Test
    void recognizes_groovy_runtime_exceptions_and_their_subclasses() {
        assertThat(GroovyTypes.isGroovyRuntimeException(new GroovyRuntimeException("a"))).isTrue();
        assertThat(GroovyTypes.isGroovyRuntimeException(new MissingPropertyException("a"))).isTrue();
        assertThat(GroovyTypes.isGroovyRuntimeException(new InvokerInvocationException(new RuntimeException()))).isTrue();
    }

    @Test
    void other_objects_are_not_groovy_runtime_exceptions() {
        assertThat(GroovyTypes.isGroovyRuntimeException(new RuntimeException("a"))).isFalse();
        assertThat(GroovyTypes.isGroovyRuntimeException(null)).isFalse();
    }
}
