package io.restassured.module.spring.commons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Headers;
import io.restassured.module.spring.commons.config.SpecificationConfig;

/**
 * @author Olga Maciaszek-Sharma
 */
public class CookieHelper {

	private CookieHelper() {
	}

	public static Cookies cookies(Cookies requestCookies, Map<String, ?> cookies, Headers requestHeaders,
	                              SpecificationConfig config) {
		List<Cookie> cookieList = new ArrayList<Cookie>();
		if (requestCookies.exist()) {
			for (Cookie requestCookie : requestCookies) {
				cookieList.add(requestCookie);
			}
		}
		for (Map.Entry<String, ?> stringEntry : cookies.entrySet()) {
			cookieList.add(new Cookie.Builder(stringEntry.getKey(), Serializer.serializeIfNeeded(stringEntry.getValue(),
					HeaderHelper.getRequestContentType(requestHeaders), config)).build());
		}
		return new Cookies(cookieList);
	}

	public static Cookies cookies(Cookies requestCookies, Cookies cookies) {
		if (cookies.exist()) {
			List<Cookie> cookieList = new ArrayList<Cookie>();
			if (requestCookies.exist()) {
				for (Cookie cookie : requestCookies) {
					cookieList.add(cookie);
				}
			}
			for (Cookie cookie : cookies) {
				cookieList.add(cookie);
			}
			return new Cookies(cookieList);
		}
		return requestCookies;
	}

	public static Cookies cookie(final String cookieName, final Object cookieValue, Headers requestHeaders,
	                             final SpecificationConfig config, Object... additionalValues) {
		final String contentType = HeaderHelper.getRequestContentType(requestHeaders);
		List<Cookie> cookieList = new ArrayList<Cookie>() {{
			add(new Cookie.Builder(cookieName, Serializer.serializeIfNeeded(cookieValue, contentType, config)).build());
		}};
		if (additionalValues != null) {
			for (Object additionalCookieValue : additionalValues) {
				cookieList.add(new Cookie.Builder(cookieName,
						Serializer.serializeIfNeeded(additionalCookieValue, contentType, config)).build());
			}
		}
		return new Cookies(cookieList);
	}
}
