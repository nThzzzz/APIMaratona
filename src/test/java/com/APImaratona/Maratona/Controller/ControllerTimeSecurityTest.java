package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.Configuracao.SecurityConfig;
import com.APImaratona.Maratona.DTO.Time.CriarTimeRequest;
import com.APImaratona.Maratona.DTO.Time.TimeRequest;
import com.APImaratona.Maratona.Seguranca.JwtAuthenticationEntryPoint;
import com.APImaratona.Maratona.Seguranca.JwtService;
import com.APImaratona.Maratona.Services.TimeService;
import com.APImaratona.Maratona.support.ApiControllerTestSupport;
import com.APImaratona.Maratona.support.TestCacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Equivalente do ControllerUsuarioSecurityTest para as rotas de time: sobe a cadeia REAL do
 * Spring Security para conferir que cada rota de escrita esta de fato coberta por um matcher
 * do SecurityConfig.
 *
 * Vale para todas, mas especialmente para /editarTime/**: um matcher sem o /** nao casa com
 * rotas que tem segmentos depois do prefixo, a rota cai no anyRequest().permitAll() e o
 * Authentication chega null no controller -- resultando em 500 em vez de 401. Foi exatamente
 * isso que aconteceu com /excluirUsuario.
 */
@WebMvcTest(ControllerTime.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationEntryPoint.class, TestCacheConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=zWvbngQl1NZ/rCawtYmhf+a+toMdWLAGEOGrgZ9QWYE=",
        "jwt.expiracao-ms=3600000"
})
class ControllerTimeSecurityTest extends ApiControllerTestSupport {

    @MockitoBean
    private TimeService timeService;

    @Autowired
    private JwtService jwtService;

    @Override
    protected String nomeControlador() {
        return "ControllerTime (seguranca real)";
    }

    private void esperaNaoAutenticado(MvcResult resultado) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(resultado.getResponse().getContentAsString()).contains("Token JWT ausente, inválido ou expirado");
    }

    @Test
    @DisplayName("PUT /editarTime/{nomeTime}/nome sem token retorna 401 no filtro")
    void renomearSemToken() throws Exception {
        esperaNaoAutenticado(chamar("Sem header Authorization", put("/editarTime/Timaco/nome")
                .contentType(APPLICATION_JSON)
                .content(json(new TimeRequest.AlterarNomeTime("Timacao")))));
    }

    @Test
    @DisplayName("PUT /editarTime/{nomeTime}/capitao sem token retorna 401 no filtro")
    void transferirCapitaniaSemToken() throws Exception {
        esperaNaoAutenticado(chamar("Sem header Authorization", put("/editarTime/Timaco/capitao")
                .contentType(APPLICATION_JSON)
                .content(json(new TimeRequest.TransferirCapitania("sicrano")))));
    }

    @Test
    @DisplayName("POST /cadastroTime sem token retorna 401 no filtro")
    void cadastrarSemToken() throws Exception {
        esperaNaoAutenticado(chamar("Sem header Authorization", post("/cadastroTime")
                .contentType(APPLICATION_JSON)
                .content(json(new CriarTimeRequest("Timaco", List.of("fulano"))))));
    }

    @Test
    @DisplayName("PUT /adicionarUsuario sem token retorna 401 no filtro")
    void adicionarSemToken() throws Exception {
        esperaNaoAutenticado(chamar("Sem header Authorization", put("/adicionarUsuario")
                .contentType(APPLICATION_JSON)
                .content(json(new CriarTimeRequest("Timaco", List.of("sicrano"))))));
    }

    @Test
    @DisplayName("PUT /removerUsuario sem token retorna 401 no filtro")
    void removerSemToken() throws Exception {
        esperaNaoAutenticado(chamar("Sem header Authorization", put("/removerUsuario")
                .contentType(APPLICATION_JSON)
                .content(json(new CriarTimeRequest("Timaco", List.of("sicrano"))))));
    }

    @Test
    @DisplayName("DELETE /excluirTime sem token retorna 401 no filtro")
    void excluirSemToken() throws Exception {
        esperaNaoAutenticado(chamar("Sem header Authorization", delete("/excluirTime").param("nome", "Timaco")));
    }

    @Test
    @DisplayName("PUT /editarTime/{nomeTime}/nome com token valido chega no controller")
    void renomearComTokenValido() throws Exception {
        String token = jwtService.gerarToken("fulano");

        MvcResult resultado = chamar("Token valido emitido pelo JwtService real", put("/editarTime/Timaco/nome")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(json(new TimeRequest.AlterarNomeTime("Timacao"))));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("nome alterado com sucesso");
    }

    @Test
    @DisplayName("GET /listarTimes continua publico com a cadeia de seguranca real ativa")
    void listarTimesContinuaPublico() throws Exception {
        when(timeService.listarTimes()).thenReturn(List.of());

        MvcResult resultado = chamar("Endpoint publico sem token", get("/listarTimes"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }
}
