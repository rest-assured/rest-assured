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

package com.jayway.restassured.config;

import com.jayway.restassured.internal.http.ContentEncoding;
import java.util.Arrays;

/**
 * Configure the "Accept-Encoding" header for REST Assured.
 * Here you can define a default "Accept-Encoding" value that'll be used for each request.
 */
public class AcceptEncodingConfig {
    /** Default "Accept-Encoding" header in requests is "gzip,deflate". */
    public static final AcceptEncodingConfig DEFAULT = new AcceptEncodingConfig(ContentEncoding.Type.GZIP, ContentEncoding.Type.DEFLATE);
    /** Configuration for no "Accept-Encoding" header in requests. */
    public static final AcceptEncodingConfig NONE = new AcceptEncodingConfig();
    /** "Accept-Encoding" header in requests is "gzip". */
    public static final AcceptEncodingConfig GZIP = new AcceptEncodingConfig(ContentEncoding.Type.GZIP);
    /** "Accept-Encoding" header in requests is "compress". */
    public static final AcceptEncodingConfig COMPRESS = new AcceptEncodingConfig(ContentEncoding.Type.COMPRESS);
    /** "Accept-Encoding" header in requests is "deflate". */
    public static final AcceptEncodingConfig DEFLATE = new AcceptEncodingConfig(ContentEncoding.Type.DEFLATE);
    
    private final ContentEncoding.Type[] acceptEncodings;
    
    private AcceptEncodingConfig(ContentEncoding.Type... acceptEncodings) {
        this.acceptEncodings = acceptEncodings;
    }
    
    /**
     * Returns the list of the "Accept-Encoding" header values.
     * @return 
     */
    protected ContentEncoding.Type[] getAcceptedEncodings() {
        return Arrays.copyOf(acceptEncodings, acceptEncodings.length);
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < acceptEncodings.length; i++) {
            result.append(acceptEncodings[i].name().toLowerCase());
            if (i < acceptEncodings.length - 1) {
                result.append(",");
            }
        }
        return result.toString();
    }
}
