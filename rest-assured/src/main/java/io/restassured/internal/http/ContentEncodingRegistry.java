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

import io.restassured.config.DecoderConfig;
import io.restassured.internal.http.ContentEncoding.Type;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.AbstractHttpClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of available content-encoding handlers.
 *
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 */
public class ContentEncodingRegistry {

    private final boolean useNoWrapForInflateDecoding;
    protected Map<String, ContentEncoding> availableEncoders;

    public ContentEncodingRegistry(DecoderConfig decoderConfig) {
        useNoWrapForInflateDecoding = (decoderConfig == null ? DecoderConfig.decoderConfig() : decoderConfig).shouldUseNoWrapForInflateDecoding();
        availableEncoders = getDefaultEncoders();
    }


    /**
     * This implementation adds a {@link GZIPEncoding} and {@link DeflateEncoding}
     * handler to the registry.  Override this method to provide a different set
     * of defaults.
     *
     * @return a map to content-encoding strings to {@link ContentEncoding} handlers.
     */
    protected Map<String, ContentEncoding> getDefaultEncoders() {
        Map<String, ContentEncoding> map = new HashMap<String, ContentEncoding>();
        map.put(Type.GZIP.toString(), new GZIPEncoding());
        map.put(Type.DEFLATE.toString(), new DeflateEncoding(useNoWrapForInflateDecoding));
        return map;
    }

    /**
     * Add the request and response interceptors to the {@link HttpClient},
     * which will provide transparent decoding of the given content-encoding
     * types.  This method is called by HTTPBuilder and probably should not need
     * be modified by sub-classes.
     *
     * @param client    client on which to set the request and response interceptors
     * @param encodings encoding name (either a {@link ContentEncoding.Type} or
     *                  a <code>content-encoding</code> string.
     */
    void setInterceptors(final AbstractHttpClient client, Object... encodings) {
        // remove any encoding interceptors that are already set
        client.removeRequestInterceptorByClass(ContentEncoding.RequestInterceptor.class);
        client.removeResponseInterceptorByClass(ContentEncoding.ResponseInterceptor.class);

        for (Object encName : encodings) {
            ContentEncoding enc = availableEncoders.get(encName.toString());
            if (enc == null) continue;
            client.addRequestInterceptor(enc.getRequestInterceptor());
            client.addResponseInterceptor(enc.getResponseInterceptor());
        }
    }
}
