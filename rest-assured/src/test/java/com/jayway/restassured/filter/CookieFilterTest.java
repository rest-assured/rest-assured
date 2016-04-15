package com.jayway.restassured.filter;

import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.filter.cookie.CookieFilter;
import com.jayway.restassured.internal.ResponseSpecificationImpl;
import com.jayway.restassured.internal.RestAssuredResponseImpl;
import com.jayway.restassured.internal.http.Method;
import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;
import com.jayway.restassured.specification.RequestSender;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
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

        response.cookie("foo", "bar");
        cookieFilter.filter(req, response, new TestFilterContext());

        for (Cookie cookie : req.getCookies()) {
            assertThat(cookie.getName(), Matchers.is("foo"));
            assertThat(cookie.getValue(), Matchers.is("bar"));
        }
    }

    @Test
    public void preserveCookies() {
        CookieFilter cookieFilter = new CookieFilter();

        response.cookie("foo", "bar");
        req.cookie("foo", "barbar");
        cookieFilter.filter(req, response, new TestFilterContext());

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

        public Method getRequestMethod() {
            return null;
        }

        public String getRequestPath() {
            return null;
        }

        public String getOriginalRequestPath() {
            return null;
        }

        public String getRequestURI() {
            return null;
        }

        public String getCompleteRequestPath() {
            return null;
        }

        public Response next(FilterableRequestSpecification request, FilterableResponseSpecification response) {
            return new RestAssuredResponseImpl();
        }
    }
}
