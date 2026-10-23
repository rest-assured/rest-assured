package io.restassured.internal.csrf;

import io.restassured.config.CsrfConfig;
import io.restassured.internal.support.HtmlDocument;
import io.restassured.response.Response;

import static io.restassured.config.CsrfConfig.CsrfPrioritization.FORM;
import static io.restassured.config.CsrfConfig.CsrfPrioritization.HEADER;

public class CsrfTokenFinder {

    public static CsrfData findInHtml(CsrfConfig csrfConfig, Response pageThatContainsCsrfToken) {
        return findInHtml(csrfConfig, HtmlDocument.parse(pageThatContainsCsrfToken.asString()));
    }

    public static CsrfData findInHtml(CsrfConfig csrfConfig, HtmlDocument html) {
        if (csrfConfig.isCsrfPrioritization(HEADER)) {
            CsrfData csrfData = findCsrfHeaderToken(csrfConfig, html);
            return csrfData != null ? csrfData : findCsrfFormToken(csrfConfig, html);
        } else {
            CsrfData csrfData = findCsrfFormToken(csrfConfig, html);
            return csrfData != null ? csrfData : findCsrfHeaderToken(csrfConfig, html);
        }
    }

    private static CsrfData findCsrfFormToken(CsrfConfig csrfConfig, HtmlDocument html) {
        String csrfFieldName = csrfConfig.getCsrfInputFieldName();
        String csrfToken = html.inputsNamed(csrfFieldName).findFirst().map(input -> input.attribute("value")).orElse(null);
        if (csrfToken == null) {
            return null;
        }
        return new CsrfData(csrfFieldName, csrfToken, FORM);
    }

    private static CsrfData findCsrfHeaderToken(CsrfConfig csrfConfig, HtmlDocument html) {
        String metaTagName = csrfConfig.getCsrfMetaTagName();
        String csrfToken = html.elements("meta").filter(meta -> metaTagName.equals(meta.attribute("name"))).findFirst()
                .map(meta -> meta.attribute("content")).orElse(null);
        if (csrfToken == null) {
            return null;
        }
        return new CsrfData(csrfConfig.getCsrfHeaderName(), csrfToken, HEADER);
    }
}
