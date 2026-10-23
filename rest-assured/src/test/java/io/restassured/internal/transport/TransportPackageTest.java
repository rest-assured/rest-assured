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

package io.restassured.internal.transport;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The transport types move to rest-assured-common in 7.0, so they may only use JDK types and the neutral types of core.
 */
class TransportPackageTest {

    @Test
    void transport_types_use_no_http_client_library() throws IOException {
        Path packageDirectory = Paths.get("src/main/java/io/restassured/internal/transport");
        List<String> imports;
        try (Stream<Path> files = Files.list(packageDirectory)) {
            imports = files.flatMap(file -> {
                try {
                    return Files.readAllLines(file).stream().filter(line -> line.startsWith("import "));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        }

        assertThat(imports).isNotEmpty().allSatisfy(line ->
                assertThat(line).matches("import (static )?(java\\.|io\\.restassured\\.internal\\.http\\.RequestBody;).*"));
    }
}
