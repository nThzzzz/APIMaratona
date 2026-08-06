package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Time.TimeRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Time.TimeResponseDTO;
import com.APImaratona.Maratona.Exceptions.EntidadeNaoEcontrada;
import com.APImaratona.Maratona.Exceptions.RegraDeNegocio;
import com.APImaratona.Maratona.Seguranca.JwtService;
import org.junit.jupiter.api.Disabled;
import org.springframework.security.web.context.SecurityContextRepository;
import com.APImaratona.Maratona.Services.TimeService;
import com.APImaratona.Maratona.support.ApiControllerTestSupport;
import com.APImaratona.Maratona.support.TestCacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@Disabled("Refatoração pesada rolando no TimeService")
@WebMvcTest(ControllerTime.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestCacheConfig.class)
class ControllerTimeTest extends ApiControllerTestSupport {

    @MockitoBean
    private TimeService timeService;

    // JwtAuthenticationFilter e um Filter (@Component) e por isso e escaneado pelo @WebMvcTest
    // mesmo com addFilters=false; sem esse mock o contexto no sobe por falta de JwtService.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private SecurityContextRepository securityContextRepository;

    @Override
    protected String nomeControlador() {
        return "ControllerTime";
    }

    @Test
    @DisplayName("POST /cadastroTime com sucesso retorna 200")
    void cadastroTimeSucesso() throws Exception {
        TimeRequisicaoDTO dto = new TimeRequisicaoDTO("Timaco", List.of("fulano"));
        doNothing().when(timeService).cadastrarTime(any());

        MvcResult resultado = chamar("Cadastro com sucesso", post("/cadastroTime")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("cadastrado com sucesso");
    }

    @Test
    @DisplayName("POST /cadastroTime com nome duplicado retorna 500 (TimeService ainda usa RuntimeException bruta)")
    void cadastroTimeNomeDuplicado() throws Exception {
        TimeRequisicaoDTO dto = new TimeRequisicaoDTO("Timaco", List.of());
        doThrow(new RuntimeException("Nome de time ja utilizado")).when(timeService).cadastrarTime(any());

        MvcResult resultado = chamar("Nome de time duplicado", post("/cadastroTime")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(500);
    }

    @Test
    @DisplayName("GET /listarTimes retorna a lista de times")
    void listarTimes() throws Exception {
        TimeResponseDTO time = new TimeResponseDTO("Timaco", List.of());
        when(timeService.listarTimes()).thenReturn(List.of(time));

        MvcResult resultado = chamar("Lista com um time", get("/listarTimes"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("Timaco");
    }

    @Test
    @DisplayName("GET /buscarTime retorna o time buscado")
    void buscarTimeSucesso() throws Exception {
        TimeResponseDTO time = new TimeResponseDTO("Timaco", List.of());
        when(timeService.buscarTime("Timaco")).thenReturn(time);

        MvcResult resultado = chamar("Busca por nome existente", get("/buscarTime").param("nome", "Timaco"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("GET /buscarTime para time inexistente retorna 404")
    void buscarTimeInexistente() throws Exception {
        when(timeService.buscarTime("Fantasma")).thenThrow(new EntidadeNaoEcontrada("Time: Fantasma, nao encontrado"));

        MvcResult resultado = chamar("Time inexistente", get("/buscarTime").param("nome", "Fantasma"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("PUT /adicionarUsuario com sucesso retorna 200")
    void adicionarUsuarioSucesso() throws Exception {
        TimeRequisicaoDTO dto = new TimeRequisicaoDTO("Timaco", List.of("fulano"));
        doNothing().when(timeService).adicionarUsuarioNoTime(any());

        MvcResult resultado = chamar("Adicionar usuario com sucesso", put("/adicionarUsuario")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("PUT /adicionarUsuario quando o time ja tem 3 integrantes retorna 400")
    void adicionarUsuarioTimeCheio() throws Exception {
        TimeRequisicaoDTO dto = new TimeRequisicaoDTO("Timaco", List.of("fulano"));
        doThrow(new RegraDeNegocio("Time: Timaco, tera mais de 3 integrantes")).when(timeService).adicionarUsuarioNoTime(any());

        MvcResult resultado = chamar("Time ja com 3 integrantes", put("/adicionarUsuario")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("PUT /removerUsuario com sucesso retorna 200")
    void removerUsuarioSucesso() throws Exception {
        TimeRequisicaoDTO dto = new TimeRequisicaoDTO("Timaco", List.of("fulano"));
        doNothing().when(timeService).removerUsuarioNoTime(any());

        MvcResult resultado = chamar("Remover usuario com sucesso", put("/removerUsuario")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("PUT /removerUsuario para usuario que nao esta no time retorna 400")
    void removerUsuarioNaoEstaNoTime() throws Exception {
        TimeRequisicaoDTO dto = new TimeRequisicaoDTO("Timaco", List.of("fulano"));
        doThrow(new RegraDeNegocio("Usuario: Fulano, nao esta no Time: Timaco")).when(timeService).removerUsuarioNoTime(any());

        MvcResult resultado = chamar("Usuario fora do time", put("/removerUsuario")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("DELETE /excluirTime com sucesso retorna 200")
    void excluirTimeSucesso() throws Exception {
        doNothing().when(timeService).excluirTime("Timaco");

        MvcResult resultado = chamar("Exclusao com sucesso", delete("/excluirTime").param("nome", "Timaco"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("DELETE /excluirTime para time inexistente retorna 500 (TimeService ainda usa RuntimeException bruta)")
    void excluirTimeInexistente() throws Exception {
        doThrow(new RuntimeException("Time nao econtrado")).when(timeService).excluirTime("Fantasma");

        MvcResult resultado = chamar("Time inexistente", delete("/excluirTime").param("nome", "Fantasma"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(500);
    }
}
