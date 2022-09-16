package io.restassured.internal.csrf

import io.restassured.config.CsrfConfig
import io.restassured.path.xml.XmlPath
import io.restassured.response.Response

import static io.restassured.config.CsrfConfig.CsrfPrioritization.FORM
import static io.restassured.config.CsrfConfig.CsrfPrioritization.HEADER
import static java.lang.String.format

class CsrfTokenFinder {
  private static final String FIND_INPUT_FIELD_WITH_NAME = "html.depthFirst().grep { it.name() == 'input' && it.@name == '%s' }.collect { it.@value }.get(0)"
  private static final String FIND_META_FIELD_WITH_NAME = "html.depthFirst().grep { it.name() == 'meta' && it.@name == '%s' }.collect { it.@content }.get(0)"

  static CsrfData findInHtml(CsrfConfig csrfConfig, Response pageThatContainsCsrfToken) {
    def htmlPath = pageThatContainsCsrfToken.htmlPath()

    def csrfFinders
    if (csrfConfig.isCsrfPrioritization(HEADER)) {
      csrfFinders = [CsrfTokenFinder.&findCsrfHeaderToken, CsrfTokenFinder.&findCsrfFormToken]
    } else {
      csrfFinders = [CsrfTokenFinder.&findCsrfFormToken, CsrfTokenFinder.&findCsrfHeaderToken]
    }

    return csrfFinders.inject((CsrfData) null) { csrfToken, fn ->
      csrfToken ?: fn.call(csrfConfig, htmlPath)
    }
  }

  private static findCsrfFormToken(CsrfConfig csrfConfig, XmlPath htmlPath) {
    def csrfFieldName = csrfConfig.getCsrfInputFieldName()
    String csrfToken = nullIfException { htmlPath.getString(format(FIND_INPUT_FIELD_WITH_NAME, csrfFieldName)) }
    if (csrfToken == null) {
      return null
    }
    return new CsrfData(csrfFieldName, csrfToken, FORM)
  }

  private static findCsrfHeaderToken(CsrfConfig csrfConfig, XmlPath htmlPath) {
    def metaTagName = csrfConfig.getCsrfMetaTagName()
    String csrfToken = nullIfException { htmlPath.getString(format(FIND_META_FIELD_WITH_NAME, metaTagName)) }
    if (csrfToken == null) {
      return null
    }
    return new CsrfData(csrfConfig.getCsrfHeaderName(), csrfToken, HEADER)
  }

  private static def nullIfException(Closure closure) {
    try {
      closure.call()
    } catch (Exception ignored) {
      null
    }
  }
}