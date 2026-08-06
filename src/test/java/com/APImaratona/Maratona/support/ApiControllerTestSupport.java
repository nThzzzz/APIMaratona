package com.APImaratona.Maratona.support;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base para testes de controller para executar as chamadas via MockMvc.
 */
public abstract class ApiControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected abstract String nomeControlador();

    protected MvcResult chamar(String cenario, MockHttpServletRequestBuilder requisicao) throws Exception {
        return mockMvc.perform(requisicao).andReturn();
    }

    protected String json(Object objeto) throws Exception {
        return objectMapper.writeValueAsString(objeto);
    }
}