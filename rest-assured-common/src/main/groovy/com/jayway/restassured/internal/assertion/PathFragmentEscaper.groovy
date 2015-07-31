package com.jayway.restassured.internal.assertion

/**
 * Escapes a path fragment if required
 */
interface PathFragmentEscaper {

  boolean shouldEscape(String pathFragment)

  String escape(String pathFragment)

}