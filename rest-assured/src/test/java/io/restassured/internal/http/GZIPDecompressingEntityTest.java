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

import io.restassured.internal.http.GZIPEncoding.GZIPDecompressingEntity;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class GZIPDecompressingEntityTest {


    // Asserts that issue 853 is resolved
    @Test
    public void
    returns_gzipped_decompressed_content_when_content_length_is_minus_one() throws IOException {
        // Given
        String json = "{\"userId\":\"e047379\",\"ldapId\":\"0dfdf5a0-483c-45d7-8b24-8dd1299586c8\",\"firstName\":\"Ninju\",\"lastName\":\"BohraB\",\"cookieNotice\":true}";
        byte[] compressed = gzipCompress(json);

        // When
        GZIPDecompressingEntity gzipDecompressingEntity = new GZIPDecompressingEntity(new ByteArrayEntity(compressed) {
            @Override
            public long getContentLength() {
                return -1;
            }
        });

        // Then
        String string = IOUtils.toString(gzipDecompressingEntity.getContent());
        assertThat(string).isEqualTo(json);
    }


    //Adjustments for 814
    @Test
    public void should_not_fail_on_empty_response_with_gzip() throws Exception {
        //Given
        byte[] input = new byte[0];

        //When
        GZIPDecompressingEntity gzipDecompressingEntity = new GZIPDecompressingEntity(new ByteArrayEntity(input) {
            @Override
            public long getContentLength() {
                return -1;
            }
        });

        //Then
        byte[] response = IOUtils.toByteArray(gzipDecompressingEntity.getContent());
        assertThat(response.length).isEqualTo(0);
    }

    private byte[] gzipCompress(String string) throws IOException {
        byte[] bytes = string.getBytes("UTF-8");
        ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(bytes);
        gzip.close();
        byte[] compressed = bos.toByteArray();
        bos.close();
        return compressed;
    }
}