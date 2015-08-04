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
package com.jayway.restassured.builder;

import com.jayway.restassured.internal.MultiPartSpecificationImpl;
import com.jayway.restassured.specification.MultiPartSpecification;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;

import static java.lang.String.format;

/**
 * Builder for creating more advanced multi-part requests.
 * <p/>
 * Usage example:
 * <pre>
 * File myFile = ..
 * given().multiPart(new MultiPartSpecBuilder(myFile).with().fileName("some-name.txt").and().with().mimeType("application/vnd.mycompany+text").build()). ..
 * </pre>
 */
public class MultiPartSpecBuilder {

    private Object content;
    private String controlName;
    private String mimeType;
    private String charset;
    private String fileName;

    /**
     * Create a new multi-part specification with control name equal to file.
     *
     * @param content The content to include in the multi-part specification.
     */
    public MultiPartSpecBuilder(Object content) {
        Validate.notNull(content, "Multi-part content cannot be null");
        this.content = content;
        this.controlName = "file";
    }

    /**
     * Create a new multi-part specification with control name equal to file.
     *
     * @param content The content to include in the multi-part specification.
     */
    public MultiPartSpecBuilder(InputStream content) {
        this((Object) content);
    }

    /**
     * Create a new multi-part specification with control name equal to file.
     *
     * @param content The content to include in the multi-part specification.
     */
    public MultiPartSpecBuilder(String content) {
        this((Object) content);
    }

    /**
     * Create a new multi-part specification with control name equal to file.
     *
     * @param content The content to include in the multi-part specification.
     */
    public MultiPartSpecBuilder(byte[] content) {
        this((Object) content);
    }

    /**
     * Create a new multi-part specification with control name equal to file.
     *
     * @param content The content to include in the multi-part specification.
     */
    public MultiPartSpecBuilder(File content) {
        this((Object) content);
    }


    /**
     * Specify the control name of this multi-part.
     *
     * @param controlName The control name to use. Default is <code>file</code>.
     * @return An instance of MultiPartSpecBuilder
     */
    public MultiPartSpecBuilder controlName(String controlName) {
        Validate.notEmpty(controlName, "Control name cannot be empty");
        this.controlName = controlName;
        return this;
    }

    /**
     * Specify the file name of this multi-part. Note that this is only applicable for input streams, byte arrays and files
     * and <i>not</i> string content.
     *
     * @param fileName The file name to use.
     * @return An instance of MultiPartSpecBuilder
     */
    public MultiPartSpecBuilder fileName(String fileName) {
        Validate.notEmpty(fileName, "File name cannot be empty");
        if (!(content instanceof File || content instanceof byte[] || content instanceof InputStream)) {
            throw new IllegalArgumentException(format("Cannot specify file name for non file content (%s).", content.getClass().getName()));
        }
        this.fileName = fileName;
        return this;
    }

    /**
     * Specify the mime-type for this multi-part.
     *
     * @param mimeType The mime-type
     * @return An instance of MultiPartSpecBuilder
     */
    public MultiPartSpecBuilder mimeType(String mimeType) {
        Validate.notEmpty(mimeType, "Mime-type cannot be empty");
        this.mimeType = mimeType;
        return this;
    }

    /**
     * Specify the charset for this charset.
     *
     * @param charset The charset to use
     * @return An instance of MultiPartSpecBuilder
     */
    public MultiPartSpecBuilder charset(String charset) {
        Validate.notEmpty(charset, "Charset cannot be empty");
        if (content instanceof byte[] || content instanceof InputStream) {
            throw new IllegalArgumentException(format("Cannot specify charset input streams or byte arrays."));
        }
        this.charset = charset;
        return this;
    }

    /**
     * Just a method that can be used as syntactic sugar.
     *
     * @return The same instance of the MultiPartSpecBuilder
     */
    public MultiPartSpecBuilder with() {
        return this;
    }

    /**
     * Just a method that can be used as syntactic sugar.
     *
     * @return The same instance of the MultiPartSpecBuilder
     */
    public MultiPartSpecBuilder and() {
        return this;
    }

    /**
     * Specify the charset for this charset.
     *
     * @param charset The charset to use
     * @return An instance of MultiPartSpecBuilder
     */
    public MultiPartSpecBuilder charset(Charset charset) {
        Validate.notNull(charset, "Charset cannot be null");
        this.charset = charset.toString();
        return this;
    }

    public MultiPartSpecification build() {
        MultiPartSpecificationImpl spec = new MultiPartSpecificationImpl();
        spec.setCharset(charset);
        spec.setContent(content);
        spec.setControlName(controlName);
        spec.setFileName(fileName);
        spec.setMimeType(mimeType);
        return spec;
    }
}
