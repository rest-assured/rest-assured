package com.jayway.restassured.config;

import org.apache.commons.lang3.Validate;

import static com.jayway.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import static com.jayway.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL;

/**
 * Allows you to configure properties of JSON parsing.
 */
public class JsonConfig {
    private final NumberReturnType numberReturnType;

    /**
     * Create a new instance of XmlConfig without any features and that is namespace unaware.
     */
    public JsonConfig() {
        this(NumberReturnType.FLOAT_AND_DOUBLE);
    }

    public JsonConfig(NumberReturnType numberReturnType) {
        Validate.notNull(numberReturnType, "numberReturnType cannot be null");
        this.numberReturnType = numberReturnType;
    }

    public NumberReturnType numberReturnType() {
        return numberReturnType;
    }

    public boolean shouldRepresentJsonNumbersAsBigDecimal() {
        return numberReturnType() == BIG_DECIMAL;
    }

    /**
     * Specifies if JSON parsing should use floats and doubles or BigDecimals to represent Json numbers.
     *
     * @param numberReturnType The choice.
     * @return A new instance of JsonConfig with the given configuration
     */
    public JsonConfig numberReturnType(NumberReturnType numberReturnType) {
        return new JsonConfig(numberReturnType);
    }

    /**
     * @return A static way to create a new JsonConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static JsonConfig jsonConfig() {
        return new JsonConfig();
    }
}

