package io.restassured.config;

public interface SerializationConfig extends Config {

	ObjectMapperConfig getObjectMapperConfig();

	EncoderConfig getEncoderConfig();
}
