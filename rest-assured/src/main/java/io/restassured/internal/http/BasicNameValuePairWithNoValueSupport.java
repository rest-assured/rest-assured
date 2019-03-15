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

package io.restassured.internal.http;

import io.restassured.internal.NoParameterValue;
import org.apache.http.NameValuePair;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.LangUtils;

import java.io.Serializable;

/**
 * Basically a copy of the BasicNameValuePair that allows for no-value parameters.
 */
public class BasicNameValuePairWithNoValueSupport implements NameValuePair, Cloneable, Serializable {

    private final String name;
    private final String value;
    private final boolean noValueParam;

    /**
     * Default Constructor taking a name and a value. The value may be null.
     *
     * @param name The name.
     * @param value The value.
     */
    public BasicNameValuePairWithNoValueSupport(final String name, final Object value) {
        super();
        if (name == null) {
            throw new IllegalArgumentException("Name may not be null");
        }
        this.name = name;
        this.value = value == null ? "" : value.toString();
        this.noValueParam = value instanceof NoParameterValue;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public boolean hasValue() {
        return !noValueParam;
    }

    public String toString() {
        // don't call complex default formatting for a simple toString

        if (this.value == null) {
            return name;
        } else {
            int len = this.name.length() + 1 + this.value.length();
            CharArrayBuffer buffer = new CharArrayBuffer(len);
            buffer.append(this.name);
            if(!noValueParam) {
                buffer.append("=");
                buffer.append(this.value);
            }
            return buffer.toString();
        }
    }

    public boolean equals(final Object object) {
        if (this == object) return true;
        if (object instanceof NameValuePair) {
            BasicNameValuePairWithNoValueSupport that = (BasicNameValuePairWithNoValueSupport) object;
            return this.name.equals(that.name)
                    && LangUtils.equals(this.value, that.value);
        } else {
            return false;
        }
    }

    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, this.name);
        hash = LangUtils.hashCode(hash, this.value);
        return hash;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
