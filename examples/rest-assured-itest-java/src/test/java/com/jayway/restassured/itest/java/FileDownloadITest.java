/*
 * Copyright 2012 the original author or authors.
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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.support.WithJetty;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class FileDownloadITest extends WithJetty {

    @Test
    public void canDownloadLargeFiles() throws Exception {
        int expectedSize = IOUtils.toByteArray(getClass().getResourceAsStream("/powermock-easymock-junit-1.4.12.zip")).length;
        final InputStream inputStream = expect().log().headers().when().get("http://powermock.googlecode.com/files/powermock-easymock-junit-1.4.12.zip").asInputStream();


        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, byteArrayOutputStream);
        IOUtils.closeQuietly(byteArrayOutputStream);
        IOUtils.closeQuietly(inputStream);

        assertThat(byteArrayOutputStream.size(), equalTo(expectedSize));
    }
}
