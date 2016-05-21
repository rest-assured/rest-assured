/*
 * Copyright 2016 the original author or authors.
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

package io.restassured.filter;

import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.internal.ResponseSpecificationImpl;
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.response.Cookie;
import io.restassured.response.Cookies;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSender;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertThat;


public class CookieFilterTest {

    private FilterableResponseSpecification response;
    private FilterableRequestSpecification req;

    @Before
    public void setUp() {
        req = (FilterableRequestSpecification) given();
        response = new ResponseSpecificationImpl("test", null, null, RestAssuredConfig.newConfig(), null);
    }

    @Test
    public void storeCookies() {
        CookieFilter cookieFilter = new CookieFilter();

        cookieFilter.filter((FilterableRequestSpecification) given(), response, new TestFilterContext());
        cookieFilter.filter(req, response, new TestFilterContext());

        assertThat(req.getCookies().size(), Matchers.is(1));

        for (Cookie cookie : req.getCookies()) {
            assertThat(cookie.getName(), Matchers.is("foo"));
            assertThat(cookie.getValue(), Matchers.is("bar"));
        }
    }

    @Test
    public void preserveCookies() {
        CookieFilter cookieFilter = new CookieFilter();

        req.cookie("foo", "barbar");
        cookieFilter.filter((FilterableRequestSpecification) given(), response, new TestFilterContext());
        cookieFilter.filter(req, response, new TestFilterContext());

        assertThat(req.getCookies().size(), Matchers.is(1));

        for (Cookie cookie : req.getCookies()) {
            assertThat(cookie.getName(), Matchers.is("foo"));
            assertThat(cookie.getValue(), Matchers.is("barbar"));
        }
    }

    private static class TestFilterContext implements FilterContext {
        public void setValue(String name, Object value) {

        }

        public <T> T getValue(String name) {
            return null;
        }

        public boolean hasValue(String name) {
            return false;
        }

        public Response send(RequestSender requestSender) {
            return null;
        }

        public Response next(FilterableRequestSpecification request, FilterableResponseSpecification response) {
            RestAssuredResponseImpl restAssuredResponse = new RestAssuredResponseImpl();
            Cookie cookie = new Cookie.Builder("foo", "bar").build();
            restAssuredResponse.setCookies(new Cookies(cookie));
            return restAssuredResponse;
        }
    }
}
