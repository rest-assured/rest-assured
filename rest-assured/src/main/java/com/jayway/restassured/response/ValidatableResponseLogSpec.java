package com.jayway.restassured.response;


import org.hamcrest.Matcher;

public interface ValidatableResponseLogSpec {


    /**
     * Logs only the status line (includes the status code)
     *
     * @return The validatable response specification
     */
    ValidatableResponse status();

    /**
     * Logs everything only if an error occurs (status code >= 400).
     *
     * @return The validatable response specification
     */
    ValidatableResponse ifError();

    /**
     * Logs everything only if if the status code is equal to <code>statusCode</code>.
     *
     * @param statusCode The status code
     * @return The validatable response specification
     */
    ValidatableResponse ifStatusCodeIsEqualTo(int statusCode);

    /**
     * Logs everything only if if the status code matches the supplied <code>matcher</code>
     *
     * @param matcher The hamcrest matcher
     * @return The validatable response specification
     */
    ValidatableResponse ifStatusCodeMatches(Matcher<Integer> matcher);

    /**
     * Logs only the content of the body. The body will be pretty-printed by default if content-type is either XML, JSON or HTML.
     *
     * @return The specification
     */
    ValidatableResponse body();

    /**
     * Logs only the content of the body and pretty-print the body if specified. Note that pretty-printing can only take place if the
     * content-type is either XML, JSON or HTML.
     *
     * @param shouldPrettyPrint <code>true</code> if the body should be pretty-printed, <code>false</code> otherwise.
     * @return The specification
     */
    ValidatableResponse body(boolean shouldPrettyPrint);

    /**
     * Logs everything in the response, including e.g. headers, cookies, body.   Pretty-prints the body if content-type is either either XML, JSON or HTML..
     *
     * @return The specification
     */
    ValidatableResponse all();

    /**
     * Logs everything in the response, including e.g. headers, cookies, body with the option to pretty-print the body if the content-type is
     * either XML, JSON or HTML..
     *
     * @param shouldPrettyPrint <code>true</code> if the body should be pretty-printed, <code>false</code> otherwise.
     * @return The specification
     */
    ValidatableResponse all(boolean shouldPrettyPrint);

    /**
     * Logs everything in the response, including e.g. headers, cookies, body. Pretty-prints the body if content-type is either either XML, JSON or HTML..
     *
     * @return The specification
     */
    ValidatableResponse everything();

    /**
     * * Logs everything in the response, including e.g. headers, cookies, body with the option to pretty-print the body if the content-type is
     * either XML, JSON or HTML..
     *
     * @param shouldPrettyPrint <code>true</code> if the body should be pretty-printed, <code>false</code> otherwise.
     * @return The specification
     */
    ValidatableResponse everything(boolean shouldPrettyPrint);

    /**
     * Logs only the headers.
     *
     * @return The specification
     */
    ValidatableResponse headers();

    /**
     * Logs only the cookies.
     *
     * @return The specification
     */
    ValidatableResponse cookies();
}
