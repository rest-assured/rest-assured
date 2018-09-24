package io.restassured.module.spring.commons;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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

	public static byte[] toByteArray(File file) {
		ByteArrayOutputStream ous = null;
		InputStream ios = null;
		try {
			byte[] buffer = new byte[4096];
			ous = new ByteArrayOutputStream();
			ios = new FileInputStream(file);
			int read = 0;
			while ((read = ios.read(buffer)) != -1) {
				ous.write(buffer, 0, read);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (ous != null) {
					ous.close();
				}
				if (ios != null) {
					ios.close();
				}
			} catch (IOException ignored) {
			}
		}

		return ous.toByteArray();
	}
}
