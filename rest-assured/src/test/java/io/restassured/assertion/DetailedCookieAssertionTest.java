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

package io.restassured.assertion;

import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.restassured.matcher.RestAssuredMatchers.detailedCookie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;

@SuppressWarnings({"unchecked", "RedundantCast"})
public class DetailedCookieAssertionTest {

    private static final Date EXPIRY_DATE = new Date(1700000000000L);

    private static Map<String, Object> validate(Matcher<? super Cookie> matcher, List<String> setCookieHeaders, Cookies responseCookies) {
        DetailedCookieAssertion assertion = new DetailedCookieAssertion();
        assertion.setCookieName("name");
        assertion.setMatcher(matcher);
        return (Map<String, Object>) assertion.validateCookies(setCookieHeaders, responseCookies);
    }

    @Test
    public void returns_success_and_empty_error_message_when_cookie_in_set_cookie_header_matches() {
        Map<String, Object> result = validate(detailedCookie().value("value").path("/p"), Collections.singletonList("name=value; Path=/p"), new Cookies());

        assertThat(result).containsOnlyKeys("success", "errorMessage");
        assertThat(result.get("success")).isEqualTo(true);
        assertThat(String.valueOf(result.get("errorMessage"))).isEmpty();
    }

    @Test
    public void reports_expected_description_and_mismatch_when_cookie_does_not_match() {
        Map<String, Object> result = validate(detailedCookie().value("other"), Collections.singletonList("name=value; Path=/p"), new Cookies());

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(String.valueOf(result.get("errorMessage")))
                .isEqualTo("Expected cookie \"name\" was not (not null and hasProperty(\"value\", \"other\")), hasProperty(\"value\", \"other\")  property 'value' was \"value\".\n");
    }

    @Test
    public void falls_back_to_response_cookies_when_cookie_is_not_in_set_cookie_header() {
        Cookies responseCookies = new Cookies(new Cookie.Builder("name", "fromResponse").build());

        Map<String, Object> result = validate(detailedCookie().value("fromResponse"), Collections.emptyList(), responseCookies);

        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    public void takes_expiry_date_from_response_cookie_when_set_cookie_header_has_none() {
        Cookies responseCookies = new Cookies(new Cookie.Builder("name", "fromResponse").setExpiryDate(EXPIRY_DATE).build());

        Map<String, Object> result = validate(detailedCookie().value("value").expiryDate(EXPIRY_DATE), Collections.singletonList("name=value"), responseCookies);

        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    public void keeps_set_cookie_header_cookie_when_response_cookie_has_no_expiry_date() {
        Cookies responseCookies = new Cookies(new Cookie.Builder("name", "fromResponse").build());

        Map<String, Object> result = validate(detailedCookie().value("value").expiryDate(nullValue()), Collections.singletonList("name=value"), responseCookies);

        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    public void matches_null_when_cookie_is_missing_everywhere() {
        Map<String, Object> result = validate(nullValue(), Collections.emptyList(), new Cookies());

        assertThat(result.get("success")).isEqualTo(true);
    }

    @Test
    public void reports_missing_cookie_with_default_detailed_cookie_matcher() {
        Map<String, Object> result = validate(detailedCookie(), Collections.emptyList(), new Cookies());

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(String.valueOf(result.get("errorMessage"))).isEqualTo("Expected cookie \"name\" was not not null, was null.\n");
    }

    @Test
    public void keeps_groovy_accessors() {
        Matcher<Cookie> matcher = detailedCookie();
        DetailedCookieAssertion assertion = new DetailedCookieAssertion();
        assertion.setCookieName("name");
        assertion.setMatcher(matcher);

        assertThat(assertion.getCookieName()).isEqualTo("name");
        assertThat((Object) assertion.getMatcher()).isSameAs(matcher);
    }
}
