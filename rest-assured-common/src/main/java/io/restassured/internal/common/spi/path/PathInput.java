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

import java.io.InputStream;

/**
 * The body to parse, either as text or as a stream.
 *
 * @param text    The body as text, or {@code null} for a stream.
 * @param stream  The body as a stream, or {@code null} for text.
 * @param charset The charset of the stream. It's required for JSON. For XML and HTML, {@code null} lets the parser
 *                read the encoding from the document.
 */
public record PathInput(String text, InputStream stream, String charset) {

    public static PathInput ofText(String text) {
        return new PathInput(text, null, null);
    }

    public static PathInput ofStream(InputStream stream, String charset) {
        return new PathInput(null, stream, charset);
    }

    public boolean isText() {
        return stream == null;
    }
}
