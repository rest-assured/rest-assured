package io.restassured.internal.csrf;

public class CsrfInputField {
    public final String name;
    public final String value;

    public CsrfInputField(String name, String value) {
        this.name = name;
        this.value = value;
    }
}