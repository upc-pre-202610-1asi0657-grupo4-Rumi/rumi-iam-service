package com.rumi.iam.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Map;


/**
 * Sends the requests of one scenario and keeps the last response, so that every step class can check it.
 */
public class ApiClient {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    private MockHttpServletResponse response;

    public ApiClient(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public void post(String path, Map<String, String> body) throws Exception {
        post(path, body, Map.of());
    }

    public void post(String path, Map<String, String> body, Map<String, String> headers) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        headers.forEach(request::header);
        response = mockMvc.perform(request).andReturn().getResponse();
    }

    public MockHttpServletResponse response() {
        return response;
    }

    public JsonNode body() throws Exception {
        return objectMapper.readTree(responseText());
    }

    public String responseText() throws Exception {
        return response.getContentAsString(StandardCharsets.UTF_8);
    }
}
