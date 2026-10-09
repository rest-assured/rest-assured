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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Supplier;

/**
 * Runs code and rethrows the exception it throws without the wrappers that reflection, proxies and Groovy (which
 * json-path, xml-path and user code written in Groovy still use) put around it, so that callers see the original
 * exception, for example an {@code AssertionError} or an {@code SSLException}.
 */
public final class ExceptionUnwrapper {

    private ExceptionUnwrapper() {
    }

    /**
     * Run {@code supplier} and rethrow what it throws, unwrapped from {@link UndeclaredThrowableException},
     * {@link InvocationTargetException} and {@code groovy.lang.GroovyRuntimeException} (including its subclasses),
     * without declaring it.
     */
    public static <T> T runWithUnwrap(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            return SafeExceptionRethrower.safeRethrow(unwrap(e));
        }
    }

    private static Throwable unwrap(Throwable t) {
        while (true) {
            Throwable cause = t.getCause();
            if (cause == null || cause == t || !isWrapper(t)) {
                return t;
            }
            t = cause;
        }
    }

    private static boolean isWrapper(Throwable t) {
        return t instanceof UndeclaredThrowableException || t instanceof InvocationTargetException || GroovyTypes.isGroovyRuntimeException(t);
    }
}
