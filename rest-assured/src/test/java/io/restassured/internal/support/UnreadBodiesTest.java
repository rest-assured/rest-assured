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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class UnreadBodiesTest {

    @Test
    void the_returned_stream_reads_the_body() throws IOException {
        InputStream body = UnreadBodies.closeWhenUnreachable(new ByteArrayInputStream("body".getBytes()), () -> { });

        assertThat(body.readAllBytes()).isEqualTo("body".getBytes());
    }

    @Test
    @Timeout(30)
    void closes_the_response_and_the_body_once_the_returned_stream_is_unreachable() throws InterruptedException {
        AtomicBoolean responseClosed = new AtomicBoolean();
        AtomicBoolean bodyClosed = new AtomicBoolean();
        InputStream body = new ByteArrayInputStream("body".getBytes()) {
            @Override
            public void close() {
                bodyClosed.set(true);
            }
        };
        UnreadBodies.closeWhenUnreachable(body, () -> responseClosed.set(true));

        while (!responseClosed.get() || !bodyClosed.get()) {
            System.gc();
            Thread.sleep(50);
        }
    }

    @Test
    void closing_the_returned_stream_closes_the_response_and_the_body_once() throws IOException {
        AtomicInteger responseClosed = new AtomicInteger();
        AtomicInteger bodyClosed = new AtomicInteger();
        InputStream body = new ByteArrayInputStream("body".getBytes()) {
            @Override
            public void close() {
                bodyClosed.incrementAndGet();
            }
        };
        InputStream returned = UnreadBodies.closeWhenUnreachable(body, responseClosed::incrementAndGet);

        returned.close();
        returned.close();

        assertThat(responseClosed).hasValue(1);
        assertThat(bodyClosed).hasValue(1);
    }

    @Test
    void doesnt_close_while_the_returned_stream_is_reachable() throws InterruptedException {
        AtomicBoolean responseClosed = new AtomicBoolean();
        InputStream reachable = UnreadBodies.closeWhenUnreachable(new ByteArrayInputStream(new byte[0]), () -> responseClosed.set(true));

        for (int i = 0; i < 5; i++) {
            System.gc();
            Thread.sleep(20);
        }

        assertThat(responseClosed).isFalse();
        assertThat(reachable).isNotNull();
    }
}
