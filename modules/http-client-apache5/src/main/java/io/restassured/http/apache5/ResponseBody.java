/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.http.apache5;

import io.restassured.common.exception.ResponseTimeoutException;

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

/**
 * The body of a response, which notes whether it has been read to its end, so that closing it before then drops the
 * connection instead of reading the rest (HttpClient 5.4 and later read the rest, which never ends for an endless
 * stream). A read timeout throws a {@link ResponseTimeoutException}.
 */
final class ResponseBody extends FilterInputStream {
    private final long contentLength;
    private final Closeable onClose;
    private long read;
    private boolean atEnd;

    /**
     * @param contentLength the length of the body, or {@code -1} if it's unknown
     * @param onClose       what closing the body does
     */
    ResponseBody(InputStream body, long contentLength, Closeable onClose) {
        super(body);
        this.contentLength = contentLength;
        this.onClose = onClose;
    }

    @Override
    public int read() throws IOException {
        try {
            int b = super.read();
            count(b == -1 ? -1 : 1);
            return b;
        } catch (SocketTimeoutException e) {
            throw new ResponseTimeoutException(e.getMessage(), e);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            // HttpClient 5.2 waits for more of the body first, which InputStream.readNBytes(..) asks for at the end
            return 0;
        }
        try {
            return count(super.read(b, off, len));
        } catch (SocketTimeoutException e) {
            throw new ResponseTimeoutException(e.getMessage(), e);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        try {
            long skipped = super.skip(n);
            read += skipped;
            return skipped;
        } catch (SocketTimeoutException e) {
            throw new ResponseTimeoutException(e.getMessage(), e);
        }
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    private int count(int bytes) {
        if (bytes == -1) {
            atEnd = true;
        } else {
            read += bytes;
        }
        return bytes;
    }

    /**
     * @return whether the whole body has been read. The rest of a body of known length that has already arrived is
     * read here, without waiting, so that the connection can be used again.
     */
    boolean isAtEnd() {
        if (atEnd || contentLength >= 0 && read >= contentLength) {
            return true;
        }
        try {
            long rest = contentLength - read;
            if (contentLength >= 0 && rest <= in.available()) {
                in.skipNBytes(rest);
                return true;
            }
        } catch (IOException e) {
            // Then the connection isn't used again
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        onClose.close();
    }
}
