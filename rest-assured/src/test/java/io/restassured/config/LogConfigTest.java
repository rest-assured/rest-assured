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
package io.restassured.config;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static io.restassured.config.LogConfig.logConfig;
import static org.assertj.core.api.Assertions.assertThat;

class LogConfigTest {

    private final PrintStream stream = new PrintStream(new ByteArrayOutputStream(), true);

    @Test
    void default_stream_keeps_pretty_printing_disabled() {
        LogConfig config = logConfig().enablePrettyPrinting(false).defaultStream(stream);

        assertThat(config.isPrettyPrintingEnabled()).isFalse();
        assertThat(config.defaultStream()).isSameAs(stream);
    }

    @Test
    void default_stream_keeps_pretty_printing_enabled() {
        LogConfig config = logConfig().defaultStream(stream);

        assertThat(config.isPrettyPrintingEnabled()).isTrue();
        assertThat(config.defaultStream()).isSameAs(stream);
    }
}
