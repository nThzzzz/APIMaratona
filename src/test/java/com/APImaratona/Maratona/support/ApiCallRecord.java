package com.APImaratona.Maratona.support;

import java.time.Instant;
import java.util.Map;

public record ApiCallRecord(
        String controlador,
        String cenario,
        String metodo,
        String url,
        Map<String, String> requestHeaders,
        String requestBody,
        int status,
        Map<String, String> responseHeaders,
        String responseBody,
        long duracaoMs,
        Instant momento
) {
}
