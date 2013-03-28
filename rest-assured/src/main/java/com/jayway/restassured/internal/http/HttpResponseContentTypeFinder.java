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

package com.jayway.restassured.internal.http;

import org.apache.http.HttpResponse;

/**
 * @see com.jayway.restassured.http.ContentType
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 */
public class HttpResponseContentTypeFinder {
    /**
     * Helper method to get the content-type string from the response
     * (no charset).
     * @param resp
     */
    public static String findContentType(HttpResponse resp) {
        if ( resp.getEntity() == null )
            throw new IllegalArgumentException( "Response does not contain data" );
        if ( resp.getEntity().getContentType() == null )
            throw new IllegalArgumentException( "Response does not have a content-type header" );
        try {
            return resp.getEntity().getContentType().getValue();
        }
        catch ( RuntimeException ex ) {  // NPE or OOB Exceptions
            throw new IllegalArgumentException( "Could not parse content-type from response" );
        }
    }
}