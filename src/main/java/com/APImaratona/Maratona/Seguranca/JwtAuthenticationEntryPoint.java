package com.APImaratona.Maratona.Seguranca;

import com.APImaratona.Maratona.DTO.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

// Responde 401 em JSON quando um request sem token (ou com token invalido) tenta
// acessar uma rota protegida. Esse e o unico ponto que intercepta ANTES de chegar
// no controller, entao nao passa pelo GlobalExceptionHandler -- por isso monta o
// ErrorResponseDTO manualmente aqui, mas seguindo o mesmo formato de erro do resto da API.
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        ErrorResponseDTO erro = new ErrorResponseDTO(
                LocalDateTime.now(),
                401,
                "Não autenticado",
                "Token JWT ausente, inválido ou expirado"
        );

        response.getWriter().write(objectMapper.writeValueAsString(erro));
    }
}
