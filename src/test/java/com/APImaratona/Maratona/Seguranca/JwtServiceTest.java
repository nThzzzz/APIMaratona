package com.APImaratona.Maratona.Seguranca;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste unitario puro (sem contexto Spring) -- JwtService usa @Value para injetar
 * jwt.secret/jwt.expiracao-ms, entao os campos sao setados via ReflectionTestUtils.
 */
class JwtServiceTest {

    private static final String SEGREDO_A = gerarSegredoBase64();
    private static final String SEGREDO_B = gerarSegredoBase64();

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = criarJwtService(SEGREDO_A, 3_600_000L);
    }

    @Test
    @DisplayName("gerarToken produz um token valido do qual da pra extrair o mesmo nomeUsuario")
    void gerarTokenEValidarCaminhoFeliz() {
        String token = jwtService.gerarToken("fulano");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(jwtService.tokenValido(token)).isTrue();
        assertThat(jwtService.extrairNomeUsuario(token)).isEqualTo("fulano");
    }

    @Test
    @DisplayName("tokenValido retorna false para uma string que nao e um JWT")
    void tokenValidoRejeitaTokenMalformado() {
        assertThat(jwtService.tokenValido("isso-nao-e-um-jwt")).isFalse();
    }

    @Test
    @DisplayName("tokenValido retorna false para token vazio")
    void tokenValidoRejeitaTokenVazio() {
        assertThat(jwtService.tokenValido("")).isFalse();
    }

    @Test
    @DisplayName("tokenValido retorna false para token expirado")
    void tokenValidoRejeitaTokenExpirado() {
        JwtService jwtServiceExpirado = criarJwtService(SEGREDO_A, -1000L);
        String tokenExpirado = jwtServiceExpirado.gerarToken("fulano");

        assertThat(jwtService.tokenValido(tokenExpirado)).isFalse();
    }

    @Test
    @DisplayName("tokenValido retorna false para token assinado com outro segredo")
    void tokenValidoRejeitaAssinaturaDeOutroSegredo() {
        JwtService outroServico = criarJwtService(SEGREDO_B, 3_600_000L);
        String tokenDeOutroSegredo = outroServico.gerarToken("fulano");

        assertThat(jwtService.tokenValido(tokenDeOutroSegredo)).isFalse();
    }

    private static JwtService criarJwtService(String secretBase64, long expiracaoMs) {
        JwtService servico = new JwtService();
        ReflectionTestUtils.setField(servico, "secretBase64", secretBase64);
        ReflectionTestUtils.setField(servico, "expiracaoMs", expiracaoMs);
        return servico;
    }

    private static String gerarSegredoBase64() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
