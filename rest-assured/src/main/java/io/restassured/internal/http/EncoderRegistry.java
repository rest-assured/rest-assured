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

import io.restassured.common.spi.http.RequestBody;
import io.restassured.config.EncoderConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.http.ContentType;
import io.restassured.internal.mapping.ObjectMapping;
import io.restassured.internal.util.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import static io.restassured.internal.support.FileReader.readToString;
import static io.restassured.internal.util.GroovyTypes.isClosure;
import static io.restassured.internal.util.GroovyTypes.isGString;
import static java.nio.charset.StandardCharsets.ISO_8859_1;


/**
 * <p>This class handles creation of the request body (i.e. for a
 * PUT or POST operation) based on content-type.   When a
 * {@link HTTPBuilder.RequestConfigDelegate#setBody(Object) body} is set from the builder, it is
 * processed based on the {@link HTTPBuilder.RequestConfigDelegate#getRequestContentType()
 * request content-type}.  For instance, the {@link #encodeJSON(Object, Object)}
 * method will be invoked if the request content-type is JSON, which will
 * cause a Map body to be encoded as a JSON object in the request body.
 * Every encoder produces a {@link RequestBody}.</p>
 * <p/>
 * <p>A Groovy closure is not accepted as a request body.</p>
 * <p/>
 * <p>Contrary to its name, this class does not have anything to do with the
 * <code>content-encoding</code> HTTP header.  </p>
 *
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 */
public class EncoderRegistry {

    private Map<String, RequestBodyEncoder> registeredEncoders = buildDefaultEncoderMap();
    private EncoderConfig encoderConfig = new EncoderConfig();
    private ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();

    /**
     * Set the encoder config
     */
    public void setEncoderConfig(EncoderConfig encoderConfig) {
        this.encoderConfig = encoderConfig;
    }

    /**
     * Set the object mapper config, which writes the values of a Map or Collection JSON body that aren't JDK types
     */
    public void setObjectMapperConfig(ObjectMapperConfig objectMapperConfig) {
        this.objectMapperConfig = objectMapperConfig;
    }

    /**
     * Default request encoder for a binary stream.  Acceptable argument
     * types are:
     * <ul>
     * <li>InputStream</li>
     * <li>byte[] / ByteArrayOutputStream</li>
     * <li>File</li>
     * </ul>
     *
     * @param data
     * @return the request body
     */
    public RequestBody encodeStream(Object contentType, Object data) {
        rejectClosure(data);
        String contentTypeAsString = contentTypeToString(contentType);

        if (data instanceof ByteArrayInputStream) {
            // special case for ByteArrayIS so that we can set the content length.
            ByteArrayInputStream in = ((ByteArrayInputStream) data);
            return RequestBody.stream(in, in.available(), contentTypeAsString);
        } else if (data instanceof InputStream) {
            return RequestBody.stream((InputStream) data, RequestBody.UNKNOWN_LENGTH, contentTypeAsString);
        } else if (data instanceof File) {
            // Sent with the file size, since we already know it at this time. Ref: Issue #988
            return RequestBody.file((File) data, contentTypeAsString);
        } else if (data instanceof byte[]) {
            byte[] out = ((byte[]) data);
            return RequestBody.stream(new ByteArrayInputStream(out), out.length, contentTypeAsString);
        } else if (data instanceof ByteArrayOutputStream) {
            ByteArrayOutputStream out = ((ByteArrayOutputStream) data);
            return RequestBody.stream(new ByteArrayInputStream(out.toByteArray()), out.size(), contentTypeAsString);
        }

        throw new IllegalArgumentException(
                "Don't know how to encode " + data + " as a byte stream.\n\nPlease use EncoderConfig (EncoderConfig#encodeContentTypeAs) to specify how to serialize data for this content-type.\n" +
                        "For example: \"given().config(RestAssured.config().encoderConfig(encoderConfig().encodeContentTypeAs(\"" + ContentTypeExtractor.getContentTypeWithoutCharset(contentTypeAsString) + "\", ContentType.TEXT))). ..\"");
    }

    /**
     * Default handler used for a plain text content-type.  Acceptable argument
     * types are:
     * <ul>
     * <li>Reader</li>
     * <li>File</li>
     * <li>byte[] / InputStream</li>
     * <li>any other object, whose <code>toString()</code> is sent</li>
     * </ul>
     *
     * @param data
     * @return the request body
     * @throws IOException
     */
    public RequestBody encodeText(Object contentType, Object data) throws IOException {
        rejectClosure(data);
        String contentTypeAsString = contentTypeToString(contentType);
        if (data instanceof Reader) {
            data = readAndClose((Reader) data);
        } else if (data instanceof File) {
            data = toString((File) data, contentTypeAsString);
        }
        // if data is a String, we are already covered.
        return createBody(contentTypeAsString, data);
    }

    /**
     * Encodes a request body that is sent without a content-type (see
     * {@link io.restassured.specification.RequestSpecification#noContentType()}). Binary content (an InputStream,
     * byte[], ByteArrayOutputStream or File) is sent as is, anything else as text in the
     * {@link EncoderConfig#defaultContentCharset() default content charset}. The body has no content-type.
     *
     * @param data the request body
     * @return the request body
     * @throws IOException
     */
    public RequestBody encodeWithoutContentType(Object data) throws IOException {
        if (data instanceof InputStream || data instanceof byte[] || data instanceof ByteArrayOutputStream || data instanceof File) {
            return encodeStream(null, data);
        }
        return encodeText(null, data);
    }

    /**
     * Accepts a String as a url-encoded form post.  This method assumes the
     * String is an already-encoded POST string.
     *
     * @param formData a url-encoded form POST string.  See
     *                 <a href='http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.1'>
     *                 The W3C spec</a> for more info.
     * @return the request body
     */
    public RequestBody encodeForm(Object contentType, String formData) {
        return this.createBody(contentTypeToString(contentType), formData);
    }

    /**
     * The default encoder for a url-encoded form content-type. The body must be an already-encoded String, a GString
     * is converted to a String.
     */
    private RequestBody encodeFormBody(Object contentType, Object body) {
        rejectClosure(body);
        final String formData;
        if (body == null || body instanceof String) {
            formData = (String) body;
        } else if (isGString(body)) {
            formData = body.toString();
        } else {
            throw new IllegalArgumentException("Don't know how to encode a request body of type " + body.getClass().getTypeName() +
                    " as content-type " + contentType + ". A form url-encoded request body must be a String, " +
                    "use formParam(..) or formParams(..) to send form parameters.");
        }
        return encodeForm(contentType, formData);
    }

    /**
     * Encode the content as XML.  The argument may be a File, a byte[] or
     * InputStream, or an object whose <code>toString</code> produces valid markup.
     *
     * @param xml data that defines the XML structure
     * @return the request body
     */
    public RequestBody encodeXML(Object contentType, Object xml) {
        rejectClosure(xml);
        String contentTypeAsString = contentTypeToString(contentType);
        if (xml instanceof File) {
            xml = toString((File) xml, contentTypeAsString);
        }
        return createBody(contentTypeAsString, xml);
    }

    /**
     * <p>A Map or Collection is converted to JSON by {@link JsonBodyWriter}, with
     * values that aren't JDK types written by the configured object mapper. A String or GString will be interpreted as
     * valid JSON and passed directly as the request body (with charset
     * conversion if necessary). A File, byte[] or InputStream is assumed to
     * contain valid JSON.</p>
     *
     * @param model data to be converted to JSON, as specified above.
     * @return the request body
     * @throws IOException
     */
    public RequestBody encodeJSON(Object contentType, Object model) throws IOException {
        rejectClosure(model);
        String contentTypeAsString = contentTypeToString(contentType);
        Object json;
        if (model instanceof Map || model instanceof Collection) {
            json = new JsonBodyWriter(value -> ObjectMapping.serialize(value, ContentType.JSON.toString(), null, null, objectMapperConfig, encoderConfig))
                    .write(model);
        } else if (model instanceof String || isGString(model) || model instanceof byte[]) {
            json = model; // assume valid JSON already.
        } else if (model instanceof File) {
            json = toString((File) model, contentTypeAsString);
        } else if (model instanceof InputStream) {
            json = IOUtils.toByteArray((InputStream)model); // assume valid JSON.
        } else {
            throw new UnsupportedOperationException("Internal error: Can't encode " + model + " to JSON.");
        }

        return createBody(contentTypeAsString, json);
    }

    private RequestBody createBody(String ct, Object object) {
        if (object instanceof byte[]) {
            return RequestBody.bytes((byte[]) object, ct);
        } else if (object instanceof InputStream) {
            return RequestBody.stream((InputStream) object, RequestBody.UNKNOWN_LENGTH, ct);
        } else {
            return createBody(ct, object.toString());
        }
    }

    /**
     * Creates the body of textual request data, encoded in the charset of the content-type, else the configured
     * default charset for the content-type, else the {@link EncoderConfig#defaultContentCharset() default content charset}.
     *
     * @param ct   content-type of the data
     * @param data textual request data to be encoded
     * @return the request body
     */
    private RequestBody createBody(String ct, String data) {
        String charset = CharsetExtractor.getCharsetFromContentType(ct);
        if (charset == null) {
            if (encoderConfig.hasDefaultCharsetForContentType(ct)) {
                charset = encoderConfig.defaultCharsetForContentType(ct);
            } else {
                charset = encoderConfig.defaultContentCharset();
            }
        }
        // Like HttpClient 4's StringEntity did: no charset means ISO-8859-1
        Charset charsetToUse = StringUtils.isBlank(charset) ? ISO_8859_1 : Charset.forName(charset);
        return RequestBody.bytes(data.getBytes(charsetToUse), ct);
    }

    /**
     * Returns a map of default encoders.  Override this method to change
     * what encoders are registered by default.  You can of course call
     * <code>super.buildDefaultEncoderMap()</code> and then add or remove
     * from that result as well.
     */
    protected Map<String, RequestBodyEncoder> buildDefaultEncoderMap() {
        Map<String, RequestBodyEncoder> encoders = new HashMap<>();

        encoders.put(ContentType.BINARY.toString(), this::encodeStream);
        encoders.put(ContentType.TEXT.toString(), this::encodeText);
        encoders.put(ContentType.URLENC.toString(), this::encodeFormBody);

        RequestBodyEncoder xmlEncoder = this::encodeXML;
        for (String ct : ContentType.XML.getContentTypeStrings())
            encoders.put(ct, xmlEncoder);
        encoders.put(ContentType.HTML.toString(), xmlEncoder);

        RequestBodyEncoder jsonEncoder = this::encodeJSON;
        for (String ct : ContentType.JSON.getContentTypeStrings())
            encoders.put(ct, jsonEncoder);

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
    public RequestBodyEncoder getAt(Object contentType) {
        String ct = contentType.toString();
        int idx = ct.indexOf(';');
        if (idx > 0) ct = ct.substring(0, idx);

        RequestBodyEncoder encoder = registeredEncoders.get(ct);
        if (encoder == null) {
            final ContentType foundCt = ContentType.fromContentType(ct);
            if (foundCt != null) {
                encoder = registeredEncoders.get(foundCt.toString());
            }
        }

        // We couldn't find an explicit encoder for the given content-type so try to find a match
        if (encoder == null) {
            encoder = tryToFindMatchingEncoder(ct);
        }

        // If no encoder could be found then use binary
        if (encoder == null) {
            return getAt(ContentType.BINARY.toString());
        }
        return encoder;
    }

    private RequestBodyEncoder tryToFindMatchingEncoder(String contentType) {
        final RequestBodyEncoder encoder;
        if (contentType == null) {
            encoder = null;
        } else if (StringUtils.startsWithIgnoreCase(contentType, "text/") || StringUtils.containsIgnoreCase(contentType, "+text")) {
            encoder = this::encodeText;
        } else {
            encoder = null;
        }

        return encoder;
    }

    /**
     * Register a new encoder for the given content type.  If any encoder
     * previously existed for that content type it will be replaced.  The
     * encoder is called with the request content-type and the request body,
     * which is whatever is set via {@link HTTPBuilder.RequestConfigDelegate#setBody(Object, Object)}.
     *
     * @param contentType
     */
    public void putAt(Object contentType, RequestBodyEncoder value) {
        if (contentType instanceof ContentType) {
            for (String ct : ((ContentType) contentType).getContentTypeStrings())
                this.registeredEncoders.put(ct, value);
        } else this.registeredEncoders.put(contentTypeToString(contentType), value);
    }

    /**
     * Iterate over the entire parser map
     *
     * @return
     */
    public Iterator<Map.Entry<String, RequestBodyEncoder>> iterator() {
        return this.registeredEncoders.entrySet().iterator();
    }

    private String contentTypeToString(Object contentType) {
        return contentType == null ? null : contentType.toString();
    }

    private static void rejectClosure(Object body) {
        if (isClosure(body)) {
            throw new IllegalArgumentException("A Groovy closure (" + body.getClass().getName() + ") is not supported as request body. " +
                    "Serialize the body to a String, byte[] or InputStream instead, for example in your ObjectMapper.");
        }
    }

    private static String readAndClose(Reader reader) throws IOException {
        StringWriter out = new StringWriter();
        try (Reader r = reader) {
            char[] chars = new char[8192];
            for (int read; (read = r.read(chars)) != -1; ) {
                out.write(chars, 0, read);
            }
        }
        return out.toString();
    }

    private String toString(File model, String contentTypeAsString) {
        String charset = CharsetExtractor.getCharsetFromContentType(contentTypeAsString);
        return readToString(model, charset == null ? encoderConfig.defaultCharsetForContentType(contentTypeAsString) : charset);
    }
}