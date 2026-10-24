/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.internal.http.cookie;

import java.util.Date;
import java.util.Locale;

/**
 * A cookie in a {@link CookieStore}: what a {@code Set-Cookie} header said, with the defaults of the request it came
 * from filled in.
 */
public final class StoredCookie {
    private String name;
    private String value;
    private String domain;
    private String path;
    private String comment;
    private boolean secure;
    private boolean httpOnly;
    private boolean domainAttributeSet;
    private Date expiryDate;
    private int version;

    /**
     * @param name  the name
     * @param value the value, or {@code null} if the cookie has none
     */
    public StoredCookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDomain() {
        return domain;
    }

    /**
     * Sets the domain the cookie came from, which the request it came from decides.
     */
    public void setDomain(String domain) {
        this.domain = domain == null ? null : domain.toLowerCase(Locale.ROOT);
    }

    /**
     * Sets the domain of the cookie's {@code Domain} attribute, which widens the hosts it's sent to.
     */
    public void setDomainAttribute(String domain) {
        setDomain(domain);
        this.domainAttributeSet = true;
    }

    /**
     * @return whether the cookie has a {@code Domain} attribute, so that it's sent to the subdomains of its domain too
     */
    public boolean hasDomainAttribute() {
        return domainAttributeSet;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    /**
     * @return when the cookie expires, or {@code null} if it expires with the session
     */
    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * @return whether the cookie has expired at the given time
     */
    public boolean isExpired(Date now) {
        return expiryDate != null && !expiryDate.after(now);
    }

    @Override
    public String toString() {
        return "[" + version + "]" + name + "=" + value + "; domain=" + domain + "; path=" + path
                + (expiryDate == null ? "" : "; expires=" + expiryDate) + (secure ? "; secure" : "");
    }
}
