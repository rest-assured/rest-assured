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

import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.specification.MultiPartSpecification;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import static io.restassured.internal.util.GroovyStringConversion.castToString;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

public class MultiPartSpecificationImpl implements MultiPartSpecification {
    private static final String NONE = "<none>";
    private static final String INPUT_STREAM = "<inputstream>";

    private Object content;
    private String controlName;
    private String mimeType;
    private String charset;
    private String fileName;
    private boolean controlNameSpecifiedExplicitly;
    private boolean fileNameSpecifiedExplicitly;
    private Map<String, String> headers;

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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = StringUtils.trimToNull(fileName);
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public boolean hasFileName() {
        return fileName != null;
    }

    public boolean isControlNameSpecifiedExplicitly() {
        return controlNameSpecifiedExplicitly;
    }

    public boolean getControlNameSpecifiedExplicitly() {
        return controlNameSpecifiedExplicitly;
    }

    public void setControlNameSpecifiedExplicitly(boolean controlNameSpecifiedExplicitly) {
        this.controlNameSpecifiedExplicitly = controlNameSpecifiedExplicitly;
    }

    public boolean isFileNameSpecifiedExplicitly() {
        return fileNameSpecifiedExplicitly;
    }

    public boolean getFileNameSpecifiedExplicitly() {
        return fileNameSpecifiedExplicitly;
    }

    public void setFileNameSpecifiedExplicitly(boolean fileNameSpecifiedExplicitly) {
        this.fileNameSpecifiedExplicitly = fileNameSpecifiedExplicitly;
    }

    @Override
    public String toString() {
        return "controlName=" + defaultIfEmpty(controlName, NONE) + ", mimeType=" + defaultIfEmpty(mimeType, NONE)
                + ", charset=" + defaultIfEmpty(charset, NONE) + ", fileName=" + defaultIfEmpty(fileName, NONE)
                + ", content=" + (content instanceof InputStream ? INPUT_STREAM : interpolate(content))
                + ", headers=" + interpolate(headers);
    }

    /**
     * Renders a value like Groovy's GString interpolation did, which reads (and closes) a Reader.
     */
    private static String interpolate(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof Reader) {
            try (Reader reader = (Reader) value) {
                StringWriter writer = new StringWriter();
                reader.transferTo(writer);
                return writer.toString();
            } catch (IOException e) {
                return SafeExceptionRethrower.safeRethrow(e);
            }
        }
        return castToString(value);
    }
}
