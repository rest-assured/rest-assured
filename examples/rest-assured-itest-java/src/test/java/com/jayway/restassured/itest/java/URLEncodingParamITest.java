/*
 * Copyright 2012 the original author or authors.
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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import org.junit.Ignore;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class URLEncodingParamITest {

    private static final String HEISE_SEARCH = "http://www.heise.de/suche/?";

    private String search(String query, String... params) {
        String html = RestAssured.given()
                .expect()
                .statusCode(200)
                .contentType(ContentType.HTML)
                .get(HEISE_SEARCH + query, (Object[]) params)
                .asString();

        // find search term in search input element
        Pattern p = Pattern.compile("<input[^>]+search_q[^>]+value=\"([^\"]*)\"");
        Matcher m = p.matcher(html);
        assertTrue(m.find());

        String echo = m.group(1);
        return echo;
    }

    @Test
    @Ignore("Truncated chunk exception, probably because of the server?")
    public void testUrlEncoding() {
        assertEquals("foo", search("q=foo"));
        assertEquals("%3F", search("q=%3F"));
        assertEquals("%3F", search("q={token}", "%3F"));

        RestAssured.urlEncodingEnabled = false;
        try {
            assertEquals("foo", search("q=foo"));
            assertEquals("?", search("q=%3F"));
            assertEquals("?", search("q={token}", "%3F"));
        } finally {
            RestAssured.reset();
        }
    }
}
