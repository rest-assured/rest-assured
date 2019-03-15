/*
 * Copyright 2019 the original author or authors.
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

package io.restassured.itest.java;

import io.restassured.RestAssured;
import org.junit.Ignore;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.expect;
import static io.restassured.http.ContentType.HTML;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class URLEncodingParamITest {

    private static final String HEISE_SEARCH = "http://www.heise.de/suche/?";
    private static final String APACHE_404 = "http://www.apache.org/question-%3F";

    private String searchHeise(String query, String... params) {
        String html = expect().statusCode(200).and().contentType(HTML).when().get(HEISE_SEARCH + query, (Object[]) params).asString();

        // find search term in search input element
        Pattern p = Pattern.compile("<input[^>]+search_q[^>]+value=\"([^\"]*)\"");
        Matcher m = p.matcher(html);
        assertTrue(m.find());

        return m.group(1);
    }

    private String searchApache() {
        String html = RestAssured.given()
                .expect()
                .statusCode(404)
                .contentType(HTML)
                .when()
                .get(APACHE_404)
                .asString();

        // find search term in search input element
        Pattern p = Pattern.compile("/question-(.+?) ");
        Matcher m = p.matcher(html);
        assertTrue(m.find());

        String echo = m.group(1);
        return echo;
    }

    @Test
    @Ignore("Site is down atm")
    public void testUrlEncodingOnHeise() {
        assertEquals("foo", searchHeise("q=foo"));
        assertThat(searchHeise("q=%3F"), equalTo("%3F"));
        assertThat(searchHeise("q={token}", "%3F"), equalTo("%3F"));

        RestAssured.urlEncodingEnabled = false;
        try {
            assertEquals("foo", searchHeise("q=foo"));
            assertEquals("?", searchHeise("q=%3F"));
            assertEquals("%3F", searchHeise("q={token}", "%253F"));
        } finally {
            RestAssured.reset();
        }
    }

    @Ignore("Server is down atm")
    @Test
    public void testUrlEncodingOnApache() {
        assertEquals("%3F", searchApache());

        RestAssured.urlEncodingEnabled = false;

        try {
            assertEquals("?", searchApache());
        } finally {
            RestAssured.reset();
        }
    }
}
