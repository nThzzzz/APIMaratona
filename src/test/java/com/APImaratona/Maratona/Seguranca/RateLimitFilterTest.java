package com.APImaratona.Maratona.Seguranca;

import com.APImaratona.Maratona.Configuracao.SecurityConfig;
import com.APImaratona.Maratona.Controller.ControllerAuth;
import com.APImaratona.Maratona.DTO.Usuario.LoginResponse;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioRequest;
import com.APImaratona.Maratona.Services.AuthService;
import com.APImaratona.Maratona.support.ApiControllerTestSupport;
import com.APImaratona.Maratona.support.TestCacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Sobe a cadeia real de seguranca com um limite artificialmente baixo (3 por janela)
 * para exercitar o RateLimitFilter sem precisar disparar as 30 requisicoes do padrao.
 */
@WebMvcTest(ControllerAuth.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationEntryPoint.class,
         RateLimitFilter.class, TestCacheConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=zWvbngQl1NZ/rCawtYmhf+a+toMdWLAGEOGrgZ9QWYE=",
        "jwt.expiracao-ms=3600000",
        "rate-limit.login=3",
        "rate-limit.cadastro=3",
        "rate-limit.janela-segundos=60"
})
class RateLimitFilterTest extends ApiControllerTestSupport {

    @MockitoBean
    private AuthService authService;

    @Override
    protected String nomeControlador() {
        return "RateLimitFilter";
    }

    @Test
    @DisplayName("POST /auth/login passa ate o limite e depois responde 429 sem chegar no service")
    void loginBloqueiaAcimaDoLimite() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("token-fake", "Bearer"));
        var dto = new UsuarioRequest.Login("fulano", "senha123");

        for (int i = 1; i <= 3; i++) {
            MvcResult ok = chamar("Requisicao " + i + " dentro do limite", post("/auth/login")
                    .contentType(APPLICATION_JSON)
                    .content(json(dto)));

            assertThat(ok.getResponse().getStatus()).isEqualTo(200);
        }

        MvcResult bloqueado = chamar("Quarta requisicao, acima do limite", post("/auth/login")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(bloqueado.getResponse().getStatus()).isEqualTo(429);
        // Mesmo formato de erro do resto da API.
        assertThat(bloqueado.getResponse().getContentAsString())
                .contains("Muitas requisições")
                .contains("Limite de requisições excedido");

        // O ponto de barrar no filtro e nao gastar BCrypt nem banco: o service
        // continua com as 3 chamadas que passaram, nao 4.
        verify(authService, times(3)).login(any());
    }

    @Test
    @DisplayName("Rota de leitura nao entra no rate limit")
    void rotaDeLeituraNaoEhLimitada() throws Exception {
        // O mesmo IP, muito acima do limite configurado, numa rota fora da lista.
        // 401 e o esperado aqui: /listaUsuarios nao existe neste slice (so o ControllerAuth
        // foi carregado), entao cai no deny by default -- o que importa e nunca virar 429.
        for (int i = 0; i < 10; i++) {
            MvcResult resultado = chamar("Leitura repetida " + i, get("/listaUsuarios"));

            assertThat(resultado.getResponse().getStatus()).isNotEqualTo(429);
        }
    }
}
