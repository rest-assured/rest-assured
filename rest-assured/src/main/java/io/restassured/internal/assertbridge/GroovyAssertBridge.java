package io.restassured.internal.assertbridge;

import groovy.lang.GroovyRuntimeException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Supplier;

/**
 * Utility for running Groovy-based validations while preserving the original exception
 * semantics for Java callers.
 * <p>
 * Groovy and various proxy/reflective mechanisms may wrap the real cause in
 * {@link GroovyRuntimeException}, {@link UndeclaredThrowableException},
 * or {@link InvocationTargetException}. This bridge unwraps such containers and
 * then rethrows the underlying cause as-is (using a "sneaky throw" to avoid
 * changing method signatures).
 * <p>
 * Typical usage is at the Java–Groovy boundary:
 * <pre>{@code
 * GroovyAssertBridge.runWithUnwrap(() -> groovySpec.validate(response));
 * }</pre>
 * so that Groovy's internal wrapping does not leak into user-facing APIs or tests.
 */
public final class GroovyAssertBridge {

    private GroovyAssertBridge() {
    }

    /**
     * Execute the given {@link Supplier}, unwrapping common wrapper exceptions so
     * that callers observe the original underlying {@link Throwable}.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code InvokerInvocationException(AssertionError)} → {@code AssertionError}</li>
     *   <li>{@code UndeclaredThrowableException(SSLException)} → {@code SSLException}</li>
     * </ul>
     *
     * @param validator code to execute, typically a call into Groovy-based validation logic
     * @param <T>       return type of the supplier
     * @return the supplier result if no exception is thrown
     */
    public static <T> T runWithUnwrap(Supplier<T> validator) {
        try {
            return validator.get();
        } catch (Throwable e) {
            Throwable unwrapped = unwrap(e);
            return sneakyThrow(unwrapped);
        }
    }

    /**
     * Recursively unwrap known wrapper types until a "real" cause is found.
     */
    private static Throwable unwrap(Throwable t) {
        while (true) {
            Throwable cause = t.getCause();
            if (cause == null || cause == t) {
                return t;
            }
            if (t instanceof GroovyRuntimeException
                    || t instanceof UndeclaredThrowableException
                    || t instanceof InvocationTargetException) {
                t = cause;
            } else {
                return t;
            }
        }
    }

    /**
     * Throw any {@link Throwable} without declaring it, while satisfying the generic
     * return type expected by the caller.
     */
    @SuppressWarnings("unchecked")
    private static <R, E extends Throwable> R sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }
}