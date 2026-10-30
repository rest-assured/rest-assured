package io.restassured.test.osgi.options;

import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.apache5.Apache5HttpClientProvider;
import io.restassured.http.jdk.JdkHttpClientProvider;
import io.restassured.path.jayway.JaywayPath;
import io.restassured.path.json.JsonPath;
import io.restassured.path.xml.XmlPath;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.libraries.JUnitBundlesOption;

import java.net.URL;

import static org.ops4j.pax.exam.CoreOptions.*;

public class RestAssuredPaxExamOptions {

    private static final String SLF4J_2 = "2.0.20";

    private RestAssuredPaxExamOptions() {
        throw new AssertionError("Suppress default constructor for non-instantiability");
    }

    /* ---------------------------------------------------------------------------- */
    /* ---------------------------------------------------------------------------- */

    /**
     * Copied from CoreOptions.junitBundles() to replace the default pax-exam hamcrest bundle
     * (that does not contain all the nice hamcrest Matchers) by the ServiceMix Hamcrest bundle.
     *
     * https://groups.google.com/forum/#!topic/ops4j/zcow2vuOFJs
     * <p>
     * Hamcrest 3.0, which rest-assured imports, is installed first (at a lower start level). Its bundle imports org.hamcrest
     * without a version, so installed after the ServiceMix Hamcrest 1.3 bundle it would bind to that one's package, and
     * rest-assured wouldn't resolve.
     */
    public static Option restAssuredJunitBundles() {
        return new DefaultCompositeOption(mavenBundle("org.hamcrest", "hamcrest").versionAsInProject().startLevel(1),
                new JUnitBundlesOption(),
                systemProperty("pax.exam.invoker").value("junit"),
                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.hamcrest", "1.3_1"),
                bundle("link:classpath:META-INF/links/org.ops4j.pax.exam.invoker.junit.link"));
    }

    /**
     * Apache Aries SPI Fly (the OSGi ServiceLoader Mediator, which the Groovy bundle requires) together with the ASM bundles
     * it imports. The versions come from this module's pom (versionAsInProject). SPI Fly weaves bytecode with ASM, so the
     * ASM version must support the class file version of the classes in the container (Java 17 for REST Assured).
     */
    public static Option spiflyBundles() {
        return new DefaultCompositeOption(
                mavenBundle("org.ow2.asm", "asm").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-tree").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-analysis").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-commons").versionAsInProject(),
                mavenBundle("org.ow2.asm", "asm-util").versionAsInProject(),
                mavenBundle("org.apache.aries.spifly", "org.apache.aries.spifly.dynamic.bundle").versionAsInProject());
    }

    /**
     * The REST Assured bundles under test: json-path-gpath, xml-path-gpath, rest-assured, rest-assured-common and http-client-jdk.
     * <p>
     * Each bundle is the jar that this module's test classpath loads one of its classes from. In a reactor build, such as
     * {@code mvn verify -Posgi-tests -pl examples/rest-assured-itest-java-osgi -am} or the release build, that is the jar
     * the reactor just built, so the tests run against the code being built. {@code mavenBundle(..).versionAsInProject()}
     * would instead take whatever happens to be installed in the local Maven repository, which is stale or missing (for
     * example in the release build, which doesn't install the release version before running the tests).
     */
    public static Option restAssuredBundles() {
        return new DefaultCompositeOption(gpathBundles(), restAssuredCoreBundles());
    }

    /**
     * rest-assured, rest-assured-common and http-client-jdk, the default HTTP client that rest-assured requires, which
     * don't need a path engine or Groovy (see {@link #restAssuredBundles()}).
     */
    public static Option restAssuredCoreBundles() {
        return new DefaultCompositeOption(
                bundleContaining(RestAssured.class),
                bundleContaining(TypeRef.class),
                bundleContaining(JdkHttpClientProvider.class));
    }

    /**
     * The GPath path engines, json-path-gpath and xml-path-gpath, which need Groovy (see {@link #restAssuredBundles()}).
     */
    public static Option gpathBundles() {
        return new DefaultCompositeOption(
                bundleContaining(JsonPath.class),
                bundleContaining(XmlPath.class));
    }

    /**
     * The JSONPath engine, json-path-jayway (see {@link #restAssuredBundles()}), with Jayway JsonPath 3.0.0 and what its
     * bundle requires: json-smart, which needs accessors-smart and ASM, slf4j-api 2 (add {@link #slf4jBundles()} and
     * {@link #spiflyBundles()}), and Jackson 3's jackson-core and jackson-databind, with the Jackson 2 jackson-annotations
     * that jackson-databind 3 imports. Jayway 2.9.0 and 2.10.0 don't need Jackson. Jayway 2.8.0 imports slf4j-api 1.7
     * instead, which needs an SLF4J 1.7 binding bundle but no SPI Fly.
     */
    public static Option jaywayBundles() {
        return new DefaultCompositeOption(
                bundleContaining(JaywayPath.class),
                mavenBundle("com.jayway.jsonpath", "json-path").versionAsInProject(),
                mavenBundle("net.minidev", "json-smart").versionAsInProject(),
                mavenBundle("net.minidev", "accessors-smart").versionAsInProject(),
                mavenBundle("tools.jackson.core", "jackson-core").versionAsInProject(),
                mavenBundle("tools.jackson.core", "jackson-databind").versionAsInProject(),
                mavenBundle("com.fasterxml.jackson.core", "jackson-annotations").versionAsInProject());
    }

    /**
     * The HTTP client module http-client-apache5, the bundle built by this build, with Apache HttpClient 5, HttpCore 5 and
     * httpcore5-h2 in the versions of this module's pom. Their jars aren't bundles, so they're wrapped. They import each
     * other and SLF4J (add {@link #slf4jBundles()} and {@link #spiflyBundles()}). What HttpClient 5 uses only when it's
     * present, such as Conscrypt, Brotli, Commons Compress and zstd-jni, is imported optionally. httpcore5-h2 is needed
     * even for HTTP/1.1, since the classic client loads some of its classes at each connect.
     */
    public static Option apache5Bundles() {
        return new DefaultCompositeOption(
                bundleContaining(Apache5HttpClientProvider.class),
                wrappedHttpComponents("org.apache.httpcomponents.client5", "httpclient5"),
                wrappedHttpComponents("org.apache.httpcomponents.core5", "httpcore5"),
                wrappedHttpComponents("org.apache.httpcomponents.core5", "httpcore5-h2"));
    }

    /**
     * slf4j-api 2 with slf4j-simple, its provider, which slf4j-api finds with ServiceLoader. Add {@link #spiflyBundles()},
     * the OSGi ServiceLoader Mediator that makes that work. SLF4J 2, as Jayway 2.9.0 and later import org.slf4j [2.0,3).
     * HttpClient 5 depends on SLF4J 1.7 but only uses LoggerFactory and Logger, which SLF4J 2 still has. This module's own
     * slf4j-api is Pax Exam's 1.7, so the version is given here.
     */
    public static Option slf4jBundles() {
        return new DefaultCompositeOption(
                mavenBundle("org.slf4j", "slf4j-api").version(SLF4J_2),
                mavenBundle("org.slf4j", "slf4j-simple").version(SLF4J_2));
    }

    private static Option wrappedHttpComponents(String groupId, String artifactId) {
        return wrappedBundle(mavenBundle(groupId, artifactId).versionAsInProject())
                .imports("org.apache.hc.*", "org.slf4j", "*;resolution:=optional");
    }

    private static Option bundleContaining(Class<?> type) {
        URL location = type.getProtectionDomain().getCodeSource().getLocation();
        if (!location.getPath().endsWith(".jar")) {
            throw new IllegalStateException(type.getName() + " is loaded from " + location + ", which isn't a bundle jar. " +
                    "The OSGi tests need the packaged REST Assured bundles, so run them with at least the package phase, for example mvn verify.");
        }
        return bundle(location.toExternalForm());
    }
}
