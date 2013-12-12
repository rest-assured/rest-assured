package com.jayway.restassured.module.mockmvc;

import com.jayway.restassured.module.mockmvc.http.GreetingController;
import com.jayway.restassured.module.mockmvc.http.PostController;
import com.jayway.restassured.path.json.JsonPath;
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
