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

import org.apache.http.*;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Base class for handing content-encoding.  
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 */
public abstract class ContentEncoding {

	public static final String ACCEPT_ENC_HDR = "Accept-Encoding";
	public static final String CONTENT_ENC_HDR = "Content-Encoding";

	protected abstract String getContentEncoding();
	protected abstract HttpEntity wrapResponseEntity( HttpEntity raw );

	public HttpRequestInterceptor getRequestInterceptor() {
		return new RequestInterceptor();
	}
	
	public HttpResponseInterceptor getResponseInterceptor() {
		return new ResponseInterceptor();
	}
	
	/**
	 * Enumeration of common content-encodings.
	 */
	public static enum Type {
		GZIP,
		COMPRESS,
		DEFLATE;

		/** Prints the value as it should appear in an HTTP header */
		@Override public String toString() {
			return this.name().toLowerCase();
		}
	}
	
	/**
	 * Request interceptor that adds the correct <code>Accept</code> header
	 * to the outgoing request.
	 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
	 */
	protected class RequestInterceptor implements HttpRequestInterceptor {
		public void process( final HttpRequest req,
				final HttpContext context ) throws HttpException, IOException {
			
			// set the Accept-Encoding header:
			String encoding = getContentEncoding();			
			if ( !req.containsHeader( ACCEPT_ENC_HDR ) )
				req.addHeader( ACCEPT_ENC_HDR, encoding );

			else {
				StringBuilder values = new StringBuilder();
				for ( Header h : req.getHeaders( ACCEPT_ENC_HDR ) )
					values.append( h.getValue() ).append( "," );

				String encList = (!values.toString().contains( encoding )) ? values
						.append( encoding ).toString()
						: values.toString().substring( 0, values.lastIndexOf( "," ) );
						
				req.setHeader( ACCEPT_ENC_HDR, encList );
			}

			//TODO compress request and add content-encoding header.
		}
	}

	/**
	 * Response interceptor that filters the response stream to decode the 
	 * compressed content before it is passed on to the parser.
	 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
	 */
	protected class ResponseInterceptor implements HttpResponseInterceptor {
		public void process( final HttpResponse response, final HttpContext context ) 
				throws HttpException, IOException {

			if ( hasEncoding( response, getContentEncoding() ) )
				response.setEntity( wrapResponseEntity( response.getEntity() ) );
		}
		
		protected boolean hasEncoding( final HttpResponse response, final String encoding ) {
			HttpEntity entity = response.getEntity();
			if ( entity == null ) return false;
			Header ceHeader = entity.getContentEncoding();
			if ( ceHeader == null ) return false;

			HeaderElement[] codecs = ceHeader.getElements();
			for ( int i = 0; i < codecs.length; i++ )
				if ( encoding.equalsIgnoreCase( codecs[i].getName() ) )
					return true;
			
			return false;
		}
	}
}