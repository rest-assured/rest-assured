package io.restassured.module.mockmvc.util;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReflectionUtilTest extends TestCase {

    public void testInvokeMethod() {
        Object result = ReflectionUtil.invokeMethod(
                new TestClass(),
                "test",
                "/path/%s", new Object[] {"param"});

        assertThat(result).isEqualTo("/path/param");
    }

    public void testInvokeMethodWithTyped() {
        Object result = ReflectionUtil.invokeMethod(
                new TestClass(),
                "test",
                "/path/", "param".split("\\|"));

        assertThat(result).isEqualTo("/path/[param]");
    }

    public void testInvokeMethodWithTypedArray() {
        Object result = ReflectionUtil.invokeMethod(
                new TestClass(),
                "test",
                new Class[] {String.class, String[].class},
                "/path/", "param");

        assertThat(result).isEqualTo("/path/[param]");
    }


    public void testInvokeMethodWithType() {
        Object result = ReflectionUtil.invokeMethod(
                new TestClass(),
                "test",
                new Class[] {String.class, Object[].class},
                "/path/%s", "param");

        assertThat(result).isEqualTo("/path/param");
    }

    public void testInvokeMethodWithoutParam() {
        Object result = ReflectionUtil.invokeMethod(
                new TestClass(),
                "test",
                new Class[] {String.class, Object[].class},
                "/path/%s");

        assertThat(result).isEqualTo("/path/%s");
    }

    public void testGetArgumentTypes() {
        Class<?>[] result = ReflectionUtil.getArgumentTypes(new Object[]{"", new Object[]{"1"}});
        assertThat(result).contains(String.class, Object[].class);
    }

    /**
     * Reproduces #1842: Spring's MockHttpServletRequestBuilder exposes both a real
     * {@code cookie(Cookie...)} method and a synthetic bridge with the same array parameter
     * type where {@code isVarArgs()} is false. Passing a single cookie element must still
     * pack into Cookie[] instead of causing "argument type mismatch".
     */
    public void testCookieStyleInvocationPacksSingleElementWhenLastParamIsArray() {
        CookieReceiver receiver = new CookieReceiver();
        FakeCookie cookie = new FakeCookie("MyCookie", "MyCookieValue");

        ReflectionUtil.invokeMethod(
                receiver,
                "cookie",
                new Class[]{FakeCookie[].class},
                cookie);

        assertThat(receiver.names).containsExactly("MyCookie");
        assertThat(receiver.values).containsExactly("MyCookieValue");
    }

    public void testCookieStyleInvocationAcceptsPrebuiltArray() {
        CookieReceiver receiver = new CookieReceiver();
        FakeCookie[] cookies = {
                new FakeCookie("a", "1"),
                new FakeCookie("b", "2")
        };

        ReflectionUtil.invokeMethod(
                receiver,
                "cookie",
                new Class[]{FakeCookie[].class},
                (Object) cookies);

        assertThat(receiver.names).containsExactly("a", "b");
    }

    /**
     * Covariant override produces bridge methods (isVarArgs often false) similar to
     * MockHttpServletRequestBuilder.cookie vs AbstractMockHttpServletRequestBuilder.cookie.
     */
    public void testCovariantVarargsOverrideBridgeDoesNotCauseTypeMismatch() throws Exception {
        ConcreteRequestBuilder builder = new ConcreteRequestBuilder();

        boolean sawBridgeWithArrayParam = false;
        boolean sawVarargs = false;
        for (Method m : ConcreteRequestBuilder.class.getMethods()) {
            if (!"cookie".equals(m.getName()) || m.getParameterCount() != 1) {
                continue;
            }
            if (m.getParameterTypes()[0].isArray()) {
                if (m.isBridge() || m.isSynthetic()) {
                    sawBridgeWithArrayParam = true;
                    assertThat(m.isVarArgs())
                            .as("bridge cookie method is typically not marked varargs")
                            .isFalse();
                }
                if (m.isVarArgs()) {
                    sawVarargs = true;
                }
            }
        }
        assertThat(sawVarargs).as("expected a real varargs cookie method").isTrue();
        assertThat(sawBridgeWithArrayParam)
                .as("expected a bridge/synthetic cookie method from covariant override")
                .isTrue();

        FakeCookie cookie = new FakeCookie("session", "abc");
        // Same call shape as MockMvcRequestSenderImpl.applyCookies
        Object result = ReflectionUtil.invokeMethod(
                builder,
                "cookie",
                new Class[]{FakeCookie[].class},
                cookie);

        assertThat(result).isSameAs(builder);
        assertThat(builder.received).containsExactly(cookie);
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

    /** Minimal stand-in for jakarta/javax.servlet.http.Cookie */
    public static final class FakeCookie {
        final String name;
        final String value;

        FakeCookie(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class CookieReceiver {
        final List<String> names = new ArrayList<String>();
        final List<String> values = new ArrayList<String>();

        public void cookie(FakeCookie... cookies) {
            if (cookies == null) {
                return;
            }
            for (FakeCookie c : cookies) {
                names.add(c.name);
                values.add(c.value);
            }
        }
    }

    /**
     * Mirrors Spring's AbstractMockHttpServletRequestBuilder / MockHttpServletRequestBuilder
     * hierarchy: abstract builder returns B, concrete builder overrides with covariant return.
     */
    public abstract static class AbstractRequestBuilder<B extends AbstractRequestBuilder<B>> {
        final List<FakeCookie> received = new ArrayList<FakeCookie>();

        @SuppressWarnings("unchecked")
        public B cookie(FakeCookie... cookies) {
            if (cookies != null) {
                received.addAll(Arrays.asList(cookies));
            }
            return (B) this;
        }
    }

    public static class ConcreteRequestBuilder extends AbstractRequestBuilder<ConcreteRequestBuilder> {
        @Override
        public ConcreteRequestBuilder cookie(FakeCookie... cookies) {
            return super.cookie(cookies);
        }
    }
}
