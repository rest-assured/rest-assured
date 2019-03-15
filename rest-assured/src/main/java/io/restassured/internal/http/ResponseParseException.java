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

/**
 * Thrown when a response body is parsed unsuccessfully.  This most often 
 * occurs when a server returns an error status code and sends a different 
 * content-type body from what was expected.  You can inspect the response 
 * content-type by calling <code>ex.response.contentType</code>.
 * 
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 * @since 0.5.0
 */
public class ResponseParseException extends HttpResponseException {
	
	private static final long serialVersionUID = -1398234959324603287L;

	/* TODO this is a bit wonky because org.apache.http...HttpResponseException
	   does not have a constructor to pass the 'cause'.  But I want this to 
	   extend HttpResponseException so that one exception type can catch 
	   everything thrown from HttpBuilder. */
	private Throwable cause;
	
	public ResponseParseException( HttpResponseDecorator response, Throwable cause ) {
		super( response );
		this.cause = cause;
	}
	
	@Override public Throwable getCause() { return this.cause; }
}
