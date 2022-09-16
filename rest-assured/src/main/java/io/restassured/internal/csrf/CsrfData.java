package io.restassured.internal.csrf;

import io.restassured.config.CsrfConfig.CsrfPrioritization;

public class CsrfData {
    public final String inputFieldOrHeaderName;
    public final String token;
    public final CsrfPrioritization csrfPrioritization;

    public CsrfData(String inputFieldOrHeaderName, String token, CsrfPrioritization csrfPrioritization) {
        this.inputFieldOrHeaderName = inputFieldOrHeaderName;
        this.token = token;
        this.csrfPrioritization = csrfPrioritization;
    }

    public boolean shouldSendTokenAs(CsrfPrioritization csrfPrioritization) {
        return this.csrfPrioritization == csrfPrioritization;
    }
}