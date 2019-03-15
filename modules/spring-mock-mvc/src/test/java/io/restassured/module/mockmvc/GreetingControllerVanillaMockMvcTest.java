/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.restassured.module.mockmvc;

import io.restassured.module.mockmvc.http.GreetingController;
import io.restassured.module.mockmvc.http.PostController;
import io.restassured.path.json.JsonPath;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;


public class GreetingControllerVanillaMockMvcTest {

    @Test public void
    mock_mvc_example_for_get_greeting_controller() throws Exception {
        MockMvc mockMvc = standaloneSetup(new GreetingController()).setMessageConverters(new MappingJackson2HttpMessageConverter()).build();

        String contentAsString = mockMvc.perform(get("/greeting?name={name}", "Johan").accept(APPLICATION_JSON)).andReturn().getResponse().getContentAsString();
        JsonPath jsonPath = new JsonPath(contentAsString);

        assertThat(jsonPath.getInt("id"), equalTo(1));
        assertThat(jsonPath.getString("content"), equalTo("Hello, Johan!"));
    }

    @Test public void
    mock_mvc_example_for_post_greeting_controller() throws Exception {
        MockMvc mockMvc = standaloneSetup(new PostController()).setMessageConverters(new MappingJackson2HttpMessageConverter()).build();

        String contentAsString = mockMvc.perform(post("/greetingPost").param("name", "Johan").contentType(MediaType.APPLICATION_FORM_URLENCODED)).andReturn().getResponse().getContentAsString();
        JsonPath jsonPath = new JsonPath(contentAsString);

        assertThat(jsonPath.getInt("id"), equalTo(1));
        assertThat(jsonPath.getString("content"), equalTo("Hello, Johan!"));
    }
}
