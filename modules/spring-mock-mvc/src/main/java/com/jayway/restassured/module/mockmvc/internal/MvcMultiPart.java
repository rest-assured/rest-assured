
/*
 * Copyright 2013 the original author or authors.
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
package com.jayway.restassured.module.mockmvc.internal;

import java.io.File;
import java.io.InputStream;

import static com.jayway.restassured.internal.assertion.AssertParameter.notNull;

class MvcMultiPart {
    private static final String OCTET_STREAM = "application/octet-stream";
    private static final String TEXT_PLAIN = "text/plain";

    private static final String DEFAULT_CONTROL_NAME = "file";
    private static final String DEFAULT_FILE_NAME = "file";
    private final String controlName;
    private final String fileName;
    private final Object content;
    private final String mimeType;

    MvcMultiPart(File file) {
        this(DEFAULT_CONTROL_NAME, file);
    }

    MvcMultiPart(String controlName, File file) {
        this(controlName, file, OCTET_STREAM);
    }

    MvcMultiPart(String controlName, File file, String mimeType) {
        notNull(controlName, "Control name");
        notNull(file, File.class);
        notNull(mimeType, "Mime-Type");
        this.controlName = controlName;
        this.fileName = file.getName();
        this.content = file;
        this.mimeType = mimeType;
    }

    MvcMultiPart(String controlName, String content) {
        notNull(controlName, "Control name");
        notNull(content, "Content");
        this.controlName = controlName;
        this.fileName = DEFAULT_FILE_NAME;
        this.content = content;
        this.mimeType = TEXT_PLAIN;
    }

    MvcMultiPart(String controlName, String content, String mimeType) {
        notNull(controlName, "Control name");
        notNull(mimeType, "Mime-Type");
        notNull(content, "Content");
        this.controlName = controlName;
        this.fileName = DEFAULT_FILE_NAME;
        this.content = content;
        this.mimeType = mimeType;
    }

    MvcMultiPart(String controlName, String fileName, Object content) {
        notNull(controlName, "Control name");
        notNull(content, "Content");
        this.controlName = controlName;
        this.fileName = fileName;
        this.content = content;
        this.mimeType = OCTET_STREAM;
    }

    MvcMultiPart(String controlName, String fileName, Object content, String mimeType) {
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
