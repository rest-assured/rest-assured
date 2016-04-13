package com.jayway.restassured.filter.cookie;

import com.jayway.restassured.filter.Filter;
import com.jayway.restassured.filter.FilterContext;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.FilterableRequestSpecification;
import com.jayway.restassured.specification.FilterableResponseSpecification;

import java.util.HashMap;
import java.util.Map;

/**
 * The cookie filter can be used to keep track of all the cookies sent by the server and use them in subsequent requests.
 * It might come in handy when more than just the {@link com.jayway.restassured.filter.session.SessionFilter} is needed.
 * For example:
 * <pre>
 * CookieFilter cookieFilter = new CookieFilter();
 *
 * given().
 *         filter(cookieFilter).
 * expect().
 *         statusCode(200).
 * when().
 *         get("/x");
 *
 * given().
 *         cookie("foo", "bar").
 *         filter(cookieFilter). // Reuse the same cookie filter
 *                               // if "foo" is stored in cookieFilter it won't be applied because it's already applied explicitly
 * expect().
 *         statusCode(200).
 * when().
 *         get("/y");
 * </pre>
 */
public class CookieFilter implements Filter {

    private Map<String, String> cookies = new HashMap<String, String>();

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {

        // add all previously stored cookies to subsequent requests
        // but only if they're not already in the request spec
        for (Map.Entry<String, String> cookie : cookies.entrySet()) {
            if (!requestSpec.getCookies().hasCookieWithName(cookie.getKey())) {
                requestSpec.cookie(cookie.getKey(), cookie.getValue());
            }
        }

        final Response response = ctx.next(requestSpec, responseSpec);

        cookies.putAll(response.getCookies());

        return response;
    }
}
