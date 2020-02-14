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

import groovy.json.JsonBuilder;
import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.Writable;
import groovy.xml.StreamingMarkupBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import io.restassured.internal.util.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.codehaus.groovy.runtime.MethodClosure;

import java.io.*;
import java.util.*;

import static io.restassured.internal.support.FileReader.readToString;


/**
 * <p>This class handles creation of the request body (i.e. for a
 * PUT or POST operation) based on content-type.   When a
 * {@link HTTPBuilder.RequestConfigDelegate#setBody(Object) body} is set from the builder, it is
 * processed based on the {@link HTTPBuilder.RequestConfigDelegate#getRequestContentType()
 * request content-type}.  For instance, the {@link #encodeForm(Map)} method
 * will be invoked if the request content-type is form-urlencoded, which will
 * cause the following:<code>body=[a:1, b:'two']</code> to be encoded as
 * the equivalent <code>a=1&b=two</code> in the request body.</p>
 * <p/>
 * <p>Most default encoders can handle a closure as a request body.  In this
 * case, the closure is executed and a suitable 'builder' passed to the
 * closure that is  used for constructing the content.  In the case of
 * binary encoding this would be an OutputStream; for TEXT encoding it would
 * be a PrintWriter, and for XML it would be an already-bound
 * {@link StreamingMarkupBuilder}. See each <code>encode...</code> method
 * for details for each particular content-type.</p>
 * <p/>
 * <p>Contrary to its name, this class does not have anything to do with the
 * <code>content-encoding</code> HTTP header.  </p>
 *
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 */
public class EncoderRegistry {

    private Map<String, Closure> registeredEncoders = buildDefaultEncoderMap();
    private EncoderConfig encoderConfig = new EncoderConfig();

    /**
     * Set the encoder config
     */
    public void setEncoderConfig(EncoderConfig encoderConfig) {
        this.encoderConfig = encoderConfig;
    }

    /**
     * Default request encoder for a binary stream.  Acceptable argument
     * types are:
     * <ul>
     * <li>InputStream</li>
     * <li>byte[] / ByteArrayOutputStream</li>
     * <li>Closure</li>
     * </ul>
     * If a closure is given, it is executed with an OutputStream passed
     * as the single closure argument.  Any data sent to the stream from the
     * body of the closure is used as the request content body.
     *
     * @param data
     * @return an {@link HttpEntity} encapsulating this request data
     * @throws UnsupportedEncodingException
     */
    public InputStreamEntity encodeStream(Object contentType, Object data) throws UnsupportedEncodingException {
        InputStreamEntity entity = null;

        if (data instanceof ByteArrayInputStream) {
            // special case for ByteArrayIS so that we can set the content length.
            ByteArrayInputStream in = ((ByteArrayInputStream) data);
            entity = new InputStreamEntity(in, in.available());
        } else if (data instanceof InputStream) {
            entity = new InputStreamEntity((InputStream) data, -1);
        } else if (data instanceof File) {
            FileInputStream fileInputStream;
            File file = (File) data;
            try {
                fileInputStream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("File " + file.getPath() + " not found", e);
            }
            // Set file size, since we already know it at this time. Ref: Issue #988
            entity = new InputStreamEntity(fileInputStream, file.length());
        } else if (data instanceof byte[]) {
            byte[] out = ((byte[]) data);
            entity = new InputStreamEntity(new ByteArrayInputStream(
                    out), out.length);
        } else if (data instanceof ByteArrayOutputStream) {
            ByteArrayOutputStream out = ((ByteArrayOutputStream) data);
            entity = new InputStreamEntity(new ByteArrayInputStream(
                    out.toByteArray()), out.size());
        } else if (data instanceof Closure) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ((Closure) data).call(out); // data is written to out
            entity = new InputStreamEntity(new ByteArrayInputStream(
                    out.toByteArray()), out.size());
        }

        if (entity == null) throw new IllegalArgumentException(
                "Don't know how to encode " + data + " as a byte stream.\n\nPlease use EncoderConfig (EncoderConfig#encodeContentTypeAs) to specify how to serialize data for this content-type.\n" +
                        "For example: \"given().config(RestAssured.config().encoderConfig(encoderConfig().encodeContentTypeAs(\"" + ContentTypeExtractor.getContentTypeWithoutCharset(contentTypeToString(contentType)) + "\", ContentType.TEXT))). ..\"");

        entity.setContentType(contentTypeToString(contentType));
        return entity;
    }

    /**
     * Default handler used for a plain text content-type.  Acceptable argument
     * types are:
     * <ul>
     * <li>Closure</li>
     * <li>Writable</li>
     * <li>Reader</li>
     * </ul>
     * For Closure argument, a {@link PrintWriter} is passed as the single
     * argument to the closure.  Any data sent to the writer from the
     * closure will be sent to the request content body.
     *
     * @param data
     * @return an {@link HttpEntity} encapsulating this request data
     * @throws IOException
     */
    public HttpEntity encodeText(Object contentType, Object data) throws IOException {
        String contentTypeAsString = contentTypeToString(contentType);
        if (data instanceof Closure) {
            StringWriter out = new StringWriter();
            PrintWriter writer = new PrintWriter(out);
            ((Closure) data).call(writer);
            writer.close();
            out.flush();
            data = out;
        } else if (data instanceof Writable) {
            StringWriter out = new StringWriter();
            ((Writable) data).writeTo(out);
            out.flush();
            data = out;
        } else if (data instanceof Reader && !(data instanceof BufferedReader)) {
            data = new BufferedReader((Reader) data);
        } else if (data instanceof File) {
            data = toString((File) data, contentTypeAsString);
        }
        if (data instanceof BufferedReader) {
            StringWriter out = new StringWriter();
            IOGroovyMethods.leftShift(out, (BufferedReader) data);

            data = out;
        }
        // if data is a String, we are already covered.
        return createEntity(contentTypeAsString, data);
    }

    /**
     * Set the request body as a url-encoded list of parameters.  This is
     * typically used to simulate a HTTP form POST.
     * For multi-valued parameters, enclose the values in a list, e.g.
     * <pre>[ key1 : ['val1', 'val2'], key2 : 'etc.' ]</pre>
     *
     * @param params
     * @return an {@link HttpEntity} encapsulating this request data
     * @throws UnsupportedEncodingException
     */
    public UrlEncodedFormEntity encodeForm(Map<?, ?> params)
            throws UnsupportedEncodingException {
        List<NameValuePair> paramList = new ArrayList<NameValuePair>();

        for (Object key : params.keySet()) {
            Object val = params.get(key);
            if (val instanceof List)
                for (Object subVal : (List) val)
                    paramList.add(new BasicNameValuePair(key.toString(),
                            (subVal == null) ? "" : subVal.toString()));

            else paramList.add(new BasicNameValuePair(key.toString(),
                    (val == null) ? "" : val.toString()));
        }

        return new UrlEncodedFormEntity(paramList, encoderConfig.defaultContentCharset());
    }

    /**
     * Accepts a String as a url-encoded form post.  This method assumes the
     * String is an already-encoded POST string.
     *
     * @param formData a url-encoded form POST string.  See
     *                 <a href='http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.1'>
     *                 The W3C spec</a> for more info.
     * @return an {@link HttpEntity} encapsulating this request data
     * @throws UnsupportedEncodingException
     */
    public HttpEntity encodeForm(Object contentType, String formData) throws UnsupportedEncodingException {
        return this.createEntity(contentTypeToString(contentType), formData);
    }

    /**
     * Encode the content as XML.  The argument may be either an object whose
     * <code>toString</code> produces valid markup, or a Closure which will be
     * interpreted as a builder definition.
     *
     * @param xml data that defines the XML structure
     * @return an {@link HttpEntity} encapsulating this request data
     * @throws UnsupportedEncodingException
     */
    public HttpEntity encodeXML(Object contentType, Object xml) throws UnsupportedEncodingException {
        String contentTypeAsString = contentTypeToString(contentType);
        if (xml instanceof Closure) {
            StreamingMarkupBuilder smb = new StreamingMarkupBuilder();
            xml = smb.bind(xml);
        } else if (xml instanceof File) {
            xml = toString((File) xml, contentTypeAsString);
        }
        return createEntity(contentTypeAsString, xml);
    }

    /**
     * <p>Accepts a Collection or a JavaBean object which is converted to JSON.
     * A Map or Collection will be converted to a {@link JsonBuilder}..  A
     * String or GString will be interpreted as valid JSON and passed directly
     * as the request body (with charset conversion if necessary.)</p>
     * <p/>
     * <p>If a Closure is passed as the model, it will be executed as if it were
     * a JSON object definition passed to a {@link JsonBuilder}.  In order
     * for the closure to be interpreted correctly, there must be a 'root'
     * element immediately inside the closure.  For example:</p>
     * <p/>
     * <pre>builder.post( JSON ) {
     *   body = {
     *     root {
     *       first {
     *         one = 1
     *         two = '2'
     *       }
     *       second = 'some string'
     *     }
     *   }
     * }</pre>
     * <p> will return the following JSON string:<pre>
     * {"root":{"first":{"one":1,"two":"2"},"second":"some string"}}</pre></p>
     *
     * @param model data to be converted to JSON, as specified above.
     * @return an {@link HttpEntity} encapsulating this request data
     * @throws UnsupportedEncodingException
     */
    @SuppressWarnings("unchecked")
    public HttpEntity encodeJSON(Object contentType, Object model) throws IOException {
        String contentTypeAsString = contentTypeToString(contentType);
        Object json;
        if (model instanceof Map || model instanceof Collection) {
            json = new JsonBuilder(model);
        } else if (model instanceof Closure) {
            Closure closure = (Closure) model;
            closure.setDelegate(new JsonBuilder());
            json = closure.call();
        } else if (model instanceof String || model instanceof GString || model instanceof byte[]) {
            json = model; // assume valid JSON already.
        } else if (model instanceof File) {
            json = toString((File) model, contentTypeAsString);
        } else if (model instanceof InputStream) {
            json = IOUtils.toByteArray((InputStream)model); // assume valid JSON.
        } else {
            throw new UnsupportedOperationException("Internal error: Can't encode " + model + " to JSON.");
        }

        return createEntity(contentTypeAsString, json);
    }

    private HttpEntity createEntity(String ct, Object object) throws UnsupportedEncodingException {
        if (object instanceof byte[]) {
            return createEntity(ct, (byte[]) object);
        } else {
            return createEntity(ct, object.toString());
        }
    }

    protected HttpEntity createEntity(String ct, byte[] byteArray) {
        final ByteArrayEntity byteArrayEntity = new ByteArrayEntity(byteArray);
        byteArrayEntity.setContentType(ct);
        return byteArrayEntity;
    }

    /**
     * Helper method used by encoder methods to create an {@link HttpEntity}
     * instance that encapsulates the request data.  This may be used by any
     * non-streaming encoder that needs to send textual data.
     *
     * @param ct   content-type of the data
     * @param data textual request data to be encoded
     * @return an instance to be used for the
     * {@link HttpEntityEnclosingRequest#setEntity(HttpEntity) request content}
     * @throws UnsupportedEncodingException
     */
    protected HttpEntity createEntity(String ct, String data)
            throws UnsupportedEncodingException {
        String charset = CharsetExtractor.getCharsetFromContentType(ct);
        if (charset == null) {
            if (encoderConfig.hasDefaultCharsetForContentType(ct)) {
                charset = encoderConfig.defaultCharsetForContentType(ct);
            } else {
                charset = encoderConfig.defaultContentCharset();
            }
        }
        StringEntity entity = new StringEntity(data, charset);
        entity.setContentType(ct);
        return entity;
    }

    /**
     * Returns a map of default encoders.  Override this method to change
     * what encoders are registered by default.  You can of course call
     * <code>super.buildDefaultEncoderMap()</code> and then add or remove
     * from that result as well.
     */
    protected Map<String, Closure> buildDefaultEncoderMap() {
        Map<String, Closure> encoders = new HashMap<String, Closure>();

        encoders.put(ContentType.BINARY.toString(), new MethodClosure(this, "encodeStream"));
        encoders.put(ContentType.TEXT.toString(), new MethodClosure(this, "encodeText"));
        encoders.put(ContentType.URLENC.toString(), new MethodClosure(this, "encodeForm"));

        Closure encClosure = new MethodClosure(this, "encodeXML");
        for (String ct : ContentType.XML.getContentTypeStrings())
            encoders.put(ct, encClosure);
        encoders.put(ContentType.HTML.toString(), encClosure);

        encClosure = new MethodClosure(this, "encodeJSON");
        for (String ct : ContentType.JSON.getContentTypeStrings())
            encoders.put(ct, encClosure);

        return encoders;
    }

    /**
     * Retrieve a encoder for the given content-type.  This
     * is called by HTTPBuilder to retrieve the correct encoder for a given
     * content-type.  The encoder is then used to serialize the request data
     * in the request body.
     *
     * @param contentType
     * @return encoder that can interpret the given content type,
     * or null.
     */
    public Closure getAt(Object contentType) {
        String ct = contentType.toString();
        int idx = ct.indexOf(';');
        if (idx > 0) ct = ct.substring(0, idx);

        Closure closure = registeredEncoders.get(ct);
        if (closure == null) {
            final ContentType foundCt = ContentType.fromContentType(ct);
            if (foundCt != null) {
                closure = registeredEncoders.get(foundCt.toString());
            }
        }

        // We couldn't find an explicit encoder for the given content-type so try to find a match
        if (closure == null) {
            closure = tryToFindMatchingEncoder(ct);
        }

        // If no encoder could be found then use binary
        if (closure == null) {
            return getAt(ContentType.BINARY.toString());
        }
        return closure;
    }

    private Closure tryToFindMatchingEncoder(String contentType) {
        final Closure closure;
        if (contentType == null) {
            closure = null;
        } else if (StringUtils.startsWithIgnoreCase(contentType, "text/") || StringUtils.containsIgnoreCase(contentType, "+text")) {
            closure = new MethodClosure(this, "encodeText");
        } else {
            closure = null;
        }

        return closure;
    }

    /**
     * Register a new encoder for the given content type.  If any encoder
     * previously existed for that content type it will be replaced.  The
     * closure must return an {@link HttpEntity}.  It will also usually
     * accept a single argument, which will be whatever is set in the request
     * configuration closure via {@link HTTPBuilder.RequestConfigDelegate#setBody(Object)}.
     *
     * @param contentType
     */
    public void putAt(Object contentType, Closure value) {
        if (contentType instanceof ContentType) {
            for (String ct : ((ContentType) contentType).getContentTypeStrings())
                this.registeredEncoders.put(ct, value);
        } else this.registeredEncoders.put(contentTypeToString(contentType), value);
    }

    /**
     * Alias for {@link #getAt(Object)} to allow property-style access.
     *
     * @param key
     * @return
     */
    public Closure propertyMissing(Object key) {
        return this.getAt(key);
    }

    /**
     * Alias for {@link #putAt(Object, Closure)} to allow property-style access.
     *
     * @param key
     * @param value
     */
    public void propertyMissing(Object key, Closure value) {
        this.putAt(key, value);
    }

    /**
     * Iterate over the entire parser map
     *
     * @return
     */
    public Iterator<Map.Entry<String, Closure>> iterator() {
        return this.registeredEncoders.entrySet().iterator();
    }

    private String contentTypeToString(Object contentType) {
        return contentType == null ? null : contentType.toString();
    }

    private String toString(File model, String contentTypeAsString) {
        String charset = CharsetExtractor.getCharsetFromContentType(contentTypeAsString);
        return readToString(model, charset == null ? encoderConfig.defaultCharsetForContentType(contentTypeAsString) : charset);
    }
}