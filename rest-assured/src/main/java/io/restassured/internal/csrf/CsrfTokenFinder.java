package io.restassured.internal.csrf;

import io.restassured.config.CsrfConfig;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.Response;

import java.util.function.Supplier;

import static io.restassured.config.CsrfConfig.CsrfPrioritization.FORM;
import static io.restassured.config.CsrfConfig.CsrfPrioritization.HEADER;
import static java.lang.String.format;

public class CsrfTokenFinder {
    private static final String FIND_INPUT_FIELD_WITH_NAME = "html.depthFirst().grep { it.name() == 'input' && it.@name == '%s' }.collect { it.@value }.get(0)";
    private static final String FIND_META_FIELD_WITH_NAME = "html.depthFirst().grep { it.name() == 'meta' && it.@name == '%s' }.collect { it.@content }.get(0)";

    public static CsrfData findInHtml(CsrfConfig csrfConfig, Response pageThatContainsCsrfToken) {
        XmlPath htmlPath = pageThatContainsCsrfToken.htmlPath();

        if (csrfConfig.isCsrfPrioritization(HEADER)) {
            CsrfData csrfData = findCsrfHeaderToken(csrfConfig, htmlPath);
            return csrfData != null ? csrfData : findCsrfFormToken(csrfConfig, htmlPath);
        } else {
            CsrfData csrfData = findCsrfFormToken(csrfConfig, htmlPath);
            return csrfData != null ? csrfData : findCsrfHeaderToken(csrfConfig, htmlPath);
        }
    }

    private static CsrfData findCsrfFormToken(CsrfConfig csrfConfig, XmlPath htmlPath) {
        String csrfFieldName = csrfConfig.getCsrfInputFieldName();
        String csrfToken = nullIfException(() -> htmlPath.getString(format(FIND_INPUT_FIELD_WITH_NAME, csrfFieldName)));
        if (csrfToken == null) {
            return null;
        }
        return new CsrfData(csrfFieldName, csrfToken, FORM);
    }

    private static CsrfData findCsrfHeaderToken(CsrfConfig csrfConfig, XmlPath htmlPath) {
        String metaTagName = csrfConfig.getCsrfMetaTagName();
        String csrfToken = nullIfException(() -> htmlPath.getString(format(FIND_META_FIELD_WITH_NAME, metaTagName)));
        if (csrfToken == null) {
            return null;
        }
        return new CsrfData(csrfConfig.getCsrfHeaderName(), csrfToken, HEADER);
    }

    private static String nullIfException(Supplier<String> supplier) {
        try {
            return supplier.get();
        } catch (Exception ignored) {
            return null;
        }
    }
}
