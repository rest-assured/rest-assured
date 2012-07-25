package com.jayway.restassured.internal.mapping;

public interface IMapperConfig<T>
{
	void configure(T mapper);
}
