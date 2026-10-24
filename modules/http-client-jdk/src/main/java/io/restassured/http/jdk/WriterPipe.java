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

package io.restassured.http.jdk;

import io.restassured.common.spi.http.RequestBody.ContentWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The bytes that a {@link ContentWriter} writes, as a stream that the JDK client reads. The JDK client pulls the body
 * from a stream, while the writer pushes it, so a daemon thread runs the writer and hands the bytes over in chunks. A
 * writer that fails makes the stream fail once the bytes written before have been read.
 */
final class WriterPipe extends InputStream {
    private static final int CHUNK_SIZE = 8192;
    private static final byte[] END = new byte[0];

    private final BlockingQueue<byte[]> chunks = new ArrayBlockingQueue<>(16);
    private volatile Throwable failure;
    private volatile boolean closed;
    private byte[] chunk = new byte[0];
    private int position;

    private WriterPipe() {
    }

    /**
     * @return the stream of what the writer writes, written by a thread that starts now
     */
    static WriterPipe start(ContentWriter writer) {
        WriterPipe pipe = new WriterPipe();
        Thread thread = new Thread(() -> pipe.write(writer), "rest-assured-request-body");
        thread.setDaemon(true);
        thread.start();
        return pipe;
    }

    private void write(ContentWriter writer) {
        ChunkingOutputStream out = new ChunkingOutputStream();
        try {
            writer.writeTo(out);
            out.flushBuffer();
        } catch (Throwable e) {
            // Set before the end, so that the reader reports it
            failure = e;
        }
        put(END);
    }

    // Returns false once the reader has closed the stream
    private boolean put(byte[] bytes) {
        try {
            while (!closed) {
                if (chunks.offer(bytes, 100, TimeUnit.MILLISECONDS)) {
                    return true;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    @Override
    public int read() throws IOException {
        byte[] one = new byte[1];
        int read = read(one, 0, 1);
        return read == -1 ? -1 : one[0] & 0xff;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        if (length == 0) {
            return 0;
        }
        while (position == chunk.length) {
            if (chunk == END) {
                return endOfStream();
            }
            try {
                chunk = chunks.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Interrupted while waiting for the request body");
            }
            position = 0;
        }
        int read = Math.min(length, chunk.length - position);
        System.arraycopy(chunk, position, bytes, offset, read);
        position += read;
        return read;
    }

    private int endOfStream() throws IOException {
        Throwable cause = failure;
        if (cause != null) {
            throw new IOException("Writing the request body failed: " + cause, cause);
        }
        return -1;
    }

    @Override
    public void close() {
        closed = true;
        chunks.clear();
    }

    private final class ChunkingOutputStream extends OutputStream {
        private final byte[] buffer = new byte[CHUNK_SIZE];
        private int count;

        @Override
        public void write(int b) throws IOException {
            if (count == buffer.length) {
                flushBuffer();
            }
            buffer[count++] = (byte) b;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            while (length > 0) {
                if (count == buffer.length) {
                    flushBuffer();
                }
                int copied = Math.min(length, buffer.length - count);
                System.arraycopy(bytes, offset, buffer, count, copied);
                count += copied;
                offset += copied;
                length -= copied;
            }
        }

        private void flushBuffer() throws IOException {
            if (count > 0) {
                if (!put(Arrays.copyOf(buffer, count))) {
                    throw new IOException("The request body is no longer read");
                }
                count = 0;
            }
        }

        // The pipe puts the end once the writer has returned
        @Override
        public void close() throws IOException {
            flushBuffer();
        }
    }
}
