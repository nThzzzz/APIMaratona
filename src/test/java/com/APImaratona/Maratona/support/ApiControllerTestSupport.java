package com.APImaratona.Maratona.support;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base para testes de controller que, alem de executar a chamada via MockMvc, registra a
 * requisicao/resposta completa para o relatorio HTML gerado ao final da suite (ApiCallRecorder).
 */
public abstract class ApiControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected abstract String nomeControlador();

    protected MvcResult chamar(String cenario, MockHttpServletRequestBuilder requisicao) throws Exception {
        long inicio = System.currentTimeMillis();
        MvcResult resultado = mockMvc.perform(requisicao).andReturn();
        long duracao = System.currentTimeMillis() - inicio;

        MockHttpServletRequest req = resultado.getRequest();
        MockHttpServletResponse resp = resultado.getResponse();

        Map<String, String> requestHeaders = new LinkedHashMap<>();
        Collections.list(req.getHeaderNames()).forEach(h -> requestHeaders.put(h, req.getHeader(h)));

        Map<String, String> responseHeaders = new LinkedHashMap<>();
        resp.getHeaderNames().forEach(h -> responseHeaders.put(h, resp.getHeader(h)));

        String url = req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
        byte[] corpoRequisicao = req.getContentAsByteArray();
        String requestBody = corpoRequisicao == null ? "" : new String(corpoRequisicao, java.nio.charset.StandardCharsets.UTF_8);
        String responseBody = resp.getContentAsString();

        ApiCallRecorder.registrar(new ApiCallRecord(
                nomeControlador(), cenario, req.getMethod(), url,
                requestHeaders, requestBody, resp.getStatus(), responseHeaders, responseBody,
                duracao, Instant.now()
        ));

        return resultado;
    }

    protected String json(Object objeto) throws Exception {
        return objectMapper.writeValueAsString(objeto);
    }
}
