package io.restassured.internal.assertion;

import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.DateUtils;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import java.util.*;

import static io.restassured.http.Cookie.*;

public class CookieMatcher {
    private static final Log log = LogFactory.getLog(CookieMatcher.class);
    private Object cookieName;
    private Matcher<String> matcher;

    public Map<String, Object> validateCookies(List<String> headerWithCookieList, Cookies responseCookies) {
        boolean success = true;
        String errorMessage = "";
        if (headerWithCookieList == null || (headerWithCookieList.isEmpty() && !responseCookies.exist())) {
            success = false;
            errorMessage = "No cookies defined in the response\n";
        } else {
            Cookies cookiesInHeader = getCookies(headerWithCookieList);
            List<Cookie> mergedCookies = new ArrayList<>();
            cookiesInHeader.forEach(mergedCookies::add);
            for (Cookie responseCookie : (responseCookies == null ? new Cookies() : responseCookies)) {
                if (!cookiesInHeader.hasCookieWithName(responseCookie.getName())) {
                    mergedCookies.add(responseCookie);
                }
            }


            Cookies raCookies = new Cookies(mergedCookies);
            Cookie cookie = raCookies.get((String) cookieName);
            if (cookie == null) {
                String cookiesAsString = raCookies.toString();
                success = false;
                errorMessage = "Cookie \"" + getCookieName() + "\" was not defined in the response. Cookies are: \n" + cookiesAsString + "\n";
            } else {
                String value = cookie.getValue();
                if (!matcher.matches(value)) {
                    success = false;
                    String expectedDescription = getExpectedDescription(matcher);
                    String mismatchDescription = getMismatchDescription(matcher, value);
                    errorMessage = "Expected cookie \"" + getCookieName() + "\" was not " + expectedDescription + ", " + mismatchDescription + ".\n";
                }
            }
        }

        Map<String, Object> map = new LinkedHashMap<>(2);
        map.put("success", success);
        map.put("errorMessage", errorMessage);
        return map;
    }

    public static String getExpectedDescription(Matcher<?> matcher) {
        StringDescription expectedDescription = new StringDescription();
        matcher.describeTo(expectedDescription);
        return expectedDescription.toString();
    }

    public static String getMismatchDescription(Matcher<?> matcher, Object value) {
        StringDescription mismatchDescription = new StringDescription();
        matcher.describeMismatch(value, mismatchDescription);
        return mismatchDescription.toString();
    }

    public static Cookies getCookies(List<String> headerWithCookieList) {
        List<Cookie> cookieList = new ArrayList<>();
        headerWithCookieList.forEach(it -> {
            String[] cookieStrings = StringUtils.split(it, ";");
            Cookie.Builder cookieBuilder = null;
            int cookieStringsLength = cookieStrings.length;
            for (int i = 0; i < cookieStringsLength; i++) {
                String part = cookieStrings[i];
                if (i == 0) {
                    if (part.contains("=")) {
                        final Iterator<String> iterator = getKeyAndValueOfCookie(part).iterator();
                        String cookieKey = iterator.hasNext() ? iterator.next() : null;
                        String cookieValue = iterator.hasNext() ? iterator.next() : null;
                        cookieBuilder = new Cookie.Builder(cookieKey, cookieValue);
                    } else {
                        cookieBuilder = new Cookie.Builder(part, null);
                    }
                } else if (part.contains("=")) {
                    final Iterator<String> iterator = getKeyAndValueOfCookie(part).iterator();
                    String cookieKey = iterator.hasNext() ? iterator.next() : null;
                    String cookieValue = iterator.hasNext() ? iterator.next() : null;
                    setCookieProperty(cookieBuilder, cookieKey, cookieValue);
                } else {
                    setCookieProperty(cookieBuilder, part, null);
                }
            }
            cookieList.add(cookieBuilder == null ? null : cookieBuilder.build());
        });
        return new Cookies(cookieList);
    }


    public static List<String> getKeyAndValueOfCookie(String part) {
        int indexOfEqual = StringUtils.indexOf(part, "=");
        String cookieKey;
        String cookieValue;
        if (indexOfEqual > -1) {
            cookieKey = StringUtils.substring(part, 0, indexOfEqual);
            cookieValue = StringUtils.substring(part, indexOfEqual + 1);
        } else {
            cookieKey = part;
            cookieValue = null;
        }

        return new ArrayList<>(Arrays.asList(StringUtils.trim(cookieKey), StringUtils.trim(cookieValue)));
    }

    private static void setCookieProperty(Cookie.Builder builder, String name, String value) {
        name = StringUtils.trim(name);
        if (value != null || StringUtils.equalsIgnoreCase(name, SECURE) || StringUtils.equalsIgnoreCase(name, HTTP_ONLY)) {
            if (StringUtils.equalsIgnoreCase(name, COMMENT)) {
                builder.setComment(value);
            } else if (StringUtils.equalsIgnoreCase(name, VERSION)) {
                // Some servers supply the version in quotes, remove them
                value = StringUtils.trim(StringUtils.remove(value, "\""));
                if (NumberUtils.isDigits(value)) {
                    builder.setVersion(Integer.parseInt(value));
                }
            } else if (StringUtils.equalsIgnoreCase(name, PATH)) {
                builder.setPath(value);
            } else if (StringUtils.equalsIgnoreCase(name, DOMAIN)) {
                builder.setDomain(value);
            } else if (StringUtils.equalsIgnoreCase(name, MAX_AGE)) {
                builder.setMaxAge(Long.parseLong(value));
            } else if (StringUtils.equalsIgnoreCase(name, SECURE)) {
                builder.setSecured(true);
            } else if (StringUtils.equalsIgnoreCase(name, HTTP_ONLY)) {
                builder.setHttpOnly(true);
            } else if (StringUtils.equalsIgnoreCase(name, EXPIRES)) {
                value = StringUtils.trim(StringUtils.remove(value, "\""));
                Date parsedDate = DateUtils.parseDate(value);
                if (parsedDate != null) {
                    builder.setExpiryDate(parsedDate);
                } else {
                    log.warn("Ignoring unparsable 'Expires' attribute value: " + value);
                }

            } else if (StringUtils.equalsIgnoreCase(name, SAME_SITE)) {
                builder.setSameSite(value);
            }

        }
    }

    public Object getCookieName() {
        return cookieName;
    }

    public void setCookieName(Object cookieName) {
        this.cookieName = cookieName;
    }

    public Matcher<String> getMatcher() {
        return matcher;
    }

    public void setMatcher(Matcher<String> matcher) {
        this.matcher = matcher;
    }
}
