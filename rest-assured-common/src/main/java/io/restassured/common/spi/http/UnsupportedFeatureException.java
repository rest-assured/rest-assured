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

package io.restassured.common.spi.http;

/**
 * Thrown by a client for something its library can't do. REST Assured adds which client was used and how it was chosen.
 */
public class UnsupportedFeatureException extends UnsupportedOperationException {

    private final String feature;
    private final String workaround;

    /**
     * @param feature    what can't be done, as the end of a sentence, for example {@code send a Host header}
     * @param workaround how to do it anyway, for example {@code start the JVM with -Djdk.httpclient.allowRestrictedHeaders=host},
     *                   or {@code null}
     */
    public UnsupportedFeatureException(String feature, String workaround) {
        super(workaround == null ? "The HTTP client can't " + feature + "." : "The HTTP client can't " + feature + ". To do it anyway, " + workaround + ".");
        this.feature = feature;
        this.workaround = workaround;
    }

    /**
     * @return what can't be done
     */
    public String feature() {
        return feature;
    }

    /**
     * @return how to do it anyway, or {@code null}
     */
    public String workaround() {
        return workaround;
    }
}
