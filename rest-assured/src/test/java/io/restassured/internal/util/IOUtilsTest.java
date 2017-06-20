package io.restassured.internal.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

public class IOUtilsTest {
    @Test
    public void toByteArray_small() throws Exception {
        byte[] input = new byte[]{1, 2, 3};
        byte[] output = IOUtils.toByteArray(new ByteArrayInputStream(input));
        assertThat(output).isEqualTo(input);
    }

    @Test
    public void toByteArray_big() throws Exception {
        int size = 100000; //bigger that internal buffer size
        byte[] input = new byte[size];
        Arrays.fill(input, (byte) 1);
        byte[] output = IOUtils.toByteArray(new ByteArrayInputStream(input));
        assertThat(output).isEqualTo(input);
    }

}