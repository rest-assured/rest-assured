package com.jayway.restassured.internal.mapping

import org.codehaus.jackson.map.ObjectMapper


class ObjectMapperFactory {
	public ObjectMapper createJacksonObjectMapper(Class cls) {
		return new ObjectMapper();
	}
}
