
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
package io.restassured.module.mockmvc.internal;

import io.restassured.config.MultiPartConfig;

import java.io.File;
import java.io.InputStream;

import static io.restassured.internal.common.assertion.AssertParameter.notNull;

class MockMvcMultiPart {
    private static final String OCTET_STREAM = "application/octet-stream";
    private static final String TEXT_PLAIN = "text/plain";

    private final String controlName;
    private final String fileName;
    private final Object content;
    private final String mimeType;

    MockMvcMultiPart(MultiPartConfig config, File file) {
        this(config.defaultControlName(), file);
    }

    MockMvcMultiPart(String controlName, File file) {
        this(controlName, file, OCTET_STREAM);
    }

    MockMvcMultiPart(String controlName, File file, String mimeType) {
        notNull(controlName, "Control name");
        notNull(file, File.class);
        notNull(mimeType, "Mime-Type");
        this.controlName = controlName;
        this.fileName = file.getName();
        this.content = file;
        this.mimeType = mimeType;
    }

    MockMvcMultiPart(MultiPartConfig config, String controlName, String content) {
        notNull(controlName, "Control name");
        notNull(content, "Content");
        this.controlName = controlName;
        this.fileName = config.defaultFileName();
        this.content = content;
        this.mimeType = TEXT_PLAIN;
    }

    MockMvcMultiPart(MultiPartConfig config, String controlName, String content, String mimeType) {
        notNull(controlName, "Control name");
        notNull(mimeType, "Mime-Type");
        notNull(content, "Content");
        this.controlName = controlName;
        this.fileName = config.defaultFileName();
        this.content = content;
        this.mimeType = mimeType;
    }

    MockMvcMultiPart(String controlName, String fileName, Object content) {
        notNull(controlName, "Control name");
        notNull(content, "Content");
        this.controlName = controlName;
        this.fileName = fileName;
        this.content = content;
        this.mimeType = OCTET_STREAM;
    }

    MockMvcMultiPart(String controlName, String fileName, Object content, String mimeType) {
        notNull(mimeType, "Mime-Type");
        notNull(content, "Content");
        this.controlName = controlName;
        this.fileName = fileName;
        this.content = content;
        this.mimeType = mimeType;
    }

    public String getControlName() {
        return controlName;
    }

    public String getFileName() {
        return fileName;
    }

    public Object getContent() {
        return content;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isInputStream() {
        return content instanceof InputStream;
    }

    public boolean isByteArray() {
        return content instanceof byte[];
    }

    public boolean isFile() {
        return content instanceof File;
    }

    public boolean isText() {
        return content instanceof String;
    }
}
