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


import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Content encoding used to handle Deflate responses.
 *
 * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
 */
public class DeflateEncoding extends ContentEncoding {

    private final boolean useNoWrapForInflateDecoding;

    public DeflateEncoding(boolean useNoWrapForInflateDecoding) {
        this.useNoWrapForInflateDecoding = useNoWrapForInflateDecoding;
    }

    /**
     * Returns the {@link ContentEncoding.Type#DEFLATE} encoding string which is
     * added to the <code>Accept-Encoding</code> header by the base class.
     */
    @Override
    public String getContentEncoding() {
        return Type.DEFLATE.toString();
    }


    /**
     * Wraps the raw entity in a {@link InflaterEntity}.
     */
    @Override
    public HttpEntity wrapResponseEntity(HttpEntity raw) {
        return new InflaterEntity(raw, useNoWrapForInflateDecoding);
    }

    /**
     * Entity used to interpret a Deflate-encoded response
     *
     * @author <a href='mailto:tomstrummer+httpbuilder@gmail.com'>Tom Nichols</a>
     */
    public static class InflaterEntity extends HttpEntityWrapper {

        private final boolean useNoWrapForInflateDecoding;

        public InflaterEntity(final HttpEntity entity, boolean useNoWrapForInflateDecoding) {
            super(entity);
            this.useNoWrapForInflateDecoding = useNoWrapForInflateDecoding;
        }

        /**
         * returns a {@link InflaterInputStream} which wraps the original entity's content stream
         *
         * @see HttpEntity#getContent()
         */
        @Override
        public InputStream getContent() throws IOException, IllegalStateException {
            return new InflaterInputStream(wrappedEntity.getContent(), new Inflater(useNoWrapForInflateDecoding));
        }

        /**
         * @return -1
         */
        @Override
        public long getContentLength() {
            // length of ungzipped content is not known
            return -1;
        }
    }
}
