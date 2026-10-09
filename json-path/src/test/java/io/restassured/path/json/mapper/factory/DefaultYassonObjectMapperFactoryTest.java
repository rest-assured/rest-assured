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
package io.restassured.path.json.mapper.factory;

import jakarta.json.bind.Jsonb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultYassonObjectMapperFactoryTest {

    @Test
    @Timeout(30)
    public void allThreadsGetTheSameJsonbInstanceWhenTheyCreateItAtTheSameTime() throws Exception {
        final int numberOfThreads = 16;
        final int rounds = 5;
        final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        try {
            for (int round = 0; round < rounds; round++) {
                clearCachedJsonb();
                final CountDownLatch ready = new CountDownLatch(numberOfThreads);
                final CountDownLatch start = new CountDownLatch(1);
                final List<Future<Jsonb>> futures = new ArrayList<>();
                for (int i = 0; i < numberOfThreads; i++) {
                    futures.add(executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return new DefaultYassonObjectMapperFactory().create(Object.class, "UTF-8");
                    }));
                }
                ready.await();
                start.countDown();

                final Jsonb first = futures.get(0).get();
                for (Future<Jsonb> future : futures) {
                    assertThat(future.get()).isSameAs(first);
                }
            }
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // The cache is static, so clear it to make every round race the first creation
    private static void clearCachedJsonb() throws Exception {
        Field field = DefaultYassonObjectMapperFactory.class.getDeclaredField("cachedJsonb");
        field.setAccessible(true);
        field.set(null, null);
    }
}
