/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.response;

import org.apache.commons.lang.Validate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static com.jayway.restassured.assertion.AssertParameter.notNull;

/**
 * Cookie class represents a token or short packet of state information
 * (also referred to as "magic-cookie") that the HTTP agent and the target
 * server can exchange to maintain a session. In its simples form an HTTP
 * cookie is merely a name / value pair.
 *
 * To construct a new new Cookie use the Builder like this:
 *
 * <pre>
 *  Cookie cookie = new Cookie.Builder("name", "value").setComment("some comment").setExpiryDate(someDate).build();
 * </pre>
 *
 * Credits: Some of the javadoc in this class is copied from the <code>org.apache.http.cookie.Cookie</code> class in <a href="http://hc.apache.org/">Apache HTTP Client</a>.
 */
public class Cookie {
    public static final String COMMENT = "Comment";
    public static final String PATH = "Path";
    public static final String DOMAIN = "Domain";
    public static final String MAX_AGE = "Max-Age";
    public static final String SECURED = "Secured";
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
    private final int version;
    private final int maxAge;

    private Cookie(String name, String value, String comment, Date expiryDate,
                   String domain, String path, boolean secured, int version,
                   int maxAge) {
        this.name = name;
        this.value = value;
        this.comment = comment;
        this.expiryDate = expiryDate;
        this.domain = domain;
        this.path = path;
        this.secured = secured;
        this.version = version;
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
     * @return String value The current value.
     */
    public String getValue() {
        return value;
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
     * Returns the expiration {@link java.util.Date} of the cookie, or <tt>null</tt>
     * if none exists.
     * @return Expiration {@link java.util.Date}, or <tt>null</tt>.
     */
    public Date getExpiryDate() {
        return expiryDate;
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
     * Returns the maximum age of the cookie, specified in seconds,
     * By default, <code>-1</code> indicating the cookie will persist
     * until browser shutdown.
     *
     *
     * @return	 an integer specifying the maximum age of the
     *				cookie in seconds; if negative, means
     *				the cookie persists until browser shutdown
     */

    public int getMaxAge() {
        return maxAge;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(name).append(EQUALS).append(value);
        if(comment != null) {
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(COMMENT).append(EQUALS).append(comment);
        }
        if(path != null) {
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(PATH).append(EQUALS).append(path);
        }
        if(domain != null) {
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(DOMAIN).append(EQUALS).append(domain);
        }
        if(maxAge != UNDEFINED) {
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(MAX_AGE).append(EQUALS).append(maxAge);
        }
        if(secured) {
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(SECURED);
        }
        if(expiryDate != null) {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(EXPIRES).append(EQUALS).append(simpleDateFormat.format(expiryDate));
        }
        if(version != UNDEFINED) {
            builder.append(COOKIE_ATTRIBUTE_SEPARATOR).append(VERSION).append(EQUALS).append(version);
        }
        return super.toString();
    }

    public static class Builder {
        private final String name;
        private final String value;
        private String comment;
        private Date expiryDate;
        private String domain;
        private String path;
        private boolean secured;
        private int version;
        private int maxAge;

        public Builder(String name) {
            this(name, null);
        }

        public Builder(String name, String value) {
            this.name = name;
            this.value = value;
            notNull(name, "Cookie name");
        }

        public Builder setComment(String comment) {
            notNull(name, "Cookie name");
            this.comment = comment;
            return this;
        }

        public Builder setExpiryDate(Date date) {
            notNull(date, "Cookie expiry date");
            this.expiryDate = date;
            return this;
        }

        public Builder setDomain(String domain) {
            notNull(domain, "Cookie domain");
            this.domain = domain;
            return this;
        }

        public Builder setPath(String path) {
            notNull(domain, "Cookie path");
            this.path = path;
            return this;
        }

        public Builder setSecured(boolean secured) {
            this.secured = secured;
            return this;
        }

        public Builder setVersion(int version) {
            if(version < 0) {
                throw new IllegalArgumentException("Version cannot be less than 0");
            }
            this.version = version;
            return this;
        }

        public Builder setMaxAge(int maxAge) {
            if(maxAge < 0) {
                throw new IllegalArgumentException("Max-Age cannot be less than 0");
            }
            this.maxAge = maxAge;
            return this;
        }

        public Cookie build() {
            return new Cookie(name, value, comment, expiryDate, domain, path, secured, version, maxAge);
        }
    }
}