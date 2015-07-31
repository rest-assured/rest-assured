package com.jayway.restassured.internal.assertion

/**
 * A {@link PathFragmentEscaper} that escapes the path fragment with <code>getAt('<fragment>')</code>
 */
abstract class GetPathFragmentEscaper implements PathFragmentEscaper {

  @Override
  String escape(String pathFragment) {
    return "getAt('" + pathFragment + "')"
  }
}
