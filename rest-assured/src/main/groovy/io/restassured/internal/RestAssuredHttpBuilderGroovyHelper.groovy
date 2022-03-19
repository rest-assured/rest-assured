package io.restassured.internal

class RestAssuredHttpBuilderGroovyHelper {

  static Collection<String> flattenToString(Collection collection) {
    return collection.flatten().collect {
      it?.toString()
    }
  }

  static Closure createClosureThatCalls(assertionClosure) {
    return { response, content ->
      assertionClosure.call(response, content)
    }
  }
}
