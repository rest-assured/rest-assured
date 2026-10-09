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

package io.restassured.internal.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsFileUsingTheGivenCharset() throws Exception {
        File file = write("åäö €".getBytes(StandardCharsets.UTF_8));

        assertThat(FileReader.readToString(file, "UTF-8")).isEqualTo("åäö €");
    }

    @Test
    void decodesBytesWithTheGivenCharsetEvenWhenItDiffersFromTheEncoding() throws Exception {
        File file = write("åäö".getBytes(StandardCharsets.UTF_8));

        assertThat(FileReader.readToString(file, "ISO-8859-1")).isEqualTo(new String("åäö".getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
    }

    @Test
    void readsIso88591EncodedFile() throws Exception {
        File file = write("åäö".getBytes(StandardCharsets.ISO_8859_1));

        assertThat(FileReader.readToString(file, "iso-8859-1")).isEqualTo("åäö");
    }

    @Test
    void replacesMalformedInputWithReplacementCharacter() throws Exception {
        File file = write(new byte[]{'a', (byte) 0xC3, 'b'});

        assertThat(FileReader.readToString(file, "UTF-8")).isEqualTo("a�b");
    }

    @Test
    void keepsByteOrderMarkAndLineEndings() throws Exception {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = "line1\r\nline2\rline3\n".getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, bytes, 0, bom.length);
        System.arraycopy(content, 0, bytes, bom.length, content.length);
        File file = write(bytes);

        assertThat(FileReader.readToString(file, "UTF-8")).isEqualTo("﻿line1\r\nline2\rline3\n");
    }

    @Test
    void readsEmptyFile() throws Exception {
        File file = write(new byte[0]);

        assertThat(FileReader.readToString(file, "UTF-8")).isEmpty();
    }

    @Test
    void readsFileLargerThanTheReadBuffer() throws Exception {
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 20_000; i++) {
            expected.append((char) ('a' + i % 26));
        }
        File file = write(expected.toString().getBytes(StandardCharsets.UTF_8));

        assertThat(FileReader.readToString(file, "UTF-8")).isEqualTo(expected.toString());
    }

    @Test
    void throwsFileNotFoundExceptionUnwrappedWhenFileDoesNotExist() {
        File file = tempDir.resolve("does-not-exist.txt").toFile();

        assertThatThrownBy(() -> FileReader.readToString(file, "UTF-8"))
                .isExactlyInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("does-not-exist.txt");
    }

    @Test
    void throwsFileNotFoundExceptionBeforeCheckingTheCharsetWhenFileDoesNotExist() {
        File file = tempDir.resolve("does-not-exist.txt").toFile();

        assertThatThrownBy(() -> FileReader.readToString(file, "no-such-charset"))
                .isExactlyInstanceOf(FileNotFoundException.class);
    }

    @Test
    void throwsFileNotFoundExceptionWhenFileIsADirectory() {
        assertThatThrownBy(() -> FileReader.readToString(tempDir.toFile(), "UTF-8"))
                .isExactlyInstanceOf(FileNotFoundException.class);
    }

    @Test
    void throwsUnsupportedEncodingExceptionUnwrappedWhenCharsetIsUnknown() throws Exception {
        File file = write("content".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> FileReader.readToString(file, "no-such-charset"))
                .isExactlyInstanceOf(UnsupportedEncodingException.class)
                .hasMessage("no-such-charset");
    }

    @Test
    void throwsNullPointerExceptionWhenCharsetIsNull() throws Exception {
        File file = write("content".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> FileReader.readToString(file, null))
                .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsNullPointerExceptionWhenFileIsNull() {
        assertThatThrownBy(() -> FileReader.readToString(null, "UTF-8"))
                .isExactlyInstanceOf(NullPointerException.class);
    }

    private File write(byte[] bytes) throws Exception {
        Path path = Files.createTempFile(tempDir, "file-reader", ".txt");
        Files.write(path, bytes);
        return path.toFile();
    }
}
