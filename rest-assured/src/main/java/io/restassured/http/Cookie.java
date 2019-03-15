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

package io.restassured.http;

import io.restassured.internal.NameAndValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Cookie class represents a token or short packet of state information
 * (also referred to as "magic-cookie") that the HTTP agent and the target
 * server can exchange to maintain a session. In its simples form an HTTP
 * cookie is merely a name / value pair.
 *
 * To construct a new new Cookie use the Builder like this:
 *
 * <pre>
 * Cookie cookie = new Cookie.Builder("name", "value").setComment("some comment").setExpiryDate(someDate).build();
 * </pre>
 *
 * Credits: Some of the javadoc in this class is copied from the <code>org.apache.http.cookie.Cookie</code> class in <a href="http://hc.apache.org/">Apache HTTP Client</a>.
 * and some (version and secured) from <a href="https://github.com/scalatra/scalatra">Scalatra</a>.
 */
public class Cookie implements NameAndValue {
    public static final String COMMENT = "Comment";
    public static final String PATH = "Path";
    public static final String DOMAIN = "Domain";
    public static final String MAX_AGE = "Max-Age";
    public static final String SECURE = "Secure";
    public static final String HTTP_ONLY = "HttpOnly";
    public static final String EXPIRES = "Expires";
    public static final String VERSION = "Version";

    private static final String COOKIE_ATTRIBUTE_SEPARATOR = ";";
    private static final String EQUALS = "=";
    private static final int UNDEFINED = -1;

    private final String name;
    private final String value;
    private final String comment;
    private final Date expiryDate;
    private final String domain;
    private final String path;
    private final boolean secured;
    private final boolean httpOnly;
    private final int version;
    private final int maxAge;

    private Cookie(String name, String value, String comment, Date expiryDate,
                   String domain, String path, boolean secured, boolean httpOnly, int version,
                   int maxAge) {
        this.name = name;
        this.value = value;
        this.comment = comment;
        this.expiryDate = expiryDate;
        this.domain = domain;
        this.path = path;
        this.secured = secured;
        this.httpOnly = httpOnly;
        this.version = version < 0 ? UNDEFINED : version;
        this.maxAge = maxAge;
    }

    /**
     * Returns the name.
     *
     * @return String name The name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value.
     *
     * @return The current value.
     */
    public String getValue() {
        return value;
    }

    /**
     * @return <code>true</code> if this cookie has a value defined, <code>false</code> otherwise.
     */
    public boolean hasValue() {
        return value != null;
    }

    /**
     * Returns the comment describing the purpose of this cookie, or
     * <tt>null</tt> if no such comment has been defined.
     *
     * @return comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return <code>true</code> if this cookie has a comment defined, <code>false</code> otherwise.
     */
    public boolean hasComment() {
        return comment != null;
    }

    /**
     * Returns the expiration {@link java.util.Date} of the cookie, or <tt>null</tt>
     * if none exists.
     * @return Expiration {@link java.util.Date}, or <tt>null</tt>.
     */
    public Date getExpiryDate() {
        return expiryDate;
    }

    /**
     * @return <code>true</code> if this cookie has an expiry defined, <code>false</code> otherwise.
     */
    public boolean hasExpiryDate() {
        return expiryDate != null;
    }

    /**
     * Returns domain attribute of the cookie. The value of the Domain
     * attribute specifies the domain for which the cookie is valid.
     *
     * @return the value of the domain attribute.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return <code>true</code> if this cookie has a domain defined, <code>false</code> otherwise.
     */
    public boolean hasDomain() {
        return domain != null;
    }

    /**
     * Returns the path attribute of the cookie. The value of the Path
     * attribute specifies the subset of URLs on the origin server to which
     * this cookie applies.
     *
     * @return The value of the path attribute.
     */
    public String getPath() {
        return path;
    }

    /**
     * @return <code>true</code> if this cookie has a path defined, <code>false</code> otherwise.
     */
    public boolean hasPath() {
        return path != null;
    }

    /**
     * Indicates to the browser whether the cookie should only be sent using a secure protocol, such as HTTPS or SSL.
     * @return <code>true</code> if this cookies is secured.
     */
    public boolean isSecured() {
        return secured;
    }

    /**
     * Indicates that the cookie is only readable by the HTTP server and not other API's such as JavaScript.
     * The default value is false.
     * @return <code>true</code> if httpOnly
     */
    public boolean isHttpOnly() {
        return httpOnly;
    }

    /**
     * Gets the version of the cookie protocol this cookie complies with. Version 0 complies with the original Netscape cookie specification. Version 1 complies with RFC 2109.
     * @return The version of this cookie or -1 if version is undefined.
     */
    public int getVersion() {
        return version;
    }

    /**
     * @return <code>true</code> if this cookie has a version defined, <code>false</code> otherwise.
     */
    public boolean hasVersion() {
        return version != UNDEFINED;
    }

    /**
     * Returns the maximum age of the cookie, specified in seconds,
     * By default, <code>-1</code> indicating the cookie will persist
     * until browser shutdown.
     *
     *
     * @return an integer specifying the maximum age of the
     *				cookie in seconds; if negative, means
     *				the cookie persists until browser shutdown
     */
    public int getMaxAge() {
        return maxAge;
    }

    /**
     * @return <code>true</code> if this cookie has a Max-Age defined, <code>false</code> otherwise.
     */
    public boolean hasMaxAge() {
        return maxAge != UNDEFINED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cookie cookie = (Cookie) o;

        if (maxAge != cookie.maxAge) return false;
        if (secured != cookie.secured) return false;
        if (httpOnly != cookie.httpOnly) return false;
        if (version != cookie.version) return false;
        if (comment != null ? !comment.equals(cookie.comment) : cookie.comment != null) return false;
        if (domain != null ? !domain.equals(cookie.domain) : cookie.domain != null) return false;
        if (expiryDate != null ? !expiryDate.equals(cookie.expiryDate) : cookie.expiryDate != null) return false;
        if (name != null ? !name.equals(cookie.name) : cookie.name != null) return false;
        if (path != null ? !path.equals(cookie.path) : cookie.path != null) return false;
        if (value != null ? !value.equals(cookie.value) : cookie.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        result = 31 * result + (expiryDate != null ? expiryDate.hashCode() : 0);
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (secured ? 1 : 0);
        result = 31 * result + (httpOnly ? 1 : 0);
        result = 31 * result + version;
        result = 31 * result + maxAge;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(name);
        if (hasValue()) {
            builder.append(EQUALS).append(value);
        }
        if (hasComment()) {
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(COMMENT).append(EQUALS).append(comment);
        }
        if (hasPath()) {
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(PATH).append(EQUALS).append(path);
        }
        if (hasDomain()) {
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(DOMAIN).append(EQUALS).append(domain);
        }
        if (hasMaxAge()) {
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(MAX_AGE).append(EQUALS).append(maxAge);
        }
        if (isSecured()) {
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(SECURE);
        }
        if (isHttpOnly()) {
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(HTTP_ONLY);
        }
        if (hasExpiryDate()) {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(EXPIRES).append(EQUALS).append(simpleDateFormat.format(expiryDate));
        }
        if (hasVersion()) {
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(VERSION).append(EQUALS).append(version);
        }
        return builder.toString();
    }

    public static class Builder {
        private final String name;
        private final String value;
        private String comment;
        private Date expiryDate;
        private String domain;
        private String path;
        private boolean secured = false;
        private boolean httpOnly = false;
        private int version = UNDEFINED;
        private int maxAge = UNDEFINED;

        /**
         * Create a cookie with no value
         *
         * @param name The name of the cookie
         */
        public Builder(String name) {
            this(name, null);
        }

        /**
         * Create a cookie with name and value
         *
         * @param name The name of the cookie
         * @param value the cookie value
         */
        public Builder(String name, String value) {
            notNull(name, "Cookie name");
            this.name = name;
            this.value = value;
        }

        /**
         * Set the comment describing the purpose of this cookie.
         *
         * @param comment The comment
         * @return The builder
         */
        public Builder setComment(String comment) {
            notNull(name, "Cookie name");
            this.comment = comment;
            return this;
        }

        /**
         * Set the expiration {@link java.util.Date} of the cookie.
         * @param date The date to set
         * @return The builder
         */
        public Builder setExpiryDate(Date date) {
            notNull(date, "Cookie expiry date");
            this.expiryDate = date;
            return this;
        }

        /**
         *  Set domain attribute of the cookie. The value of the Domain
         * attribute specifies the domain for which the cookie is valid.

         * @param domain The domain
         * @return The builder
         */
        public Builder setDomain(String domain) {
            notNull(domain, "Cookie domain");
            this.domain = domain;
            return this;
        }

        /**
         * Set the path attribute of the cookie. The value of the Path
         * attribute specifies the subset of URLs on the origin server to which
         * this cookie applies.
         * @param path The path
         * @return The builder
         */
        public Builder setPath(String path) {
            notNull(path, "Cookie path");
            this.path = path;
            return this;
        }

        /**
         * Set the maximum age of the cookie, specified in seconds,
         * By default, <code>-1</code> indicating the cookie will persist
         * until browser shutdown.
         *
         * @return an integer specifying the maximum age of the cookie in seconds; if negative, means the cookie persists until browser shutdown
         */
        public Builder setMaxAge(int maxAge) {
            this.maxAge = maxAge;
            return this;
        }


        /**
         * Indicates to the browser whether the cookie should only be sent using a secure protocol, such as HTTPS or SSL.
         * The default value is false.
         * @param secured <code>true</code> if secured
         * @return The builder
         */
        public Builder setSecured(boolean secured) {
            this.secured = secured;
            return this;
        }

        /**
         * Indicates that the cookie is only readable by the HTTP server and not other API's such as JavaScript.
         * The default value is false.
         * @param httpOnly <code>true</code> if httpOnly
         * @return The builder
         */
        public Builder setHttpOnly(boolean httpOnly) {
            this.httpOnly = httpOnly;
            return this;
        }

        /**
         Sets the version of the cookie protocol this cookie complies with. Version 0 complies with the original Netscape cookie specification. Version 1 complies with RFC 2109.
         Since RFC 2109 is still somewhat new, consider version 1 as experimental; do not use it yet on production sites.
         Parameters:
         * @param version 0 if the cookie should comply with the original Netscape specification; 1 if the cookie should comply with RFC 2109
         * @return The builder
         */
        public Builder setVersion(int version) {
            if (version < 0) {
                throw new IllegalArgumentException("Version cannot be less than 0");
            } else if (version > 1) {
                throw new IllegalArgumentException("Version cannot be greater than 1");
            }
            this.version = version;
            return this;
        }

        /**
         * Build a Cookie from the specified parameters.
         * @return The Cookie
         */
        public Cookie build() {
            return new Cookie(name, value, comment, expiryDate, domain, path, secured, httpOnly, version, maxAge);
        }
    }
}
