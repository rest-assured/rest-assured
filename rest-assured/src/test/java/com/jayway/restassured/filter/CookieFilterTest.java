package com.jayway.restassured.filter;

import com.jayway.restassured.config.RestAssuredConfig;
import com.jayway.restassured.filter.cookie.CookieFilter;
import com.jayway.restassured.internal.ResponseSpecificationImpl;
import com.jayway.restassured.internal.RestAssuredResponseImpl;
import com.jayway.restassured.response.Cookie;
import com.jayway.restassured.response.Cookies;
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
