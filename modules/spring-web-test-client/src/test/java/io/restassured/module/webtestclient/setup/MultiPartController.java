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

package io.restassured.module.webtestclient.setup;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@RestController
public class MultiPartController {

    /**
     * @return one line per part that the server received: its name, file name, content-type and content, with the
     * content's bytes as ISO-8859-1, which maps each byte to one char
     */
    @PostMapping(value = "/multipart", consumes = MULTIPART_FORM_DATA_VALUE, produces = TEXT_PLAIN_VALUE)
    public Mono<String> multipart(@RequestBody Flux<Part> parts) {
        return parts.concatMap(part -> DataBufferUtils.join(part.content()).map(content -> {
            String text = content.toString(StandardCharsets.ISO_8859_1);
            DataBufferUtils.release(content);
            return part.name() + " | " + part.headers().getContentDisposition().getFilename() + " | " + part.headers().getContentType() + " | " + text;
        })).collect(Collectors.joining("\n"));
    }
}
