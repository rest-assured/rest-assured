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

package io.restassured.internal.support;

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;

/**
 * Closes the response of a body that nobody reads, once nothing refers to the body any more. A shared client keeps the
 * connection of an unread body checked out, so without this every response that's never read would hold a connection.
 */
public final class UnreadBodies {
    private static final Cleaner CLEANER = Cleaner.create(runnable -> {
        Thread thread = new Thread(runnable, "rest-assured-unread-bodies");
        thread.setDaemon(true);
        return thread;
    });

    private UnreadBodies() {
    }

    /**
     * @param body     the body that's read later, if at all
     * @param response closed, before the body, when the returned stream is closed or once it's unreachable
     * @return the stream to read the body from instead of {@code body}
     */
    public static InputStream closeWhenUnreachable(InputStream body, Closeable response) {
        // The action mustn't refer to the stream it watches
        Runnable release = () -> {
            closeQuietly(response);
            closeQuietly(body);
        };
        ReleasingStream reachable = new ReleasingStream(body);
        reachable.cleanable = CLEANER.register(reachable, release);
        return reachable;
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException | RuntimeException ignored) {
            // Nobody is left to tell
        }
    }

    // Closing it releases the response at once, and only once, so the Cleaner has nothing left to do
    private static final class ReleasingStream extends FilterInputStream {
        private Cleaner.Cleanable cleanable;

        ReleasingStream(InputStream body) {
            super(body);
        }

        @Override
        public void close() {
            cleanable.clean();
        }
    }
}
