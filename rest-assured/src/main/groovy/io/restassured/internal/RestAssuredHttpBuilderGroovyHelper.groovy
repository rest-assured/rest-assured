package io.restassured.internal

import org.codehaus.groovy.runtime.InvokerInvocationException

class RestAssuredHttpBuilderGroovyHelper {

  static Collection<String> flattenToString(Collection collection) {
    return collection.flatten().collect {
      it?.toString()
    }
  }

  static Closure createClosureThatCalls(assertionClosure) {
    return { response, content ->
      try {
        assertionClosure.call(response, content)
      } catch (InvokerInvocationException e) {
        def cause = e.getCause()
        if (cause != null) {
          throw cause
        }
        throw e
      }
    }
  }
}
