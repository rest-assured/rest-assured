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

package io.restassured.filter.cookie;

import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.FilterContext;
import io.restassured.http.Cookie;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.internal.ResponseSpecificationImpl;
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSender;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertThat;

public class CookieFilterTest {

    private FilterableResponseSpecification response;
    private FilterableRequestSpecification reqOriginDomain;
    private TestFilterContext testFilterContext;
    private CookieFilter cookieFilter;

    @Before
    public void setUp() {
        reqOriginDomain = (FilterableRequestSpecification) given().with().baseUri("https://somedomain.com/somepath");
        response = new ResponseSpecificationImpl("test", null, null, RestAssuredConfig.newConfig(), null);
        testFilterContext = new TestFilterContext();
        cookieFilter = new CookieFilter();
    }

    @Test
    public void addCookiesToMatchingUrlRequest() {

        cookieFilter.filter(reqOriginDomain, response, testFilterContext);
        cookieFilter.filter(reqOriginDomain, response, testFilterContext);

        assertThat(reqOriginDomain.getCookies().size(), Matchers.is(2));

        assertThat(reqOriginDomain.getCookies().hasCookieWithName("cookieName1"), Matchers.is(true));
        assertThat(reqOriginDomain.getCookies().getValue("cookieName1"), Matchers.is("cookieValue1"));

        assertThat(reqOriginDomain.getCookies().hasCookieWithName("cookieName2"), Matchers.is(true));
        assertThat(reqOriginDomain.getCookies().getValue("cookieName2"), Matchers.is("cookieValue2"));
    }

    @Test
    public void addDuplicateNameCookiesLikeInBrowser() {
        FilterableRequestSpecification reqOriginDomainDuplicate = (FilterableRequestSpecification)
                given().with().baseUri("https://foo.com/bar");
        DuplicateTestFilterContext duplicateTestFilterContext = new DuplicateTestFilterContext();
        final CookieFilter cookieFilterDuplicate = new CookieFilter(true);

        cookieFilterDuplicate.filter(reqOriginDomainDuplicate, response, duplicateTestFilterContext);
        cookieFilterDuplicate.filter(reqOriginDomainDuplicate, response, duplicateTestFilterContext);

        assertThat(reqOriginDomainDuplicate.getCookies().size(), Matchers.is(2));
        final List<Cookie> list = reqOriginDomainDuplicate.getCookies().getList("cookieName");
        assertThat(list.get(0).getName(), Matchers.is("cookieName"));
        assertThat(list.get(0).getValue(), Matchers.anyOf(Matchers.is("xxx"), Matchers.is("yyy")));
        assertThat(list.get(1).getName(), Matchers.is("cookieName"));
        assertThat(list.get(1).getValue(), Matchers.anyOf(Matchers.is("xxx"), Matchers.is("yyy")));
    }

    @Test
    public void doesntAddCookiesToNonMatchingUrlRequest() {

        FilterableRequestSpecification reqNonMatchingDomain =
                (FilterableRequestSpecification) given().with().baseUri("https://someother.com/somepath");

        cookieFilter.filter(reqOriginDomain, response, testFilterContext);
        cookieFilter.filter(reqNonMatchingDomain, response, testFilterContext);

        assertThat(reqNonMatchingDomain.getCookies().size(), Matchers.is(0));
    }

    @Test
    public void preserveCookies() {

        reqOriginDomain.cookie("cookieName1", "cookieInitialValue");
        cookieFilter.filter((FilterableRequestSpecification) given(), response, testFilterContext);
        cookieFilter.filter(reqOriginDomain, response, testFilterContext);

        assertThat(reqOriginDomain.getCookies().size(), Matchers.is(2));

        assertThat(reqOriginDomain.getCookies().hasCookieWithName("cookieName1"), Matchers.is(true));
        assertThat(reqOriginDomain.getCookies().getValue("cookieName1"), Matchers.is("cookieInitialValue"));

        assertThat(reqOriginDomain.getCookies().hasCookieWithName("cookieName2"), Matchers.is(true));
        assertThat(reqOriginDomain.getCookies().getValue("cookieName2"), Matchers.is("cookieValue2"));
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
            Header setCookieHeader1 =
                    new Header("Set-Cookie", "cookieName1=cookieValue1; Domain=somedomain.com; Path=/somepath; Secure; HttpOnly");
            Header setCookieHeader2 =
                    new Header("Set-Cookie", "cookieName2=cookieValue2; Domain=somedomain.com; Path=/somepath; Secure; HttpOnly");
            restAssuredResponse.setResponseHeaders(Headers.headers(setCookieHeader1, setCookieHeader2));
            return restAssuredResponse;
        }
    }

    private static class DuplicateTestFilterContext implements FilterContext {

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
            Header setCookieHeader1 =
                    new Header("Set-Cookie", "cookieName=xxx; Domain=foo.com; Path=/bar; Secure; HttpOnly");
            Header setCookieHeader2 =
                    new Header("Set-Cookie", "cookieName=yyy; Domain=foo.com; Path=/; Secure; HttpOnly");
            restAssuredResponse.setResponseHeaders(Headers.headers(setCookieHeader1, setCookieHeader2));
            return restAssuredResponse;
        }
    }
}
