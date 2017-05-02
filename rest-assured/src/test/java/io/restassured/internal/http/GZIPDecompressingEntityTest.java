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
    @Test public void
    returns_gzipped_decompressed_content_when_content_length_is_minus_one() throws IOException {
        // Given
        String json = "{\"userId\":\"e047379\",\"ldapId\":\"0dfdf5a0-483c-45d7-8b24-8dd1299586c8\",\"firstName\":\"Ninju\",\"lastName\":\"BohraB\",\"cookieNotice\":true}";
        byte[] compressed = gzipCompress(json);

        // When
        GZIPDecompressingEntity yo = new GZIPDecompressingEntity(new ByteArrayEntity(compressed) {
            @Override
            public long getContentLength() {
                return -1;
            }
        });

        // Then
        String string = IOUtils.toString(yo.getContent());
        assertThat(string).isEqualTo(json);
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