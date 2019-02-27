package io.restassured.path.json.mapper.factory;

import java.lang.reflect.Type;

import org.apache.johnzon.mapper.Mapper;
import org.apache.johnzon.mapper.MapperBuilder;

/**
 * Simply creates a new Mapper instance.
 */
public class DefaultJohnzonObjectMapperFactory implements JohnzonObjectMapperFactory {
	@Override
	public Mapper create(Type cls, String charset) {
	    return new MapperBuilder()
            .setAccessModeName("field")
            .setSupportHiddenAccess(true)
            .build();
	}
}
