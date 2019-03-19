package io.restassured.test.osgi.options;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.libraries.JUnitBundlesOption;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

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
        return new DefaultCompositeOption(new Option[]
                {
                        new JUnitBundlesOption(),
                        systemProperty("pax.exam.invoker").value("junit"),
                        mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.hamcrest", "1.3_1"),
                        bundle("link:classpath:META-INF/links/org.ops4j.pax.exam.invoker.junit.link")
                });
    }
}
