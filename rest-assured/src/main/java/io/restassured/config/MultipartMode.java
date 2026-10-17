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

package io.restassured.config;

/**
 * How multi-part requests are written.
 *
 * @see HttpClientConfig#multipartMode(MultipartMode)
 */
public enum MultipartMode {
    /**
     * Follows RFC 822, RFC 2045 and RFC 2046 strictly. This is the default.
     */
    STRICT,
    /**
     * Writes only the <code>Content-Disposition</code> header of each part, the way browsers do, and lets
     * {@link MultiPartConfig} set the charset of file names and of the default subtype.
     */
    BROWSER_COMPATIBLE,
    /**
     * Follows RFC 6532, which allows UTF-8 in part headers.
     */
    RFC6532
}
