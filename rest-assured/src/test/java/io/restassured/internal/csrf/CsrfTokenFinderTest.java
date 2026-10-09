/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.restassured.internal.csrf;

import io.restassured.builder.ResponseBuilder;
import io.restassured.config.CsrfConfig;
import io.restassured.config.CsrfConfig.CsrfPrioritization;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.config.CsrfConfig.CsrfPrioritization.FORM;
import static io.restassured.config.CsrfConfig.CsrfPrioritization.HEADER;
import static io.restassured.config.CsrfConfig.csrfConfig;
import static org.assertj.core.api.Assertions.assertThat;

class CsrfTokenFinderTest {

    private static final String FORM_ONLY = "<html><head><title>Login</title></head><body>" +
            "<form action=\"login\" method=\"POST\">" +
            "<input type=\"text\" name=\"user\"/>" +
            "<input type=\"hidden\" name=\"_csrf\" value=\"form-token\"/>" +
            "</form></body></html>";

    private static final String META_ONLY = "<html><head><title>Login</title>" +
            "<meta name=\"_csrf_header\" content=\"header-token\"/>" +
            "</head><body><p>no form here</p></body></html>";

    private static final String BOTH = "<html><head><title>Login</title>" +
            "<meta name=\"_csrf_header\" content=\"header-token\"/>" +
            "</head><body><form action=\"login\" method=\"POST\">" +
            "<input type=\"hidden\" name=\"_csrf\" value=\"form-token\"/>" +
            "</form></body></html>";

    private static final String NEITHER = "<html><head><title>Login</title></head><body><p>nothing</p></body></html>";

    @Test
    void finds_form_token_with_form_prioritization() {
        CsrfData data = CsrfTokenFinder.findInHtml(csrfConfig().csrfPrioritization(FORM), html(FORM_ONLY));

        assertCsrfData(data, "_csrf", "form-token", FORM);
    }

    @Test
    void finds_form_token_with_header_prioritization_when_page_has_no_meta_tag() {
        CsrfData data = CsrfTokenFinder.findInHtml(csrfConfig().csrfPrioritization(HEADER), html(FORM_ONLY));

        assertCsrfData(data, "_csrf", "form-token", FORM);
    }

    @Test
    void finds_header_token_with_header_prioritization() {
        CsrfData data = CsrfTokenFinder.findInHtml(csrfConfig().csrfPrioritization(HEADER), html(META_ONLY));

        assertCsrfData(data, "X-CSRF-TOKEN", "header-token", HEADER);
    }

    @Test
    void finds_header_token_with_form_prioritization_when_page_has_no_input_field() {
        CsrfData data = CsrfTokenFinder.findInHtml(csrfConfig().csrfPrioritization(FORM), html(META_ONLY));

        assertCsrfData(data, "X-CSRF-TOKEN", "header-token", HEADER);
    }

    @Test
    void default_config_prioritizes_header_when_page_contains_both() {
        CsrfData data = CsrfTokenFinder.findInHtml(new CsrfConfig(), html(BOTH));

        assertCsrfData(data, "X-CSRF-TOKEN", "header-token", HEADER);
    }

    @Test
    void form_prioritization_picks_form_when_page_contains_both() {
        CsrfData data = CsrfTokenFinder.findInHtml(csrfConfig().csrfPrioritization(FORM), html(BOTH));

        assertCsrfData(data, "_csrf", "form-token", FORM);
    }

    @Test
    void returns_null_when_page_contains_neither() {
        assertThat(CsrfTokenFinder.findInHtml(csrfConfig().csrfPrioritization(HEADER), html(NEITHER))).isNull();
        assertThat(CsrfTokenFinder.findInHtml(csrfConfig().csrfPrioritization(FORM), html(NEITHER))).isNull();
    }

    @Test
    void returns_null_when_body_is_not_html() {
        assertThat(CsrfTokenFinder.findInHtml(new CsrfConfig(), html("{\"a\":1}"))).isNull();
    }

    @Test
    void uses_configured_input_field_meta_tag_and_header_names() {
        String page = "<html><head><meta name=\"my-meta\" content=\"meta-value\"/></head><body>" +
                "<form><input type=\"hidden\" name=\"my-field\" value=\"field-value\"/>" +
                "<input type=\"hidden\" name=\"_csrf\" value=\"ignored\"/></form></body></html>";
        CsrfConfig formConfig = csrfConfig().csrfInputFieldName("my-field").csrfPrioritization(FORM);
        CsrfConfig headerConfig = csrfConfig().csrfInputFieldName("my-field").csrfMetaTagName("my-meta").csrfHeaderName("X-My-Header").csrfPrioritization(HEADER);

        assertCsrfData(CsrfTokenFinder.findInHtml(formConfig, html(page)), "my-field", "field-value", FORM);
        assertCsrfData(CsrfTokenFinder.findInHtml(headerConfig, html(page)), "X-My-Header", "meta-value", HEADER);
    }

    @Test
    void finds_the_configured_input_field_when_a_meta_tag_name_is_configured_afterwards() {
        String page = "<html><head><meta name=\"my-meta\" content=\"meta-value\"/></head><body>" +
                "<form><input type=\"hidden\" name=\"my-field\" value=\"field-value\"/></form></body></html>";
        CsrfConfig config = csrfConfig().csrfInputFieldName("my-field").csrfMetaTagName("my-meta").csrfPrioritization(FORM);

        assertCsrfData(CsrfTokenFinder.findInHtml(config, html(page)), "my-field", "field-value", FORM);
    }

    @Test
    void uses_first_input_field_when_several_have_the_csrf_name() {
        String page = "<html><body><form>" +
                "<input type=\"hidden\" name=\"_csrf\" value=\"first\"/>" +
                "<input type=\"hidden\" name=\"_csrf\" value=\"second\"/>" +
                "</form></body></html>";

        assertCsrfData(CsrfTokenFinder.findInHtml(csrfConfig().csrfPrioritization(FORM), html(page)), "_csrf", "first", FORM);
    }

    @Test
    void input_field_without_value_attribute_is_treated_as_no_token() {
        String page = "<html><body><form><input type=\"hidden\" name=\"_csrf\"/></form></body></html>";

        assertThat(CsrfTokenFinder.findInHtml(csrfConfig().csrfPrioritization(FORM), html(page))).isNull();
    }

    @Test
    void input_field_with_empty_value_yields_empty_token() {
        String page = "<html><body><form><input type=\"hidden\" name=\"_csrf\" value=\"\"/></form></body></html>";

        assertCsrfData(CsrfTokenFinder.findInHtml(csrfConfig().csrfPrioritization(FORM), html(page)), "_csrf", "", FORM);
    }

    @Test
    void ignores_input_field_that_is_not_an_input_tag() {
        String page = "<html><body><form><textarea name=\"_csrf\">nope</textarea></form></body></html>";

        assertThat(CsrfTokenFinder.findInHtml(csrfConfig().csrfPrioritization(FORM), html(page))).isNull();
    }

    private static Response html(String body) {
        return new ResponseBuilder().setStatusCode(200).setContentType("text/html").setBody(body).build();
    }

    private static void assertCsrfData(CsrfData data, String name, String token, CsrfPrioritization prioritization) {
        assertThat(data).isNotNull();
        assertThat(data.inputFieldOrHeaderName).isEqualTo(name);
        assertThat(data.token).isEqualTo(token);
        assertThat(data.csrfPrioritization).isEqualTo(prioritization);
    }
}
