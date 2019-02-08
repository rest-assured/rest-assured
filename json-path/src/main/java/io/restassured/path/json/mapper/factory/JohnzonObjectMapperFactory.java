package io.restassured.path.json.mapper.factory;

import org.apache.johnzon.mapper.Mapper;

import io.restassured.common.mapper.factory.ObjectMapperFactory;

/**
 * Interface for Johnzon object mappers. Implement this class and register it to the ObjectMapperConfig if you
 * want to override default settings for the Gson object mapper.
 */
public interface JohnzonObjectMapperFactory extends ObjectMapperFactory<Mapper> {
}
