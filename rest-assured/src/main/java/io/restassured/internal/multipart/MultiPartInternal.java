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
package io.restassured.internal.multipart;

import io.restassured.internal.NoParameterValue;
import io.restassured.internal.common.util.GroovyStyleToString;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static io.restassured.internal.util.GroovyStringConversion.castToString;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * A multipart of a request. {@link #equals(Object)}, {@link #hashCode()} and {@link #toString()} work like the ones
 * Groovy's {@code @Canonical} generated when this class was written in Groovy, except that equals compares the
 * properties with {@link Objects#deepEquals(Object, Object)} instead of Groovy's {@code ==}.
 */
public class MultiPartInternal {

    public static final String OCTET_STREAM = "application/octet-stream";
    private static final String TEXT_PLAIN = "text/plain";

    private Object content;
    private String controlName;
    private String fileName;
    private String mimeType;
    private String charset;
    private Map<String, String> headers = new LinkedHashMap<>();

    /**
     * @return The {@link ContentBody} for the content. A {@code byte[]} content is replaced by a stream of it and a
     * {@link NoParameterValue} content by an empty String.
     */
    public Object getContentBody() {
        if (content instanceof NoParameterValue) {
            content = "";
        }

        if (content instanceof File) {
            return new FileBody((File) content, contentTypeWithCharset(defaultIfEmpty(mimeType, OCTET_STREAM)), fileName);
        } else if (content instanceof InputStream) {
            return returnInputStreamBody();
        } else if (content instanceof byte[]) {
            content = new ByteArrayInputStream((byte[]) content);
            return returnInputStreamBody();
        } else if (content instanceof String) {
            return returnStringBody((String) content);
        } else if (content != null) {
            return returnStringBody(GroovyStyleToString.toString(content));
        } else {
            throw new IllegalArgumentException("Illegal content: " + content);
        }
    }

    public String getMimeType() {
        if (content instanceof File || content instanceof InputStream || content instanceof byte[]) {
            return defaultIfEmpty(mimeType, OCTET_STREAM);
        } else if (content != null) {
            return defaultIfEmpty(mimeType, TEXT_PLAIN);
        } else {
            return mimeType;
        }
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getControlName() {
        return controlName;
    }

    public void setControlName(String controlName) {
        this.controlName = controlName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    private StringBody returnStringBody(String content) {
        return new StringBody(content, contentTypeWithCharset(defaultIfEmpty(mimeType, TEXT_PLAIN)));
    }

    private ContentType contentTypeWithCharset(String baseMimeType) {
        ContentType baseContentType = ContentType.parse(baseMimeType);
        // withParameters (unlike withCharset) keeps any other parameters of the mime-type, e.g. "application/xml; version=2"
        return isEmpty(charset) ? baseContentType : baseContentType.withParameters(new BasicNameValuePair("charset", Charset.forName(charset).name()));
    }

    private InputStreamBody returnInputStreamBody() {
        return new InputStreamBody((InputStream) content, ContentType.parse(defaultIfEmpty(mimeType, OCTET_STREAM)), fileName);
    }

    public boolean canEqual(Object other) {
        return other instanceof MultiPartInternal;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MultiPartInternal)) {
            return false;
        }
        MultiPartInternal that = (MultiPartInternal) other;
        return that.canEqual(this)
                && Objects.deepEquals(getContent(), that.getContent())
                && Objects.equals(getControlName(), that.getControlName())
                && Objects.equals(getFileName(), that.getFileName())
                && Objects.equals(getMimeType(), that.getMimeType())
                && Objects.equals(getCharset(), that.getCharset())
                && Objects.equals(getHeaders(), that.getHeaders());
    }

    @Override
    public int hashCode() {
        // Same algorithm as Groovy's HashCodeHelper, which @Canonical used
        int hash = 127;
        for (Object value : new Object[]{getContent(), getControlName(), getFileName(), getMimeType(), getCharset(), getHeaders()}) {
            if (value != this) {
                hash = 59 * hash + hashOf(value);
            }
        }
        return hash;
    }

    private static int hashOf(Object value) {
        if (value == null) {
            return 0;
        } else if (value instanceof Boolean) {
            return (Boolean) value ? 79 : 97;
        } else if (value instanceof Object[]) {
            return Arrays.hashCode((Object[]) value);
        } else if (value.getClass().isArray()) {
            return primitiveArrayHashCode(value);
        }
        return value.hashCode();
    }

    private static int primitiveArrayHashCode(Object array) {
        if (array instanceof byte[]) {
            return Arrays.hashCode((byte[]) array);
        } else if (array instanceof int[]) {
            return Arrays.hashCode((int[]) array);
        } else if (array instanceof char[]) {
            return Arrays.hashCode((char[]) array);
        } else if (array instanceof long[]) {
            return Arrays.hashCode((long[]) array);
        } else if (array instanceof short[]) {
            return Arrays.hashCode((short[]) array);
        } else if (array instanceof boolean[]) {
            return Arrays.hashCode((boolean[]) array);
        } else if (array instanceof float[]) {
            return Arrays.hashCode((float[]) array);
        }
        return Arrays.hashCode((double[]) array);
    }

    /**
     * Like {@code @Canonical}, this includes the content body (see {@link #getContentBody()}, which also replaces the content)
     * and fails without content.
     */
    @Override
    public String toString() {
        return MultiPartInternal.class.getName() + "("
                + castToString(getContent()) + ", "
                + getControlName() + ", "
                + getFileName() + ", "
                + getMimeType() + ", "
                + getCharset() + ", "
                + castToString(getHeaders()) + ", "
                + castToString(getContentBody()) + ")";
    }
}
