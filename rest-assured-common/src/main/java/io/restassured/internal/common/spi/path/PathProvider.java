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

package io.restassured.internal.common.spi.path;

import java.util.Set;

/**
 * A path engine module, such as GPath in json-path-gpath and xml-path-gpath. REST Assured finds the providers of its own modules by
 * class name. A provider is stateless and thread-safe.
 * <p>
 * This SPI is internal to REST Assured and may change in any release.
 */
public interface PathProvider {

    /**
     * @return The artifact id of the module, such as {@code json-path-gpath}. It names the module's version file and is used in messages.
     */
    String artifactId();

    /**
     * @return The name of the engine, such as {@code GPATH}, which matches the constant of {@code JsonPathEngine}.
     */
    String engineName();

    /**
     * @return The document types this provider parses and evaluates paths on.
     */
    Set<DocumentType> documentTypes();

    /**
     * @return How a failure message names a path of this type, such as {@code JSON path}.
     */
    String description(DocumentType type);

    /**
     * Parse a body into a document that only this provider looks into, so that one parse serves every path evaluated
     * on the body.
     *
     * @throws Exception Any exception of the parser, such as a {@code SAXParseException}, which REST Assured rethrows as is.
     */
    Object parse(PathInput input, DocumentType type, PathSettings settings) throws Exception;

    /**
     * @return The exception that {@code path(..)} on a response throws when {@link #parse(PathInput, DocumentType, PathSettings)}
     * fails with the given exception.
     */
    RuntimeException parseFailure(DocumentType type, Exception cause);

    /**
     * Evaluate a path on a document that {@link #parse(PathInput, DocumentType, PathSettings)} returned.
     */
    PathResult evaluate(Object document, DocumentType type, String path);

    /**
     * Join the root path of a response specification and the key of a body expectation into the path to evaluate.
     *
     * @param rootPath The root path, which is {@code null} or empty when there is none.
     * @see GPathRootPath#join(String, String)
     */
    String joinRootPath(String rootPath, String key);
}
