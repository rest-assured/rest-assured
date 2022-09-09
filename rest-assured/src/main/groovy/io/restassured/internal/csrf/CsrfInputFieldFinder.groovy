package io.restassured.internal.csrf

import io.restassured.config.CsrfConfig
import io.restassured.response.Response

import static java.lang.String.format

class CsrfInputFieldFinder {
  private static final String FIND_INPUT_TAG_WITH_TYPE = "html.depthFirst().grep { it.name() == 'input' && it.@type == '%s' }.collect { it.@name }"
  private static final String FIND_INPUT_FIELD_WITH_NAME = "html.depthFirst().grep { it.name() == 'input' && it.@name == '%s' }.collect { it.@value }.get(0)"


  static CsrfInputField findInHtml(CsrfConfig csrfConfig, Response pageThatContainsCsrfToken) {
    def htmlPath = pageThatContainsCsrfToken.htmlPath()
    String csrfFieldName = csrfConfig.hasCsrfInputFieldName() ? csrfConfig.csrfInputFieldName : nullIfException {
      htmlPath.getString(format(FIND_INPUT_TAG_WITH_TYPE, "hidden"))
    }
    String csrfValue = nullIfException { htmlPath.getString(format(FIND_INPUT_FIELD_WITH_NAME, csrfFieldName)) }
    if (!csrfValue) {
      throw new IllegalArgumentException("Couldn't find the CSRF input field with name $csrfFieldName in response. Response was:\n${pageThatContainsCsrfToken.prettyPrint()}")
    }
    return new CsrfInputField(csrfFieldName, csrfValue)
  }

  private static def nullIfException(Closure closure) {
    try {
      closure.call()
    } catch (Exception e) {
      null
    }
  }
}
