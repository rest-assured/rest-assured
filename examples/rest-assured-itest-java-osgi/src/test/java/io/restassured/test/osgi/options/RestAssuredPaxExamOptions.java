package io.restassured.test.osgi.options;

import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.path.json.JsonPath;
import io.restassured.path.xml.XmlPath;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.libraries.JUnitBundlesOption;

import java.net.URL;

import static org.ops4j.pax.exam.CoreOptions.*;

public class RestAssuredPaxExamOptions {

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
     */
    public static Option restAssuredJunitBundles() {
        return new DefaultCompositeOption(new JUnitBundlesOption(),
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
     * The REST Assured bundles under test: json-path, xml-path, rest-assured and rest-assured-common.
     * <p>
     * Each bundle is the jar that this module's test classpath loads one of its classes from. In a reactor build, such as
     * {@code mvn verify -Posgi-tests -pl examples/rest-assured-itest-java-osgi -am} or the release build, that is the jar
     * the reactor just built, so the tests run against the code being built. {@code mavenBundle(..).versionAsInProject()}
     * would instead take whatever happens to be installed in the local Maven repository, which is stale or missing (for
     * example in the release build, which doesn't install the release version before running the tests).
     */
    public static Option restAssuredBundles() {
        return new DefaultCompositeOption(
                bundleContaining(JsonPath.class),
                bundleContaining(XmlPath.class),
                bundleContaining(RestAssured.class),
                bundleContaining(TypeRef.class));
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
