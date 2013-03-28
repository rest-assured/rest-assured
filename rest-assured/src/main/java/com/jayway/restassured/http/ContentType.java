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

package com.jayway.restassured.http;

import com.jayway.restassured.internal.http.ContentTypeExtractor;
import org.apache.commons.collections.iterators.ArrayIterator;

import java.util.Iterator;

import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;

/**
 * Enumeration of common <a href="http://www.iana.org/assignments/media-types/">IANA</a>
 * content-types.  This may be used to specify a request or response 
 * content-type more easily than specifying the full string each time.  i.e.
 * <pre>
 * http.request( GET, JSON ) {...}</pre>
 * 
 * Is roughly equivalent to:
 * <pre>
 * http.request( GET, 'application/json' )</pre>
 * 
 * The only difference being, equivalent content-types (i.e. 
 * <code>application/xml</code> and <code>text/xml</code> are all added to the 
 * request's <code>Accept</code> header.  By default, all equivalent content-types
 * are handled the same by the {@link com.jayway.restassured.internal.http.EncoderRegistry} and {@link com.jayway.restassured.internal.http.HttpResponseContentTypeFinder}
 * as well. 
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 */
public enum ContentType {

	/** <code>&#42;/*</code> */
	ANY("*/*"),
	/** <code>text/plain</code> */
	TEXT("text/plain"), 
	/** 
	 * <ul>
	 *  <li><code>application/json</code></li>
	 *  <li><code>application/javascript</code></li>
	 *  <li><code>text/javascript</code></li>
	 * </ul>
	 */
	JSON("application/json","application/javascript","text/javascript"),
	/** 
	 * <ul>
	 *  <li><code>application/json</code></li>
	 *  <li><code>application/javascript</code></li>
	 *  <li><code>text/javascript</code></li>
	 * </ul>
	 */
	XML("application/xml","text/xml","application/xhtml+xml"),
	/** <code>text/html</code> */
	HTML("text/html"),
	/** <code>application/x-www-form-urlencoded</code> */
	URLENC("application/x-www-form-urlencoded"),
	/** <code>application/octet-stream</code> */
	BINARY("application/octet-stream");

    private static final String PLUS_XML = "+xml";
    private static final String PLUS_JSON = "+json";
    private static final String PLUS_HTML = "+html";

    private final String[] ctStrings;
	public String[] getContentTypeStrings() { return ctStrings; } 
	@Override public String toString() { return ctStrings[0]; }
	
	/**
	 * Builds a string to be used as an HTTP <code>Accept</code> header 
	 * value, i.e. "application/xml, text/xml"
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String getAcceptHeader() {
		Iterator<String> iter = new ArrayIterator(ctStrings);
		StringBuilder sb = new StringBuilder();
		while ( iter.hasNext() ) {
			sb.append( iter.next() );
			if ( iter.hasNext() ) sb.append( ", " );
		}
		return sb.toString();
	}
	
	private ContentType( String... contentTypes ) {
		this.ctStrings = contentTypes;
	}

    public static ContentType fromContentType(String contentType) {
        if(contentType == null) {
            return null;
        }
        contentType = ContentTypeExtractor.getContentTypeWithoutCharset(contentType.toLowerCase());
        final ContentType foundContentType;
        if(contains(XML.ctStrings, contentType) || endsWithIgnoreCase(contentType, PLUS_XML)) {
            foundContentType = XML;
        } else if(contains(JSON.ctStrings, contentType) || endsWithIgnoreCase(contentType, PLUS_JSON)) {
            foundContentType = JSON;
        } else if(contains(TEXT.ctStrings, contentType)) {
            foundContentType = TEXT;
        } else if(contains(HTML.ctStrings, contentType) || endsWithIgnoreCase(contentType, PLUS_HTML)) {
            foundContentType = HTML;
        } else {
            foundContentType = null;
        }
        return foundContentType;
    }
}	