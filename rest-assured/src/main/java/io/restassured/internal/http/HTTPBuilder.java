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

import groovy.lang.Closure;
import io.restassured.config.DecoderConfig;
import io.restassured.config.EncoderConfig;
import io.restassured.config.OAuthConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.protocol.HttpContext;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.MethodClosure;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

/**
 * <p>
 * Groovy DSL for easily making HTTP requests, and handling request and response
 * data.  This class adds a number of convenience mechanisms built on top of
 * Apache HTTPClient for things like URL-encoded POSTs and REST requests that
 * require building and parsing JSON or XML.  Convenient access to a few common
 * authentication methods is also available.</p>
 * <p>
 * <p>
 * <h3>Conventions</h3>
 * <p>HTTPBuilder has properties for default headers, URI, contentType, etc.
 * All of these values are also assignable (and in many cases, in much finer
 * detail) from the {@link RequestConfigDelegate} as well.  In any cases where the value
 * is not set on the delegate (from within a request closure,) the builder's
 * default value is used.  </p>
 * <p>
 * <p>For instance, any methods that do not take a <code>uri</code> parameter
 * assume you will set the <code>uri</code> property in the request closure or
 * use HTTPBuilder's assigned {@link #getUri() default URI}.</p>
 * <p>
 * <p>
 * <h3>Response Parsing</h3>
 * <p>By default, HTTPBuilder uses {@link ContentType#ANY} as the default
 * content-type.  This means the value of the request's <code>Accept</code>
 * header is <code>&#42;/*</code>, and the response parser is determined
 * based on the response <code>content-type</code> header. </p>
 * <p>
 * <p><strong>If</strong> any contentType is given (either in
 * {@link #setContentType(Object)} or as a request method parameter), the
 * builder will attempt to parse the response using that content-type,
 * regardless of what the server actually responds with.  </p>
 * <p>
 * <p>
 * <h3>Examples:</h3>
 * Perform an HTTP GET and print the response:
 * <pre>
 *   def http = new HTTPBuilder('http://www.google.com')
 *
 *   http.get( path : '/search',
 *             contentType : TEXT,
 *             query : [q:'Groovy'] ) { resp, reader ->
 *     println "response status: ${resp.statusLine}"
 *     println 'Response data: -----'
 *     System.out << reader
 *     println '\n--------------------'
 *   }
 * </pre>
 * <p>
 * Long form for other HTTP methods, and response-code-specific handlers.
 * This is roughly equivalent to the above example.
 * <p>
 * <pre>
 *   def http = new HTTPBuilder('http://www.google.com/search?q=groovy')
 *
 *   http.request( GET, TEXT ) { req ->
 *
 *     // executed for all successful responses:
 *     response.success = { resp, reader ->
 *       println 'my response handler!'
 *       assert resp.statusLine.statusCode == 200
 *       println resp.statusLine
 *       System.out << reader // print response stream
 *     }
 *
 *     // executed only if the response status code is 401:
 *     response.'404' = { resp ->
 *       println 'not found!'
 *     }
 *   }
 * </pre>
 * <p>
 * You can also set a default response handler called for any status
 * code > 399 that is not matched to a specific handler. Setting the value
 * outside a request closure means it will apply to all future requests with
 * this HTTPBuilder instance:
 * <pre>
 *   http.handler.failure = { resp ->
 *     println "Unexpected failure: ${resp.statusLine}"
 *   }
 * </pre>
 * <p>
 * <p>
 * And...  Automatic response parsing for registered content types!
 * <p>
 * <pre>
 *   http.request( 'http://ajax.googleapis.com', GET, JSON ) {
 *     uri.path = '/ajax/services/search/web'
 *     uri.query = [ v:'1.0', q: 'Calvin and Hobbes' ]
 *
 *     response.success = { resp, json ->
 *       assert json.size() == 3
 *       println "Query response: "
 *       json.responseData.results.each {
 *         println "  ${it.titleNoFormatting} : ${it.visibleUrl}"
 *       }
 *     }
 *   }
 * </pre>
 *
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 */
public abstract class HTTPBuilder {

    protected AbstractHttpClient client;
    protected URIBuilder defaultURI = null;
    protected AuthConfig auth;

    protected final Log log = LogFactory.getLog(getClass());

    protected Object defaultContentType = ContentType.ANY;
    protected Object defaultRequestContentType = null;
    protected final Map<Object, Closure> defaultResponseHandlers = new StringHashMap<Closure>(buildDefaultResponseHandlers());
    protected ContentEncodingRegistry contentEncodingHandler;

    protected final Map<Object, Object> defaultRequestHeaders = new StringHashMap<Object>();

    protected EncoderRegistry encoders;
    protected HttpResponseContentTypeFinder parsers = new HttpResponseContentTypeFinder();
    private EncoderConfig encoderConfig;
    private boolean urlEncodingEnabled;

    public HTTPBuilder(boolean urlEncodingEnabled, EncoderConfig encoderConfig, DecoderConfig decoderConfig, OAuthConfig oAuthConfig,
                       AbstractHttpClient client) {
        this.client = client;
        this.auth = new AuthConfig(this, oAuthConfig);
        this.contentEncodingHandler = new ContentEncodingRegistry(decoderConfig);
        this.setContentEncoding(ContentEncoding.Type.GZIP, ContentEncoding.Type.DEFLATE);
        this.encoderConfig = encoderConfig == null ? new EncoderConfig() : encoderConfig;
        this.urlEncodingEnabled = urlEncodingEnabled;
        encoders = new EncoderRegistry();
        if (this.encoderConfig.hasContentEncoders()) {
            Map<String, ContentType> customEncoders = this.encoderConfig.contentEncoders();
            for (Map.Entry<String, ContentType> entry : customEncoders.entrySet()) {
                // Get the pre-defined encoder for the given content-type
                Closure actualEncoder = encoders.getAt(entry.getValue().getContentTypeStrings()[0]);
                encoders.putAt(entry.getKey(), actualEncoder);
            }
        }
    }

    /**
     * Give a default URI to be used for all request methods that don't
     * explicitly take a URI parameter.
     *
     * @param defaultURI either a {@link URL}, {@link URI} or object whose
     *                   <code>toString()</code> produces a valid URI string.  See
     *                   {@link URIBuilder#convertToURI(Object)}.
     * @throws URISyntaxException if the given argument does not represent a valid URI
     */
    public HTTPBuilder(Object defaultURI, boolean urlEncodingEnabled, EncoderConfig encoderConfig, DecoderConfig decoderConfig, OAuthConfig oAuthConfig,
                       AbstractHttpClient client) {
        this(urlEncodingEnabled, encoderConfig, decoderConfig, oAuthConfig, client);
        try {
            this.defaultURI = new URIBuilder(URIBuilder.convertToURI(defaultURI), this.urlEncodingEnabled, this.encoderConfig);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Give a default URI to be used for all request methods that don't
     * explicitly take a URI parameter, and a default content-type to be used
     * for request encoding and response parsing.
     *
     * @param defaultURI         either a {@link URL}, {@link URI} or object whose
     *                           <code>toString()</code> produces a valid URI string.  See
     *                           {@link URIBuilder#convertToURI(Object)}.
     * @param defaultContentType content-type string.  See {@link ContentType}
     *                           for common types.
     * @throws URISyntaxException if the uri argument does not represent a valid URI
     */
    public HTTPBuilder(Object defaultURI, Object defaultContentType, boolean urlEncodingEnabled, EncoderConfig encoderConfig,
                       DecoderConfig decoderConfig, OAuthConfig oAuthConfig, AbstractHttpClient client) throws URISyntaxException {
        this(urlEncodingEnabled, encoderConfig, decoderConfig, oAuthConfig, client);
        this.defaultURI = new URIBuilder(URIBuilder.convertToURI(defaultURI), urlEncodingEnabled, this.encoderConfig);
        this.defaultContentType = defaultContentType;
    }

    /**
     * <p>Convenience method to perform an HTTP GET.  It will use the HTTPBuilder's
     * {@link #getHandler() registered response handlers} to handle success or
     * failure status codes.  By default, the <code>success</code> response
     * handler will attempt to parse the data and simply return the parsed
     * object.</p>
     * <p>
     * <p><strong>Note:</strong> If using the {@link #defaultSuccessHandler(HttpResponseDecorator, Object)
     * default <code>success</code> response handler}, be sure to read the
     * caveat regarding streaming response data.</p>
     *
     * @param args see {@link RequestConfigDelegate#setPropertiesFromMap(Map)}
     * @return whatever was returned from the response closure.
     * @throws URISyntaxException      if a uri argument is given which does not
     *                                 represent a valid URI
     * @throws IOException
     * @throws ClientProtocolException
     * @see #getHandler()
     * @see #defaultSuccessHandler(HttpResponseDecorator, Object)
     * @see #defaultFailureHandler(HttpResponseDecorator)
     */
    public Object get(Map<String, ?> args)
            throws ClientProtocolException, IOException, URISyntaxException {
        return this.get(args, null);
    }

    /**
     * <p>Convenience method to perform an HTTP GET.  The response closure will
     * be called only on a successful response.  </p>
     * <p>
     * <p>A 'failed' response (i.e. any HTTP status code > 399) will be handled
     * by the registered 'failure' handler.  The
     * {@link #defaultFailureHandler(HttpResponseDecorator) default failure handler}
     * throws an {@link HttpResponseException}.</p>
     *
     * @param args            see {@link RequestConfigDelegate#setPropertiesFromMap(Map)}
     * @param responseClosure code to handle a successful HTTP response
     * @return any value returned by the response closure.
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException      if a uri argument is given which does not
     *                                 represent a valid URI
     */
    public Object get(Map<String, ?> args, Closure responseClosure)
            throws ClientProtocolException, IOException, URISyntaxException {
        RequestConfigDelegate delegate = new RequestConfigDelegate(new HttpGet(),
                this.defaultContentType,
                this.defaultRequestHeaders,
                this.defaultResponseHandlers);

        delegate.setPropertiesFromMap(args);
        if (responseClosure != null) delegate.getResponse().put(
                Status.SUCCESS, responseClosure);
        return this.doRequest(delegate);
    }

    /**
     * <p>Convenience method to perform an HTTP POST.  It will use the HTTPBuilder's
     * {@link #getHandler() registered response handlers} to handle success or
     * failure status codes.  By default, the <code>success</code> response
     * handler will attempt to parse the data and simply return the parsed
     * object. </p>
     * <p>
     * <p><strong>Note:</strong> If using the {@link #defaultSuccessHandler(HttpResponseDecorator, Object)
     * default <code>success</code> response handler}, be sure to read the
     * caveat regarding streaming response data.</p>
     *
     * @param args see {@link RequestConfigDelegate#setPropertiesFromMap(Map)}
     * @return whatever was returned from the response closure.
     * @throws IOException
     * @throws URISyntaxException      if a uri argument is given which does not
     *                                 represent a valid URI
     * @throws ClientProtocolException
     * @see #getHandler()
     * @see #defaultSuccessHandler(HttpResponseDecorator, Object)
     * @see #defaultFailureHandler(HttpResponseDecorator)
     */
    public Object post(Map<String, ?> args)
            throws ClientProtocolException, URISyntaxException, IOException {
        return this.post(args, null);
    }

    /**
     * <p>
     * Convenience method to perform an HTTP form POST.  The response closure will be
     * called only on a successful response.</p>
     * <p>
     * <p>A 'failed' response (i.e. any
     * HTTP status code > 399) will be handled by the registered 'failure'
     * handler.  The {@link #defaultFailureHandler(HttpResponseDecorator) default
     * failure handler} throws an {@link HttpResponseException}.</p>
     * <p>
     * <p>The request body (specified by a <code>body</code> named parameter)
     * will be converted to a url-encoded form string unless a different
     * <code>requestContentType</code> named parameter is passed to this method.
     * (See {@link EncoderRegistry#encodeForm(Map)}.) </p>
     *
     * @param args            see {@link RequestConfigDelegate#setPropertiesFromMap(Map)}
     * @param responseClosure code to handle a successful HTTP response
     * @return any value returned by the response closure.
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException      if a uri argument is given which does not
     *                                 represent a valid URI
     */
    public Object post(Map<String, ?> args, Closure responseClosure)
            throws URISyntaxException, ClientProtocolException, IOException {
        RequestConfigDelegate delegate = new RequestConfigDelegate(new HttpPost(),
                this.defaultContentType,
                this.defaultRequestHeaders,
                this.defaultResponseHandlers);

        /* by default assume the request body will be URLEncoded, but allow
             the 'requestContentType' named argument to override this if it is
             given */
        delegate.setRequestContentType(ContentType.URLENC.toString());
        delegate.setPropertiesFromMap(args);

        if (responseClosure != null) delegate.getResponse().put(
                Status.SUCCESS.toString(), responseClosure);

        return this.doRequest(delegate);
    }

    /**
     * <p>Convenience method to perform an HTTP PATCH.  It will use the HTTPBuilder's
     * {@link #getHandler() registered response handlers} to handle success or
     * failure status codes.  By default, the <code>success</code> response
     * handler will attempt to parse the data and simply return the parsed
     * object. </p>
     * <p>
     * <p><strong>Note:</strong> If using the {@link #defaultSuccessHandler(HttpResponseDecorator, Object)
     * default <code>success</code> response handler}, be sure to read the
     * caveat regarding streaming response data.</p>
     *
     * @param args see {@link RequestConfigDelegate#setPropertiesFromMap(Map)}
     * @return whatever was returned from the response closure.
     * @throws IOException
     * @throws URISyntaxException      if a uri argument is given which does not
     *                                 represent a valid URI
     * @throws ClientProtocolException
     * @see #getHandler()
     * @see #defaultSuccessHandler(HttpResponseDecorator, Object)
     * @see #defaultFailureHandler(HttpResponseDecorator)
     */
    public Object patch(Map<String, ?> args)
            throws ClientProtocolException, URISyntaxException, IOException {
        return this.patch(args, null);
    }

    /**
     * <p>
     * Convenience method to perform an HTTP form PATCH.  The response closure will be
     * called only on a successful response.</p>
     * <p>
     * <p>A 'failed' response (i.e. any
     * HTTP status code > 399) will be handled by the registered 'failure'
     * handler.  The {@link #defaultFailureHandler(HttpResponseDecorator) default
     * failure handler} throws an {@link HttpResponseException}.</p>
     * <p>
     * <p>The request body (specified by a <code>body</code> named parameter)
     * will be converted to a url-encoded form string unless a different
     * <code>requestContentType</code> named parameter is passed to this method.
     * (See {@link EncoderRegistry#encodeForm(Map)}.) </p>
     *
     * @param args            see {@link RequestConfigDelegate#setPropertiesFromMap(Map)}
     * @param responseClosure code to handle a successful HTTP response
     * @return any value returned by the response closure.
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException      if a uri argument is given which does not
     *                                 represent a valid URI
     */
    public Object patch(Map<String, ?> args, Closure responseClosure)
            throws URISyntaxException, ClientProtocolException, IOException {
        RequestConfigDelegate delegate = new RequestConfigDelegate(new HttpPatch(),
                this.defaultContentType,
                this.defaultRequestHeaders,
                this.defaultResponseHandlers);

        /* by default assume the request body will be URLEncoded, but allow
             the 'requestContentType' named argument to override this if it is
             given */
        delegate.setRequestContentType(ContentType.URLENC.toString());
        delegate.setPropertiesFromMap(args);

        if (responseClosure != null) {
            delegate.getResponse().put(Status.SUCCESS.toString(), responseClosure);
            delegate.getResponse().put(Status.FAILURE.toString(), responseClosure);
        }

        return this.doRequest(delegate);
    }

    /**
     * Make an HTTP request to the default URI, and parse using the default
     * content-type.
     *
     * @param method        {@link HttpRequestFactory HTTP method}
     * @param configClosure request configuration options
     * @return whatever value was returned by the executed response handler.
     * @throws ClientProtocolException
     * @throws IOException
     * @see #request(Object, HttpRequestFactory, Object, Closure)
     */
    public Object request(String method, boolean hasBody, Closure configClosure) throws ClientProtocolException, IOException {
        return this.doRequest(this.defaultURI.toURI(), method, this.defaultContentType, hasBody, configClosure);
    }

    /**
     * Make an HTTP request using the default URI, with the given method,
     * content-type, and configuration.
     *
     * @param method        {@link HttpRequestFactory HTTP method}
     * @param contentType   either a {@link ContentType} or valid content-type string.
     * @param configClosure request configuration options
     * @return whatever value was returned by the executed response handler.
     * @throws ClientProtocolException
     * @throws IOException
     * @see #request(Object, HttpRequestFactory, Object, Closure)
     */
    public Object request(String method, Object contentType, boolean hasBody, Closure configClosure)
            throws ClientProtocolException, IOException {
        return this.doRequest(this.defaultURI.toURI(), method, contentType, hasBody, configClosure);
    }

    /**
     * Make a request for the given HTTP method and content-type, with
     * additional options configured in the <code>configClosure</code>.  See
     * {@link RequestConfigDelegate} for options.
     *
     * @param uri           either a {@link URL}, {@link URI} or object whose
     *                      <code>toString()</code> produces a valid URI string.  See
     *                      {@link URIBuilder#convertToURI(Object)}.
     * @param method        {@link HttpRequestFactory HTTP method}
     * @param contentType   either a {@link ContentType} or valid content-type string.
     * @param configClosure closure from which to configure options like
     *                      {@link RequestConfigDelegate#getUri() uri.path},
     *                      {@link URIBuilder#setQuery(Map) request parameters},
     *                      {@link RequestConfigDelegate#setHeaders(Map) headers},
     *                      {@link RequestConfigDelegate#setBody(Object, Object) request body} and
     *                      {@link RequestConfigDelegate#getResponse() response handlers}.
     * @return whatever value was returned by the executed response handler.
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException      if the uri argument does not represent a valid URI
     */
    public Object request(Object uri, String method, Object contentType, boolean hasBody, Closure configClosure)
            throws ClientProtocolException, IOException, URISyntaxException {
        return this.doRequest(URIBuilder.convertToURI(uri), method, contentType, hasBody, configClosure);
    }

    /**
     * Create a {@link RequestConfigDelegate} from the given arguments, execute the
     * config closure, then pass the delegate to {@link #doRequest(RequestConfigDelegate)},
     * which actually executes the request.
     */
    protected Object doRequest(URI uri, String method, Object contentType, boolean hasBody, Closure configClosure) throws IOException {
        HttpRequestBase reqMethod = HttpRequestFactory.createHttpRequest(uri, method, hasBody);
        RequestConfigDelegate delegate = new RequestConfigDelegate(reqMethod, contentType,
                this.defaultRequestHeaders,
                this.defaultResponseHandlers);
        configClosure.setDelegate(delegate);
        configClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
        configClosure.call(reqMethod);

        return this.doRequest(delegate);
    }

    /**
     * A copy of HTTP builders doRequest method with two exceptions.
     * <ol>
     * <li>The exception is that the entity's content is not closed if no body matchers are specified.</li>
     * <li>If headers contain a list of elements the headers are added and not overridden</li>
     * </ol>
     */
    protected abstract Object doRequest(final RequestConfigDelegate delegate)
            throws ClientProtocolException, IOException;


    /**
     * Parse the response data based on the given content-type.
     * If the given content-type is {@link ContentType#ANY}, the
     * <code>content-type</code> header from the response will be used to
     * determine how to parse the response.
     *
     * @param resp
     * @param contentType
     * @return whatever was returned from the parser retrieved for the given
     * content-type, or <code>null</code> if no parser could be found for this
     * content-type.  The parser will also return <code>null</code> if the
     * response does not contain any content (e.g. in response to a HEAD request).
     * @throws HttpResponseException if there is a error parsing the response
     */
    protected Object parseResponse(HttpResponse resp, Object contentType)
            throws IOException {
        // For HEAD or OPTIONS requests, there should be no response entity.
        if (resp.getEntity() == null) {
            log.debug("Response contains no entity.  Parsed data is null.");
            return null;
        }
        // first, start with the _given_ content-type
        String responseContentType = contentType.toString();
        // if the given content-type is ANY ("*/*") then use the response content-type
        try {
            if (ContentType.ANY.toString().equals(responseContentType))
                responseContentType = HttpResponseContentTypeFinder.findContentType(resp);
        } catch (RuntimeException ex) {
            /* if for whatever reason we can't determine the content-type, but
                * still want to attempt to parse the data, use the BINARY
                * content-type so that the response will be buffered into a
                * ByteArrayInputStream. */
            responseContentType = ContentType.BINARY.toString();
        }

        Object parsedData = null;
        log.debug("Parsing response as: " + responseContentType);
        parsedData = resp.getEntity().getContent();
        if (parsedData == null) log.debug("Parser returned null!");
        else log.debug("Parsed data to instance of: " + parsedData.getClass());
        return parsedData;
    }

    /**
     * Creates default response handlers for {@link Status#SUCCESS success} and
     * {@link Status#FAILURE failure} status codes.  This is used to populate
     * the handler map when a new HTTPBuilder instance is created.
     *
     * @return the default response handler map.
     * @see #defaultSuccessHandler(HttpResponseDecorator, Object)
     * @see #defaultFailureHandler(HttpResponseDecorator)
     */
    protected Map<Object, Closure> buildDefaultResponseHandlers() {
        Map<Object, Closure> map = new StringHashMap<Closure>();
        map.put(Status.SUCCESS,
                new MethodClosure(this, "defaultSuccessHandler"));
        map.put(Status.FAILURE,
                new MethodClosure(this, "defaultFailureHandler"));

        return map;
    }

    /**
     * <p>This is the default <code>response.success</code> handler.  It will be
     * executed if the response is not handled by a status-code-specific handler
     * (i.e. <code>response.'200'= {..}</code>) and no generic 'success' handler
     * is given (i.e. <code>response.success = {..}</code>.)  This handler simply
     * returns the parsed data from the response body.  In most cases you will
     * probably want to define a <code>response.success = {...}</code> handler
     * from the request closure, which will replace the response handler defined
     * by this method.  </p>
     * <p>
     * <p>In practice, a user-supplied response handler closure is
     * <i>designed</i> to handle streaming content so it can be read directly from
     * the response stream without buffering, which will be much more efficient.
     * Therefore, it is recommended that request method variants be used which
     * explicitly accept a response handler closure in these cases.</p>
     *
     * @param resp       HTTP response
     * @param parsedData parsed data as resolved from this instance's {@link HttpResponseContentTypeFinder}
     * @return the parsed data object (whatever the parser returns).
     * @throws ResponseParseException if there is an error buffering a streaming
     *                                response.
     */
    protected Object defaultSuccessHandler(HttpResponseDecorator resp, Object parsedData)
            throws ResponseParseException {
        try {
            //If response is streaming, buffer it in a byte array:
            if (parsedData instanceof InputStream) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                DefaultGroovyMethods.leftShift(buffer, (InputStream) parsedData);
                parsedData = new ByteArrayInputStream(buffer.toByteArray());
            } else if (parsedData instanceof Reader) {
                StringWriter buffer = new StringWriter();
                DefaultGroovyMethods.leftShift(buffer, (Reader) parsedData);
                parsedData = new StringReader(buffer.toString());
            } else if (parsedData instanceof Closeable)
                log.debug("Parsed data is streaming, but will be accessible after " +
                        "the network connection is closed.  Use at your own risk!");
            return parsedData;
        } catch (IOException ex) {
            throw new ResponseParseException(resp, ex);
        }
    }

    /**
     * This is the default <code>response.failure</code> handler.  It will be
     * executed if no status-code-specific handler is set (i.e.
     * <code>response.'404'= {..}</code>).  This default handler will throw a
     * {@link HttpResponseException} when executed.  In most cases you
     * will want to define your own <code>response.failure = {...}</code>
     * handler from the request closure, if you don't want an exception to be
     * thrown for 4xx and 5xx status responses.
     *
     * @param resp
     * @throws HttpResponseException
     */
    protected void defaultFailureHandler(HttpResponseDecorator resp) throws HttpResponseException {
        throw new HttpResponseException(resp);
    }

    /**
     * Retrieve the map of response code handlers.  Each map key is a response
     * code as a string (i.e. '401') or either 'success' or 'failure'.  Use this
     * to set default response handlers, e.g.
     * <pre>builder.handler.'401' = { resp -> println "${resp.statusLine}" }</pre>
     *
     * @return
     * @see Status
     */
    public Map<?, Closure> getHandler() {
        return this.defaultResponseHandlers;
    }

    /**
     * Retrieve the map of registered response content-type parsers.  Use
     * this to set default response parsers, e.g.
     * <pre>
     * builder.parser.'text/javascript' = { resp ->
     * 	  return resp.entity.content // just returns an InputStream
     * }</pre>
     *
     * @return
     */
    public HttpResponseContentTypeFinder getParser() {
        return this.parsers;
    }

    /**
     * Set the default content type that will be used to select the appropriate
     * request encoder and response parser.  The {@link ContentType} enum holds
     * some common content-types that may be used, i.e. <pre>
     * import static ContentType.*
     * builder.contentType = XML
     * </pre>
     * Setting the default content-type does three things:
     * <ol>
     * <li>It tells the builder to encode any {@link RequestConfigDelegate#setBody(Object, Object)
     * request body} as this content-type.  Calling {@link
     * RequestConfigDelegate#setRequestContentType(String)} can override this
     * on a per-request basis.</li>
     * <li>Tells the builder to parse any response as this content-type,
     * regardless of any <code>content-type</code> header that is sent in the
     * response.</li>
     * <li>Sets the <code>Accept</code> header to this content-type for all
     * requests (see {@link ContentType#getAcceptHeader()}).  Note
     * that any <code>Accept</code> header explicitly set either in
     * {@link #setHeaders(Map)} or {@link RequestConfigDelegate#setHeaders(Map)}
     * will override this value.</li>
     * </ol>
     * <p>Additionally, if the content-type is set to {@link ContentType#ANY},
     * HTTPBuilder <i>will</i> rely on the <code>content-type</code> response
     * header to determine how to parse the response data.  This allows the user
     * to rely on response headers if they are accurate, or ignore them and
     * forcibly use a certain response parser if so desired.</p>
     * <p>
     * <p>This value is a default and may always be overridden on a per-request
     * basis by using the {@link #request(Method, Closure)}
     * builder.request( Method, ContentType, Closure )} method or passing a
     * <code>contentType</code> named parameter.
     *
     * @param ct either a {@link ContentType} or string value (i.e. <code>"text/xml"</code>.)
     * @see EncoderRegistry
     * @see HttpResponseContentTypeFinder
     */
    public void setContentType(Object ct) {
        this.defaultContentType = ct;
    }

    /**
     * @return default content type used for request and response.
     */
    public Object getContentType() {
        return this.defaultContentType;
    }

    /**
     * Set acceptable request and response content-encodings.
     *
     * @param encodings each Object should be either a
     *                  {@link ContentEncoding.Type} value, or a <code>content-encoding</code>
     *                  string that is known by the {@link ContentEncodingRegistry}
     * @see ContentEncodingRegistry
     */
    public void setContentEncoding(Object... encodings) {
        this.contentEncodingHandler.setInterceptors(client, encodings);
    }

    /**
     * Set the default URI used for requests that do not explicitly take a
     * <code>uri</code> param.
     *
     * @param uri either a {@link URL}, {@link URI} or object whose
     *            <code>toString()</code> produces a valid URI string.  See
     *            {@link URIBuilder#convertToURI(Object)}.
     * @throws URISyntaxException if the uri argument does not represent a valid URI
     */
    public void setUri(Object uri) throws URISyntaxException {
        this.defaultURI = new URIBuilder(URIBuilder.convertToURI(uri), urlEncodingEnabled, encoderConfig);
    }

    /**
     * Get the default URI used for requests that do not explicitly take a
     * <code>uri</code> param.
     *
     * @return a {@link URIBuilder} instance.  Note that the return type is Object
     * simply so that it matches with its JavaBean {@link #setUri(Object)}
     * counterpart.
     */
    public Object getUri() {
        return defaultURI;
    }

    /**
     * Set the default headers to add to all requests made by this builder
     * instance.  These values will replace any previously set default headers.
     *
     * @param headers map of header names & values.
     */
    public void setHeaders(Map<?, ?> headers) {
        this.defaultRequestHeaders.clear();
        if (headers == null) return;
        for (Object key : headers.keySet()) {
            Object val = headers.get(key);
            if (val == null) continue;
            this.defaultRequestHeaders.put(key.toString(), val.toString());
        }
    }

    /**
     * Get the map of default headers that will be added to all requests.
     * This is a 'live' collection so it may be used to add or remove default
     * values.
     *
     * @return the map of default header names and values.
     */
    public Map<?, ?> getHeaders() {
        return this.defaultRequestHeaders;
    }

    /**
     * Return the underlying HTTPClient that is used to handle HTTP requests.
     *
     * @return the client instance.
     */
    public AbstractHttpClient getClient() {
        return this.client;
    }

    /**
     * Set a custom registry used to handle different
     * <code>content-encoding</code> types in responses.
     *
     * @param cer
     */
    public void setContentEncodingRegistry(ContentEncodingRegistry cer) {
        this.contentEncodingHandler = cer;
    }

    /**
     * Set the default HTTP proxy to be used for all requests.
     *
     * @param host   host name or IP
     * @param port   port, or -1 for the default port
     * @param scheme usually "http" or "https," or <code>null</code> for the default
     * @see HttpHost#HttpHost(String, int, String)
     */
    public void setProxy(String host, int port, String scheme) {
        getClient().getParams().setParameter(
                ConnRoutePNames.DEFAULT_PROXY,
                new HttpHost(host, port, scheme));
    }

    /**
     * Release any system resources held by this instance.
     *
     * @see ClientConnectionManager#shutdown()
     */
    public void shutdown() {
        client.getConnectionManager().shutdown();
    }


    /**
     * <p>Encloses all properties and method calls used within the
     * {@link HTTPBuilder#request(Object, HttpRequestFactory, Object, Closure)} 'config'
     * closure argument.  That is, an instance of this class is set as the
     * closure's delegate.  This allows the user to configure various parameters
     * within the scope of a single request.  </p>
     * <p>
     * <p>All properties of this class are available from within the closure.
     * For example, you can manipulate various aspects of the
     * {@link HTTPBuilder#setUri(Object) default request URI} for this request
     * by calling <code>uri.path = '/api/location'</code>.  This allows for the
     * ability to modify parameters per-request while leaving any values set
     * directly on the HTTPBuilder instance unchanged for subsequent requests.
     * </p>
     */
    protected class RequestConfigDelegate {
        private HttpRequestBase request;
        private Object contentType;
        private String requestContentType;
        private Map<Object, Closure> responseHandlers = new StringHashMap<Closure>();
        private URIBuilder uri;
        private Map<Object, Object> headers = new StringHashMap<Object>();
        private HttpContextDecorator context = new HttpContextDecorator();

        public RequestConfigDelegate(HttpRequestBase request, Object contentType,
                                     Map<?, ?> defaultRequestHeaders,
                                     Map<?, Closure> defaultResponseHandlers) {
            if (request == null) throw new IllegalArgumentException(
                    "Internal error - HttpRequest instance cannot be null");
            this.request = request;
            this.headers.putAll(defaultRequestHeaders);
            this.contentType = contentType;
            if (defaultRequestContentType != null)
                this.requestContentType = defaultRequestContentType.toString();
            this.responseHandlers.putAll(defaultResponseHandlers);
            URI uri = request.getURI();
            if (uri != null) this.uri = new URIBuilder(uri, urlEncodingEnabled, encoderConfig);
        }

        public RequestConfigDelegate(Map<String, ?> args, HttpRequestBase request, Closure successHandler)
                throws URISyntaxException {
            this(request, defaultContentType, defaultRequestHeaders, defaultResponseHandlers);
            if (successHandler != null)
                this.responseHandlers.put(Status.SUCCESS.toString(), successHandler);
            setPropertiesFromMap(args);
        }

        /**
         * Use this object to manipulate parts of the request URI, like
         * query params and request path.  Example:
         * <pre>
         * builder.request(GET,XML) {
         *   uri.path = '../other/request.jsp'
         *   uri.query = [p1:1, p2:2]
         *   ...
         * }</pre>
         * <p>
         * <p>This method signature returns <code>Object</code> so that the
         * complementary {@link #setUri(Object)} method can accept various
         * types. </p>
         *
         * @return {@link URIBuilder} to manipulate the request URI
         */
        public URIBuilder getUri() {
            return this.uri;
        }

        /**
         * <p>Set the entire URI to be used for this request.  Acceptable
         * parameter types are:
         * <ul>
         * <li><code>URL</code></li>
         * <li><code>URI</code></li>
         * <li><code>URIBuilder</code></li>
         * </ul>
         * Any other parameter type will be assumed that its
         * <code>toString()</code> method produces a valid URI.</p>
         * <p>
         * <p>Note that if you want to change just a portion of the request URI,
         * (e.g. the host, port, path, etc.) you can call {@link #getUri()}
         * which will return a {@link URIBuilder} which can manipulate portions
         * of the request URI.</p>
         *
         * @param uri the URI to use for this request.
         * @throws URISyntaxException if an argument is given that is not a valid URI
         * @see URIBuilder#convertToURI(Object)
         */
        public void setUri(Object uri) throws URISyntaxException {
            if (uri instanceof URIBuilder) this.uri = (URIBuilder) uri;
            this.uri = new URIBuilder(URIBuilder.convertToURI(uri), urlEncodingEnabled, encoderConfig);
        }

        /**
         * Directly access the Apache HttpClient instance that will
         * be used to execute this request.
         *
         * @see HttpRequestBase
         */
        protected HttpRequestBase getRequest() {
            return this.request;
        }

        /**
         * Get the content-type of any data sent in the request body and the
         * expected response content-type.  If the request content-type is
         * expected to differ from the response content-type (i.e. a URL-encoded
         * POST that should return an HTML page) then this value will be used
         * for the <i>response</i> content-type, while
         * {@link #setRequestContentType(String)} should be used for the request.
         *
         * @return whatever value was assigned via {@link #setContentType(Object)}
         * or passed from the {@link HTTPBuilder#defaultContentType} when this
         * RequestConfigDelegate instance was constructed.
         */
        protected Object getContentType() {
            return this.contentType;
        }

        /**
         * Set the content-type used for any data in the request body, as well
         * as the <code>Accept</code> content-type that will be used for parsing
         * the response. The value should be either a {@link ContentType} value
         * or a String, i.e. <code>"text/plain"</code>.  This will default to
         * {@link HTTPBuilder#getContentType()} for requests that do not
         * explicitly pass a <code>contentType</code> parameter (such as
         * {@link HTTPBuilder#request(HttpRequestFactory, Object, Closure)}).
         *
         * @param ct the value that will be used for the <code>Content-Type</code>
         *           and <code>Accept</code> request headers.
         */
        protected void setContentType(Object ct) {
            if (ct == null) this.contentType = defaultContentType;
            else this.contentType = ct;
        }

        /**
         * The request content-type, if different from the {@link #contentType}.
         *
         * @return
         */
        protected String getRequestContentType() {
            if (this.requestContentType != null) return this.requestContentType;
            else return this.getContentType().toString();
        }

        /**
         * <p>Assign a different content-type for the request than is expected for
         * the response.  This is useful if i.e. you want to post URL-encoded
         * form data but expect the response to be XML or HTML.  The
         * {@link #getContentType()} will always control the <code>Accept</code>
         * header, and will be used for the request content <i>unless</i> this
         * value is also explicitly set.</p>
         * <p>Note that this method is used internally; calls within a request
         * configuration closure should call {@link #send(Object, Object)}
         * to set the request body and content-type at the same time.</p>
         *
         * @param ct either a {@link ContentType} value or a valid content-type
         *           String.
         */
        protected void setRequestContentType(String ct) {
            this.requestContentType = ct;
        }

        /**
         * Valid arguments:
         * <dl>
         * <dt>uri</dt><dd>Either a URI, URL, or object whose
         * <code>toString()</code> method produces a valid URI string.
         * If this parameter is not supplied, the HTTPBuilder's default
         * URI is used.</dd>
         * <dt>path</dt><dd>Request path that is merged with the URI</dd>
         * <dt>query</dt><dd>Map of URL query parameters</dd>
         * <dt>headers</dt><dd>Map of HTTP headers</dd>
         * <dt>contentType</dt><dd>Request content type and Accept header.
         * If not supplied, the HTTPBuilder's default content-type is used.</dd>
         * <dt>requestContentType</dt><dd>content type for the request, if it
         * is different from the expected response content-type</dd>
         * <dt>body</dt><dd>Request body that will be encoded based on the given contentType</dd>
         * </dl>
         *
         * @param args named parameters to set properties on this delegate.
         * @throws URISyntaxException if the uri argument does not represent a valid URI
         */
        @SuppressWarnings("unchecked")
        protected void setPropertiesFromMap(Map<String, ?> args) throws URISyntaxException {
            if (args == null) return;
            if (args.get("url") != null) throw new IllegalArgumentException(
                    "The 'url' parameter is deprecated; use 'uri' instead");
            Object uri = args.get("uri");
            if (uri == null) uri = defaultURI;
            if (uri == null) throw new IllegalStateException(
                    "Default URI is null, and no 'uri' parameter was given");
            this.uri = new URIBuilder(URIBuilder.convertToURI(uri), urlEncodingEnabled, encoderConfig);

            Map query = (Map) args.get("params");
            if (query != null) {
                log.debug("'params' argument is deprecated; use 'query' instead.");
                this.uri.setQuery(query);
            }
            query = (Map) args.get("query");
            if (query != null) this.uri.setQuery(query);
            Map headers = (Map) args.get("headers");
            if (headers != null) this.getHeaders().putAll(headers);

            Object path = args.get("path");
            if (path != null) this.uri.setPath(path.toString());

            Object contentType = args.get("contentType");
            if (contentType != null) this.setContentType(contentType);

            contentType = args.get("requestContentType");
            if (contentType != null) this.setRequestContentType(contentType.toString());

            Object body = args.get("body");
            if (body != null) this.setBody(this.getRequestContentType(), body);
        }

        /**
         * Set request headers.  These values will be <strong>merged</strong>
         * with any {@link HTTPBuilder#getHeaders() default request headers.}
         * (The assumption is you'll probably want to add a bunch of headers to
         * whatever defaults you've already set).  If you <i>only</i> want to
         * use values set here, simply call {@link #getHeaders() headers.clear()}
         * first.
         */
        public void setHeaders(Map<?, ?> newHeaders) {
            this.headers.putAll(newHeaders);
        }

        /**
         * <p>Get request headers (including any default headers set on this
         * {@link HTTPBuilder#setHeaders(Map) HTTPBuilder instance}).  Note that
         * this will not include any <code>Accept</code>, <code>Content-Type</code>,
         * or <code>Content-Encoding</code> headers that are automatically
         * handled by any encoder or parsers in effect.  Note that any values
         * set here <i>will</i> override any of those automatically assigned
         * values.</p>
         * <p>
         * <p>Example: <code>headers.'Accept-Language' = 'en, en-gb;q=0.8'</code></p>
         *
         * @return a map of HTTP headers that will be sent in the request.
         */
        public Map<?, ?> getHeaders() {
            return this.headers;
        }

        /**
         * Convenience method to set a request content-type at the same time
         * the request body is set.  This is a variation of
         * {@link #setBody(Object, Object)} that allows for a different content-type
         * than what is expected for the response.
         * <p>
         * <p>Example:
         * <pre>
         * http.request(POST,HTML) {
         *
         *   /* request data is interpreted as a JsonBuilder closure by
         *      HTTPBuilder's default EncoderRegistry implementation * /
         *   send( 'text/javascript' ) {
         *     a : ['one','two','three']
         *   }
         *
         *   // response content-type is what was specified in the outer request() argument:
         *   response.success = { resp, html ->
         *
         *   }
         * }
         * </pre>
         * The <code>send</code> call is equivalent to the following:
         * <pre>
         *   requestContentType = 'text/javascript'
         *   body = { a : ['one','two','three'] }
         * </pre>
         *
         * @param contentType either a {@link ContentType} or equivalent
         *                    content-type string like <code>"text/xml"</code>
         * @param requestBody
         */
        public void send(Object contentType, Object requestBody) {
            this.setRequestContentType(contentType.toString());
            this.setBody(requestBody);
        }

        private void setBody(Object requestBody) {
            setBody(getRequestContentType(), requestBody);
        }

        /**
         * Set the request body.  This value may be of any type supported by
         * the associated {@link EncoderRegistry request encoder}.  That is,
         * the value of <code>body</code> will be interpreted by the encoder
         * associated with the current {@link #getRequestContentType() request
         * content-type}.
         *
         * @param body data or closure interpreted as the request body
         * @see #send(Object, Object)
         */
        public void setBody(Object requestContentType, Object body) {
            if (!(request instanceof HttpEntityEnclosingRequest))
                throw new IllegalArgumentException(
                        "Cannot set a request body for a " + request.getMethod() + " method");
            Closure encoder = encoders.getAt(requestContentType);
            HttpEntity entity = (HttpEntity) encoder.call(requestContentType, body);

            ((HttpEntityEnclosingRequest) this.request).setEntity(entity);
        }

        /**
         * Get the proper response handler for the response code.  This is called
         * by the {@link HTTPBuilder} class in order to find the proper handler
         * based on the response status code.
         *
         * @param statusCode HTTP response status code
         * @return the response handler
         */
        protected Closure findResponseHandler(int statusCode) {
            Closure handler = this.getResponse().get(Integer.toString(statusCode));
            if (handler == null) handler =
                    this.getResponse().get(Status.find(statusCode).toString());
            return handler;
        }

        /**
         * Access the response handler map to set response parsing logic.
         * i.e.<pre>
         * builder.request( GET, XML ) {
         *   response.success = { xml ->
         *      /* for XML content type, the default parser
         *         will return an XmlSlurper * /
         *   	xml.root.children().each { println it }
         *   }
         * }</pre>
         *
         * @return
         */
        public Map<Object, Closure> getResponse() {
            return this.responseHandlers;
        }

        /**
         * Get the {@link HttpContext} that will be used for this request.  By
         * default, a new context is created for each request.
         *
         * @return
         * @see ClientContext
         */
        public HttpContextDecorator getContext() {
            return this.context;
        }

        /**
         * Set the {@link HttpContext} that will be used for this request.
         *
         * @param ctx
         */
        public void setContext(HttpContext ctx) {
            this.context = new HttpContextDecorator(ctx);
        }
    }
}