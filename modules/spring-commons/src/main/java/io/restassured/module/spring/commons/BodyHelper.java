package io.restassured.module.spring.commons;

import io.restassured.http.Headers;
import io.restassured.internal.mapping.ObjectMapperSerializationContextImpl;
import io.restassured.internal.mapping.ObjectMapping;
import io.restassured.mapper.ObjectMapper;
import io.restassured.module.spring.commons.config.SpecificationConfig;

import static io.restassured.internal.serialization.SerializationSupport.isSerializableCandidate;

/**
 * @author Olga Maciaszek-Sharma
 */
public class BodyHelper {

	private BodyHelper() {
	}

	public static String toStringBody(Object object, SpecificationConfig config, Headers headers) {
		if (!isSerializableCandidate(object)) {
			return object.toString();
		}
		String requestContentType = HeaderHelper.getRequestContentType(headers);
		return ObjectMapping.serialize(object, requestContentType,
				Serializer.findEncoderCharsetOrReturnDefault(requestContentType, config), null,
				config.getObjectMapperConfig(), config.getEncoderConfig());
	}

	public static Object toSerializedBody(Object object, ObjectMapper objectMapper, SpecificationConfig config,
	                                      Headers headers) {
		String requestContentType = HeaderHelper.getRequestContentType(headers);
		ObjectMapperSerializationContextImpl ctx = new ObjectMapperSerializationContextImpl();
		ctx.setObject(object);
		ctx.setCharset(Serializer.findEncoderCharsetOrReturnDefault(requestContentType, config));
		ctx.setContentType(requestContentType);
		return objectMapper.serialize(ctx);
	}
}
