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

package io.restassured.internal;

import groovy.lang.GroovyRuntimeException;
import io.restassured.RestAssured;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.specification.RequestSpecification;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * A request rethrows the exception that a filter, or sending the request, throws, without the wrappers that reflection,
 * proxies and Groovy put around it.
 */
class RequestExceptionUnwrappingTest {

    private final IOException cause = new IOException("boom");

    @AfterEach
    void resetRestAssured() {
        RestAssured.reset();
    }

    @Test
    void a_checked_exception_from_sending_the_request_is_thrown_as_is() {
        Throwable thrown = catchThrowable(() -> given().port(1).get("/x"));

        assertThat(thrown).isInstanceOf(ConnectException.class);
    }

    @Test
    void wrappers_are_removed() {
        assertThat(thrownBy(new UndeclaredThrowableException(cause))).isSameAs(cause);
        assertThat(thrownBy(new InvocationTargetException(cause))).isSameAs(cause);
        assertThat(thrownBy(new GroovyRuntimeException("wrapped", cause))).isSameAs(cause);
        assertThat(thrownBy(new InvokerInvocationException(cause))).isSameAs(cause);
        assertThat(thrownBy(new UndeclaredThrowableException(new InvocationTargetException(new GroovyRuntimeException("wrapped", cause))))).isSameAs(cause);
    }

    @Test
    void wrappers_are_removed_for_every_way_of_sending_a_request() {
        Throwable wrapped = new UndeclaredThrowableException(cause);
        assertThat(catchThrowable(() -> failingWith(wrapped).get("/x", Collections.emptyMap()))).isSameAs(cause);
        assertThat(catchThrowable(() -> failingWith(wrapped).post("/x"))).isSameAs(cause);
        assertThat(catchThrowable(() -> failingWith(wrapped).request("PURGE", "/x"))).isSameAs(cause);
        assertThat(catchThrowable(() -> failingWith(wrapped).query("/x", Collections.emptyMap()))).isSameAs(cause);
    }

    @Test
    void wrappers_without_cause_and_other_exceptions_are_kept() {
        UndeclaredThrowableException withoutCause = new UndeclaredThrowableException(null);
        GroovyRuntimeException groovyWithoutCause = new GroovyRuntimeException("no cause");
        IllegalStateException other = new IllegalStateException("other", cause);
        UndeclaredThrowableException wrappingOther = new UndeclaredThrowableException(other);

        assertThat(thrownBy(withoutCause)).isSameAs(withoutCause);
        assertThat(thrownBy(groovyWithoutCause)).isSameAs(groovyWithoutCause);
        assertThat(thrownBy(other)).isSameAs(other);
        assertThat(thrownBy(wrappingOther)).isSameAs(other);
    }

    private static Throwable thrownBy(Throwable exception) {
        return catchThrowable(() -> failingWith(exception).get("/x"));
    }

    private static RequestSpecification failingWith(Throwable exception) {
        return given().filter((requestSpec, responseSpec, ctx) -> SafeExceptionRethrower.safeRethrow(exception));
    }
}
