package io.restassured.module.spring.commons;

import io.restassured.config.EncoderConfig;
import io.restassured.config.SerializationConfig;
import io.restassured.internal.http.CharsetExtractor;
import io.restassured.internal.mapping.ObjectMapping;

import static io.restassured.internal.serialization.SerializationSupport.isSerializableCandidate;

/**
 * @author Olga Maciaszek-Sharma
 */
public class SpringClientSerializer {

	private final SerializationConfig serializationConfig;

	public SpringClientSerializer(SerializationConfig serializationConfig) {
		this.serializationConfig = serializationConfig;
	}

	public String serializeIfNeeded(Object object, String contentType) {
		return isSerializableCandidate(object) ? ObjectMapping.serialize(object, contentType,
				findEncoderCharsetOrReturnDefault(contentType), null, serializationConfig.getObjectMapperConfig(),
				serializationConfig.getEncoderConfig()) : object.toString();
	}

	public String findEncoderCharsetOrReturnDefault(String contentType) {
		String charset = CharsetExtractor.getCharsetFromContentType(contentType);
		if (charset == null) {
			EncoderConfig encoderConfig = serializationConfig.getEncoderConfig();
			if (encoderConfig.hasDefaultCharsetForContentType(contentType)) {
				charset = encoderConfig.defaultCharsetForContentType(contentType);
			} else {
				charset = encoderConfig.defaultContentCharset();
			}
		}
		return charset;
	}
}
