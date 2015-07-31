package com.jayway.restassured.internal.assertion

/**
 * A {@link PathFragmentEscaper} that escapes the path fragment with quotes
 */
abstract class QuoteFragmentEscaper implements PathFragmentEscaper {

  @Override
  String escape(String pathFragment) {
    return "'" + pathFragment + "'"
  }
}
