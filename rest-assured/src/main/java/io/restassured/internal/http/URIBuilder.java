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

import io.restassured.config.EncoderConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;

import static io.restassured.config.EncoderConfig.encoderConfig;

/**
 * This class implements a mutable URI.  All <code>set</code>, <code>add</code>
 * and <code>remove</code> methods affect this class' internal URI
 * representation.  All mutator methods support chaining, e.g.
 * <pre>
 * new URIBuilder("http://www.google.com/")
 *   .setScheme( "https" )
 *   .setPort( 443 )
 *   .setPath( "some/path" )
 *   .toString();
 * </pre>
 * A slightly more 'Groovy' version would be:
 * <pre>
 * new URIBuilder('http://www.google.com/').with {
 *    scheme = 'https'
 *    port = 443
 *    path = 'some/path'
 *    query = [p1:1, p2:'two']
 * }.toString()
 * </pre>
 *
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 * @author Johan Haleby
 */
public class URIBuilder implements Cloneable {
    private static final String PARAMETER_SEPARATOR = "&";
    private static final String NAME_VALUE_SEPARATOR = "=";
    private static final String PLUS = "+";
    private static final String PERCENTAGE_20 = "%20";

    protected URI base;
    private String enc;
    private final boolean isUrlEncodingEnabled;

    /**
     * @param uri
     * @throws IllegalArgumentException if uri is null
     */
    public URIBuilder(URI uri, boolean urlEncodingEnabled, EncoderConfig config) throws IllegalArgumentException {
        Validate.notNull(uri, "uri cannot be null");
        Validate.notNull(config, "encoder config cannot be null");
        this.base = uri;
        this.enc = config.defaultQueryParameterCharset();
        this.isUrlEncodingEnabled = urlEncodingEnabled;
    }

    /**
     * Utility method to convert a number of type to a URI instance.
     *
     * @param uri a {@link URI}, {@link URL} or any object that produces a
     *            valid URI string from its <code>toString()</code> result.
     * @return a valid URI parsed from the given object
     * @throws URISyntaxException
     */
    public static URI convertToURI(Object uri) throws URISyntaxException {
        if (uri instanceof URI) return (URI) uri;
        if (uri instanceof URL) return ((URL) uri).toURI();
        if (uri instanceof URIBuilder) return ((URIBuilder) uri).toURI();
        return new URI(uri.toString()); // assume any other object type produces a valid URI string
    }


    /**
     * Set the URI scheme, AKA the 'protocol.'  e.g.
     * <code>setScheme('https')</code>
     *
     * @throws URISyntaxException if the given scheme contains illegal characters.
     */
    public URIBuilder setScheme(String scheme) throws URISyntaxException {
        this.base = new URI(scheme, base.getUserInfo(),
                base.getHost(), base.getPort(), base.getPath(),
                base.getQuery(), base.getFragment());
        return this;
    }

    public URIBuilder setPort(int port) throws URISyntaxException {
        this.base = new URI(base.getScheme(), base.getUserInfo(),
                base.getHost(), port, base.getPath(),
                base.getQuery(), base.getFragment());
        return this;
    }

    public URIBuilder setHost(String host) throws URISyntaxException {
        this.base = new URI(base.getScheme(), base.getUserInfo(),
                host, base.getPort(), base.getPath(),
                base.getQuery(), base.getFragment());
        return this;
    }

    /**
     * Set the path component of this URI.  The value may be absolute or
     * relative to the current path.
     * e.g. <pre>
     *   def uri = new URIBuilder( 'http://localhost/p1/p2?a=1' )
     * <p/>
     *   uri.path = '/p3/p2'
     *   assert uri.toString() == 'http://localhost/p3/p2?a=1'
     * <p/>
     *   uri.path = 'p2a'
     *   assert uri.toString() == 'http://localhost/p3/p2a?a=1'
     * <p/>
     *   uri.path = '../p4'
     *   assert uri.toString() == 'http://localhost/p4?a=1&b=2&c=3#frag'
     * <pre>
     *
     * @param path the path portion of this URI, relative to the current URI.
     * @return this URIBuilder instance, for method chaining.
     * @throws URISyntaxException if the given path contains characters that
     *                            cannot be converted to a valid URI
     */
    public URIBuilder setPath(String path) throws URISyntaxException {
        /* Passing the path string in the URI constructor will
                     * double-escape path parameters and goober things up.  So we have
                     * to create a full path+query+fragment and use URI#resolve() to
                     * create the new URI. */
        StringBuilder sb = new StringBuilder();
        if (path != null) sb.append(path);
        String query = base.getQuery();
        if (query != null) {
            sb.append('?');
            sb.append(query);
        }
        String frag = base.getRawFragment();
        if (frag != null) sb.append('#').append(frag);
        this.base = base.resolve(sb.toString());
        return this;
    }

    /* TODO null/ zero-size check if this is ever made public */
    protected URIBuilder setQueryNVP(List<NameValuePair> nvp) throws URISyntaxException {
        /* Passing the query string in the URI constructor will
           * double-escape query parameters and goober things up.  So we have
           * to create a full path+query+fragment and use URI#resolve() to
           * create the new URI.  */
        StringBuilder sb = new StringBuilder();
        String path = base.getRawPath();
        if (path != null) sb.append(path);
        sb.append('?');
        sb.append(format(nvp, isUrlEncodingEnabled, enc));
        String frag = base.getRawFragment();
        if (frag != null) sb.append('#').append(frag);
        this.base = base.resolve(sb.toString());

        return this;
    }

    /**
     * Set the query portion of the URI.  For query parameters with multiple
     * values, put the values in a list like so:
     * <pre>uri.query = [ p1:'val1', p2:['val2', 'val3'] ]
     * // will produce a query string of ?p1=val1&p2=val2&p2=val3</pre>
     *
     * @param params a Map of parameters that will be transformed into the query string
     * @return this URIBuilder instance, for method chaining.
     * @throws URISyntaxException
     */
    public URIBuilder setQuery(Map<?, ?> params) throws URISyntaxException {
        if (params != null && params.size() >= 1) {
            List<NameValuePair> nvp = new ArrayList<NameValuePair>(params.size());
            for (Object key : params.keySet()) {
                Object value = params.get(key);
                if (value instanceof List) {
                    for (Object val : (List) value)
                        nvp.add(new BasicNameValuePairWithNoValueSupport(key.toString(), val));
                } else nvp.add(new BasicNameValuePairWithNoValueSupport(key.toString(), value));
            }
            this.setQueryNVP(nvp);
        }
        return this;
    }

    /**
     * Get the query string as a map for convenience.  If any parameter contains
     * multiple values (e.g. <code>p1=one&p1=two</code>) both values will be
     * inserted into a list for that parameter key (<code>[p1 : ['one','two']]
     * </code>).  Note that this is not a "live" map.  Therefore, you cannot
     * call
     * <pre> uri.query.a = 'BCD'</pre>
     * You will not modify the query string but instead the generated map of
     * parameters.  Instead, you need to use {@link #removeQueryParam(String)}
     * first, then call {@link #setQuery(Map)} which will set the entire query string.
     *
     * @return a map of String name/value pairs representing the URI's query
     *         string.
     */
    public Map<String, Object> getQuery() {
        Map<String, Object> params = new TreeMap<String, Object>();
        List<NameValuePair> pairs = this.getQueryNVP();

        for (NameValuePair pair : pairs) {

            String key = pair.getName();
            Object existing = params.get(key);

            if (existing == null) params.put(key, pair.getValue());

            else if (existing instanceof List)
                ((List) existing).add(pair.getValue());

            else {
                List<String> vals = new ArrayList<String>(2);
                vals.add((String) existing);
                vals.add(pair.getValue());
                params.put(key, vals);
            }
        }

        return params;
    }

    protected List<NameValuePair> getQueryNVP() {
        List<NameValuePair> nvps = parse(this.base);
        List<NameValuePair> newList = new ArrayList<NameValuePair>();
        if (nvps != null) newList.addAll(nvps);
        return newList;
    }

    /**
     * Indicates if the given parameter is already part of this URI's query
     * string.
     *
     * @param name the query parameter name
     * @return true if the given parameter name is found in the query string of
     *         the URI.
     */
    public boolean hasQueryParam(String name) {
        return getQuery().get(name) != null;
    }

    /**
     * Remove the given query parameter from this URI's query string.
     *
     * @param param the query name to remove
     * @return this URIBuilder instance, for method chaining.
     * @throws URISyntaxException
     */
    public URIBuilder removeQueryParam(String param) throws URISyntaxException {
        List<NameValuePair> params = getQueryNVP();
        NameValuePair found = null;
        for (NameValuePair nvp : params)  // BOO linear search.  Assume the list is small.
            if (nvp.getName().equals(param)) {
                found = nvp;
                break;
            }

        if (found == null) throw new IllegalArgumentException("Param '" + param + "' not found");
        params.remove(found);
        this.setQueryNVP(params);
        return this;
    }

    protected URIBuilder addQueryParams(List<NameValuePair> nvp) throws URISyntaxException {
        List<NameValuePair> params = getQueryNVP();
        params.addAll(nvp);
        this.setQueryNVP(params);
        return this;
    }

    /**
     * Add these parameters to the URIBuilder's existing query string.
     * Parameters may be passed either as a single map argument, or as a list
     * of named arguments.  e.g.
     * <pre> uriBuilder.addQueryParams( [one:1,two:2] )
     * uriBuilder.addQueryParams( three : 3 ) </pre>
     * <p/>
     * If any of the parameters already exist in the URI query, these values
     * will <strong>not</strong> replace them.  Multiple values for the same
     * query parameter may be added by putting them in a list. See
     * {@link #setQuery(Map)}.
     *
     * @param params parameters to add to the existing URI query (if any).
     * @return this URIBuilder instance, for method chaining.
     * @throws URISyntaxException
     */
    @SuppressWarnings("unchecked")
    public URIBuilder addQueryParams(Map<?, ?> params) throws URISyntaxException {
        List<NameValuePair> nvp = new ArrayList<NameValuePair>();
        for (Object key : params.keySet()) {
            Object value = params.get(key);
            if (value instanceof List) {
                for (Object val : (List) value)
                    nvp.add(new BasicNameValuePairWithNoValueSupport(key.toString(), val));
            } else nvp.add(new BasicNameValuePairWithNoValueSupport(key.toString(), value));
        }
        this.addQueryParams(nvp);
        return this;
    }

    /**
     * The document fragment, without a preceeding '#'
     *
     * @param fragment
     * @return this URIBuilder instance, for method chaining.
     * @throws URISyntaxException if the given value contains illegal characters.
     */
    public URIBuilder setFragment(String fragment) throws URISyntaxException {
        this.base = new URI(base.getScheme(), base.getUserInfo(),
                base.getHost(), base.getPort(), base.getPath(),
                base.getQuery(), fragment);
        return this;
    }

    /**
     * Print this builder's URI representation.
     */
    @Override
    public String toString() {
        return base.toString();
    }

    /**
     * Convenience method to convert this object to a URL instance.
     *
     * @return this builder as a URL
     * @throws MalformedURLException if the underlying URI does not represent a
     *                               valid URL.
     */
    public URL toURL() throws MalformedURLException {
        return base.toURL();
    }

    /**
     * Convenience method to convert this object to a URI instance.
     *
     * @return this builder's underlying URI representation
     */
    public URI toURI() {
        return this.base;
    }

    /**
     * Implementation of Groovy's <code>as</code> operator, to allow type
     * conversion.
     *
     * @param type <code>URL</code>, <code>URL</code>, or <code>String</code>.
     * @return a representation of this URIBuilder instance in the given type
     * @throws MalformedURLException if <code>type</code> is URL and this
     *                               URIBuilder instance does not represent a valid URL.
     */
    public Object asType(Class<?> type) throws MalformedURLException {
        if (type == URI.class) return this.toURI();
        if (type == URL.class) return this.toURL();
        if (type == String.class) return this.toString();
        throw new ClassCastException("Cannot cast instance of URIBuilder to class " + type);
    }

    /**
     * Create a copy of this URIBuilder instance.
     */
    @Override
    protected URIBuilder clone() {
        return new URIBuilder(this.base, this.isUrlEncodingEnabled, encoderConfig().defaultQueryParameterCharset(this.enc));
    }

    /**
     * Determine if this URIBuilder is equal to another URIBuilder instance.
     *
     * @return if <code>obj</code> is a URIBuilder instance whose underlying
     *         URI implementation is equal to this one's.
     * @see URI#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof URIBuilder)) return false;
        return this.base.equals(((URIBuilder) obj).toURI());
    }

    /**
     * Returns a String that is suitable for use as an <code>application/x-www-form-urlencoded</code>
     * list of parameters in an HTTP PUT or HTTP POST.
     * <p>
     * This is a copy of {@link URLEncodedUtils#format(java.util.List, String)} that also handles {@link BasicNameValuePairWithNoValueSupport}.
     * </p>
     *
     * @param parameters The parameters to include.
     * @param encoding   The encoding to use.
     */
    private static String format(
            final List<? extends NameValuePair> parameters,
            final boolean isUrlEncodingEnabled,
            final String encoding) {
        final StringBuilder result = new StringBuilder();
        for (final NameValuePair parameter : parameters) {
            if (result.length() > 0)
                result.append(PARAMETER_SEPARATOR);
            final String encodedName = isUrlEncodingEnabled ? encode(parameter.getName(), encoding) : parameter.getName();
            result.append(encodedName);
            if (hasValue(parameter)) {
                final String value = parameter.getValue();
                final String encodedValue = value != null ? isUrlEncodingEnabled ? encode(value, encoding) : value : "";
                result.append(NAME_VALUE_SEPARATOR);
                result.append(encodedValue);
            }
        }
        return result.toString();
    }

    private static boolean hasValue(NameValuePair parameter) {
        if (!(parameter instanceof BasicNameValuePairWithNoValueSupport)) {
            return true;
        }

        return ((BasicNameValuePairWithNoValueSupport) parameter).hasValue();
    }

    // Copy of  the private method in URLEncodedUtils
    public static String encode(final String content, final String encoding) {
        try {
            String encoded = URLEncoder.encode(content, encoding != null ? encoding : Charset.defaultCharset().toString());
            // We replace spaces encoded as "+" to %20 because some server (such as Scalatra) doesn't decode "+" correctly.
            encoded = StringUtils.replace(encoded, PLUS, PERCENTAGE_20);
            return encoded;
        } catch (UnsupportedEncodingException problem) {
            throw new IllegalArgumentException(problem);
        }
    }

    /**
     * Adds all parameters within the Scanner to the list of
     * <code>parameters</code>, as encoded by <code>encoding</code>. For
     * example, a scanner containing the string <code>a=1&b=2&c=3</code> would
     * add the {@link NameValuePair NameValuePairs} a=1, b=2, and c=3 to the
     * list of parameters.
     * <p>
     * Note that this method has been copied from {@link URLEncodedUtils#parse(java.util.List, java.util.Scanner, String)} but it doesn't do URL decoding.
     * </p>
     */
    private List<NameValuePair> parse(URI uri) {
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        final String query = uri.getRawQuery();
        if (query != null && query.length() > 0) {
            final Scanner scanner = new Scanner(query);
            scanner.useDelimiter(PARAMETER_SEPARATOR);
            while (scanner.hasNext()) {
                String name;
                String value = null;
                String token = scanner.next();
                int i = token.indexOf(NAME_VALUE_SEPARATOR);
                if (i != -1) {
                    name = token.substring(0, i).trim();
                    value = token.substring(i + 1).trim();
                } else {
                    name = token.trim();
                }
                parameters.add(new BasicNameValuePair(name, value));
            }
        }
        return parameters;
    }
}
