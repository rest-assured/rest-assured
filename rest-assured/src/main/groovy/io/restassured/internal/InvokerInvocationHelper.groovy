package io.restassured.internal

import org.codehaus.groovy.runtime.InvokerInvocationException

final class InvokerInvocationHelper {

    static rethrowInvokerInvocationException(InvokerInvocationException e) {
        def cause = e.getCause()
        if (cause != null) {
            throw cause
        }
        throw e
    }
}
