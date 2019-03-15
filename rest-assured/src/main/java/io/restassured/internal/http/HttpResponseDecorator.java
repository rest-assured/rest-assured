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
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import java.util.Iterator;
import java.util.Locale;

/**
 * This class is a wrapper for {@link HttpResponse}, which allows for 
 * simplified header access, as well as carrying the auto-parsed response data.
 * (see {@link HTTPBuilder#parseResponse(HttpResponse, Object)}).
 * 
 * @see HeadersDecorator
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 * @since 0.5.0
 */
public class HttpResponseDecorator implements HttpResponse {
	
	HeadersDecorator headers = null;
	HttpResponse responseBase;
	HttpContextDecorator context;
	Object responseData;
	
	public HttpResponseDecorator( HttpResponse base, Object parsedResponse ) {
		this( base, null, parsedResponse );
	}
	
	public HttpResponseDecorator( HttpResponse base, HttpContextDecorator context, Object parsedResponse ) {
		this.responseBase = base;
		this.context = context;
		this.responseData = parsedResponse;
	}
	
	/** 
	 * Return a {@link HeadersDecorator}, which provides a more Groovy API for 
	 * accessing response headers.
	 * @return the headers for this response
	 */
	public HeadersDecorator getHeaders() {
		if ( headers == null ) headers = new HeadersDecorator();
		return headers;
	}
	
	/**
	 * Quickly determine if the request resulted in an error code.
	 * @return true if the response code is within the range of 
	 *   {@link Status#SUCCESS}
	 */
	public boolean isSuccess() {
		return Status.find( getStatus() ) == Status.SUCCESS;
	}
	
	/**
	 * Get the response status code.
	 * @see StatusLine#getStatusCode()
	 * @return the HTTP response code.
	 */
	public int getStatus() {
		return responseBase.getStatusLine().getStatusCode();
	}
	
	/**
	 * Get the content-type for this response.
	 * @see HttpResponseContentTypeFinder#findContentType(HttpResponse)
	 * @return the content-type string, without any charset information.
	 */
	public String getContentType() {
		return HttpResponseContentTypeFinder.findContentType(responseBase);
	}
	
	/**
	 * Return the parsed data from this response body.
	 * @return the parsed response object, or <code>null</code> if the response
	 * does not contain any data.
	 */
	public Object getData() { return this.responseData; }
	
	void setData( Object responseData ) { this.responseData = responseData; }
	
	/**
	 * Get the execution context used during this request
	 * @see ExecutionContext
	 * @return the {@link HttpContext} 
	 */
	public HttpContextDecorator getContext() { return this.context; }
	
	/**
	 * This class is returned by {@link HttpResponseDecorator#getHeaders()}.
	 * It provides three "Groovy" ways to access headers: 
	 * <dl>
	 *   <dt>Bracket notation</dt><dd><code>resp.headers['Content-Type']</code> 
	 *   	returns the {@link Header} instance</dd>
	 *   <dt>Property notation</dt><dd><code>resp.headers.'Content-Type'</code>
	 *   	returns the {@link Header#getValue() header value}</dd>
	 *   <dt>Iterator methods</dt><dd>Iterates over each Header:
	 * <pre>resp.headers.each {
	 *   println "${it.name} : ${it.value}"
	 * }</pre></dd>
	 * </dl>
	 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
	 * @since 0.5.0
	 */
	public final class HeadersDecorator implements Iterable<Object> {
		
		/**
		 * Access the named header value, using bracket form.  For example,
		 * <code>response.headers['Content-Encoding']</code>
		 * @see HttpResponse#getFirstHeader(String)
		 * @param name header name, e.g. <code>Content-Type<code>
		 * @return the {@link Header}, or <code>null</code> if it does not exist
		 *  in this response 
		 */
		public Header getAt( String name ) {
			return responseBase.getFirstHeader( name );
		}
		
		/**
		 * Allow property-style access to header values.  This is the same as
		 * {@link #getAt(String)}, except it simply returns the header's String 
		 * value, instead of the Header object.
		 * 
		 * @param name header name, e.g. <code>Content-Type<code>
		 * @return the {@link Header}, or <code>null</code> if it does not exist
		 *  in this response 
		 */
		protected String propertyMissing( String name ) {
			Header h = this.getAt( name );
			return h != null ? h.getValue() : null;
		}		
		
		/**
		 * Used to allow Groovy iteration methods over the response headers.
		 * For example:
		 * <pre>response.headers.each {
		 *   println "${it.name} : ${it.value}"
		 * }</pre>
		 */
		@SuppressWarnings("unchecked")
		public Iterator<Object> iterator() {
			return responseBase.headerIterator();
		}
	}


	public HttpEntity getEntity() {
		return responseBase.getEntity();
	}

	public Locale getLocale() {
		return responseBase.getLocale();
	}

	public StatusLine getStatusLine() {
		return responseBase.getStatusLine();
	}

	public void setEntity( HttpEntity arg0 ) {
		responseBase.setEntity( arg0 );
	}

	public void setLocale( Locale arg0 ) {
		responseBase.setLocale( arg0 );
	}

	public void setReasonPhrase( String arg0 ) throws IllegalStateException {
		responseBase.setReasonPhrase( arg0 );
	}

	public void setStatusCode( int arg0 ) throws IllegalStateException {
		responseBase.setStatusCode( arg0 );
	}

	public void setStatusLine( StatusLine arg0 ) {
		responseBase.setStatusLine( arg0 );
	}

	public void setStatusLine( ProtocolVersion arg0, int arg1 ) {
		responseBase.setStatusLine( arg0, arg1 );
	}

	public void setStatusLine( ProtocolVersion arg0, int arg1, String arg2 ) {
		responseBase.setStatusLine( arg0, arg1, arg2 );
	}

	public void addHeader( Header arg0 ) {
		responseBase.addHeader( arg0 );
	}

	public void addHeader( String arg0, String arg1 ) {
		responseBase.addHeader( arg0, arg1 );
	}

	public boolean containsHeader( String arg0 ) {
		return responseBase.containsHeader( arg0 );
	}

	public Header[] getAllHeaders() {
		return responseBase.getAllHeaders();
	}

	public Header getFirstHeader( String arg0 ) {
		return responseBase.getFirstHeader( arg0 );
	}

	public Header[] getHeaders( String arg0 ) {
		return responseBase.getHeaders( arg0 );
	}

	public Header getLastHeader( String arg0 ) {
		return responseBase.getLastHeader( arg0 );
	}

	public HttpParams getParams() {
		return responseBase.getParams();
	}

	public ProtocolVersion getProtocolVersion() {
		return responseBase.getProtocolVersion();
	}

	public HeaderIterator headerIterator() {
		return responseBase.headerIterator();
	}

	public HeaderIterator headerIterator( String arg0 ) {
		return responseBase.headerIterator( arg0 );
	}

	public void removeHeader( Header arg0 ) {
		responseBase.removeHeader( arg0 );
	}

	public void removeHeaders( String arg0 ) {
		responseBase.removeHeaders( arg0 );
	}

	public void setHeader( Header arg0 ) {
		responseBase.setHeader( arg0 );
	}

	public void setHeader( String arg0, String arg1 ) {
		responseBase.setHeader( arg0, arg1 );
	}

	public void setHeaders( Header[] arg0 ) {
		responseBase.setHeaders( arg0 );
	}

	public void setParams( HttpParams arg0 ) {
		responseBase.setParams( arg0 );
	}
}