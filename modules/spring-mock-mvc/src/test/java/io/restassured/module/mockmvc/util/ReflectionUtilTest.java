package io.restassured.module.mockmvc.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class ReflectionUtilTest {

    @Test
    public void testInvokeMethod() {
        Object result = ReflectionUtil.invokeMethod(
                new TestClass(),
                "test",
                "/path/%s", new Object[] {"param"});

        assertThat(result).isEqualTo("/path/param");
    }

    @Test
    public void testInvokeMethodWithTyped() {
        Object result = ReflectionUtil.invokeMethod(
                new TestClass(),
                "test",
                "/path/", "param".split("\\|"));

        assertThat(result).isEqualTo("/path/[param]");
    }

    @Test
    public void testInvokeMethodWithTypedArray() {
        Object result = ReflectionUtil.invokeMethod(
                new TestClass(),
                "test",
                new Class[] {String.class, String[].class},
                "/path/", "param");

        assertThat(result).isEqualTo("/path/[param]");
    }

    @Test
    public void testInvokeMethodWithType() {
        Object result = ReflectionUtil.invokeMethod(
                new TestClass(),
                "test",
                new Class[] {String.class, Object[].class},
                "/path/%s", "param");

        assertThat(result).isEqualTo("/path/param");
    }

    @Test
    public void testInvokeMethodWithoutParam() {
        Object result = ReflectionUtil.invokeMethod(
                new TestClass(),
                "test",
                new Class[] {String.class, Object[].class},
                "/path/%s");

        assertThat(result).isEqualTo("/path/%s");
    }

    @Test
    public void testGetArgumentTypes() {
        Class<?>[] result = ReflectionUtil.getArgumentTypes(new Object[]{"", new Object[]{"1"}});
        assertThat(result).contains(String.class, Object[].class);
    }

    public static class TestClass {

        public String test(String name, Object... params) {
            if (params.length == 0) return name;
            return String.format(name, params);
        }

        public String test(String name, String... params) {
            if (params.length == 0) return name;
            return name + Arrays.toString(params);
        }
    }
}