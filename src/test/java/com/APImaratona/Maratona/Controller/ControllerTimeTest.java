package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Time.CriarTimeRequest;
import com.APImaratona.Maratona.DTO.Time.TimeRequest;
import com.APImaratona.Maratona.DTO.Time.TimeResponse;
import com.APImaratona.Maratona.Exceptions.EntidadeNaoEcontrada;
import com.APImaratona.Maratona.Exceptions.RegraDeNegocio;
import com.APImaratona.Maratona.Seguranca.JwtService;
import org.springframework.security.web.context.SecurityContextRepository;
import com.APImaratona.Maratona.Services.TimeService;
import com.APImaratona.Maratona.support.ApiControllerTestSupport;
import com.APImaratona.Maratona.support.TestCacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Slice do ControllerTime com o TimeService mockado. addFilters=false desliga a cadeia de
 * seguranca, entao as rotas que recebem Authentication precisam do .principal(...) na
 * requisicao -- e esse nome que o controller repassa ao service como "capitao".
 */
@WebMvcTest(ControllerTime.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestCacheConfig.class)
class ControllerTimeTest extends ApiControllerTestSupport {

    private static final UsernamePasswordAuthenticationToken CAPITAO =
            new UsernamePasswordAuthenticationToken("fulano", null);

    @MockitoBean
    private TimeService timeService;

    // JwtAuthenticationFilter e um Filter (@Component) e por isso e escaneado pelo @WebMvcTest
    // mesmo com addFilters=false; sem esse mock o contexto nao sobe por falta de JwtService.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private SecurityContextRepository securityContextRepository;

    @Override
    protected String nomeControlador() {
        return "ControllerTime";
    }

    @Test
    @DisplayName("POST /cadastroTime com sucesso retorna 200 e repassa o capitao do token")
    void cadastroTimeSucesso() throws Exception {
        CriarTimeRequest dto = new CriarTimeRequest("Timaco", List.of("fulano"));
        doNothing().when(timeService).cadastrarTime(any(), eq("fulano"));

        MvcResult resultado = chamar("Cadastro com sucesso", post("/cadastroTime")
                .principal(CAPITAO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("cadastrado com sucesso");
        verify(timeService).cadastrarTime(any(), eq("fulano"));
    }

    @Test
    @DisplayName("POST /cadastroTime com nome duplicado retorna 400")
    void cadastroTimeNomeDuplicado() throws Exception {
        CriarTimeRequest dto = new CriarTimeRequest("Timaco", List.of("fulano"));
        doThrow(new RegraDeNegocio("Nome de time ja utilizado")).when(timeService).cadastrarTime(any(), eq("fulano"));

        MvcResult resultado = chamar("Nome de time duplicado", post("/cadastroTime")
                .principal(CAPITAO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("GET /listarTimes retorna a lista de times")
    void listarTimes() throws Exception {
        TimeResponse time = new TimeResponse("Timaco", List.of());
        when(timeService.listarTimes()).thenReturn(List.of(time));

        MvcResult resultado = chamar("Lista com um time", get("/listarTimes"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("Timaco");
    }

    @Test
    @DisplayName("GET /buscarTime retorna o time buscado")
    void buscarTimeSucesso() throws Exception {
        TimeResponse time = new TimeResponse("Timaco", List.of());
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
        CriarTimeRequest dto = new CriarTimeRequest("Timaco", List.of("sicrano"));
        doNothing().when(timeService).adicionarUsuarioNoTime(any(), eq("fulano"));

        MvcResult resultado = chamar("Adicionar usuario com sucesso", put("/adicionarUsuario")
                .principal(CAPITAO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("PUT /adicionarUsuario quando o time ja tem 3 integrantes retorna 400")
    void adicionarUsuarioTimeCheio() throws Exception {
        CriarTimeRequest dto = new CriarTimeRequest("Timaco", List.of("sicrano"));
        doThrow(new RegraDeNegocio("Time: Timaco, tera mais de 3 integrantes"))
                .when(timeService).adicionarUsuarioNoTime(any(), eq("fulano"));

        MvcResult resultado = chamar("Time ja com 3 integrantes", put("/adicionarUsuario")
                .principal(CAPITAO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("PUT /adicionarUsuario por quem nao e o capitao retorna 400")
    void adicionarUsuarioSemSerCapitao() throws Exception {
        CriarTimeRequest dto = new CriarTimeRequest("Timaco", List.of("sicrano"));
        doThrow(new RegraDeNegocio("Usuario não é o capitão to time, não pode adicionar integrante ao time"))
                .when(timeService).adicionarUsuarioNoTime(any(), eq("intruso"));

        MvcResult resultado = chamar("Autenticado como alguem que nao e o capitao", put("/adicionarUsuario")
                .principal(new UsernamePasswordAuthenticationToken("intruso", null))
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString()).contains("não é o capitão");
    }

    @Test
    @DisplayName("PUT /removerUsuario com sucesso retorna 200")
    void removerUsuarioSucesso() throws Exception {
        CriarTimeRequest dto = new CriarTimeRequest("Timaco", List.of("sicrano"));
        doNothing().when(timeService).removerUsuarioNoTime(any(), eq("fulano"));

        MvcResult resultado = chamar("Remover usuario com sucesso", put("/removerUsuario")
                .principal(CAPITAO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("PUT /removerUsuario para usuario que nao esta no time retorna 400")
    void removerUsuarioNaoEstaNoTime() throws Exception {
        CriarTimeRequest dto = new CriarTimeRequest("Timaco", List.of("sicrano"));
        doThrow(new RegraDeNegocio("Usuario: Sicrano, nao esta no Time: Timaco"))
                .when(timeService).removerUsuarioNoTime(any(), eq("fulano"));

        MvcResult resultado = chamar("Usuario fora do time", put("/removerUsuario")
                .principal(CAPITAO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("PUT /editarTime/{nomeTime}/nome com sucesso retorna 200")
    void editarNomeTimeSucesso() throws Exception {
        var dto = new TimeRequest.AlterarNomeTime("Timacao");
        doNothing().when(timeService).editarNomeTime(eq("Timaco"), any(), eq("fulano"));

        MvcResult resultado = chamar("Renomear time como capitao", put("/editarTime/Timaco/nome")
                .principal(CAPITAO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("nome alterado com sucesso");
    }

    @Test
    @DisplayName("PUT /editarTime/{nomeTime}/nome por quem nao e o capitao retorna 400")
    void editarNomeTimeSemSerCapitao() throws Exception {
        var dto = new TimeRequest.AlterarNomeTime("Timacao");
        doThrow(new RegraDeNegocio("Usuario não é o capitão do time, não pode renomear o time"))
                .when(timeService).editarNomeTime(eq("Timaco"), any(), eq("intruso"));

        MvcResult resultado = chamar("Renomear sem ser capitao", put("/editarTime/Timaco/nome")
                .principal(new UsernamePasswordAuthenticationToken("intruso", null))
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString()).contains("não é o capitão");
    }

    @Test
    @DisplayName("PUT /editarTime/{nomeTime}/capitao com sucesso retorna 200")
    void transferirCapitaniaSucesso() throws Exception {
        var dto = new TimeRequest.TransferirCapitania("sicrano");
        doNothing().when(timeService).transferirCapitania(eq("Timaco"), any(), eq("fulano"));

        MvcResult resultado = chamar("Transferir capitania", put("/editarTime/Timaco/capitao")
                .principal(CAPITAO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("capitania transferida para: sicrano");
    }

    @Test
    @DisplayName("PUT /editarTime/{nomeTime}/capitao com nome em branco e barrado pelo @Valid")
    void transferirCapitaniaNomeEmBranco() throws Exception {
        var dto = new TimeRequest.TransferirCapitania("  ");

        MvcResult resultado = chamar("Novo capitao em branco", put("/editarTime/Timaco/capitao")
                .principal(CAPITAO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString()).contains("Nome do novo capitão nulo");
    }

    @Test
    @DisplayName("DELETE /excluirTime com sucesso retorna 200")
    void excluirTimeSucesso() throws Exception {
        doNothing().when(timeService).excluirTime("Timaco", "fulano");

        MvcResult resultado = chamar("Exclusao com sucesso", delete("/excluirTime")
                .principal(CAPITAO)
                .param("nome", "Timaco"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("DELETE /excluirTime para time inexistente retorna 404")
    void excluirTimeInexistente() throws Exception {
        doThrow(new EntidadeNaoEcontrada("Time: Fantasma, nao encontrado"))
                .when(timeService).excluirTime("Fantasma", "fulano");

        MvcResult resultado = chamar("Time inexistente", delete("/excluirTime")
                .principal(CAPITAO)
                .param("nome", "Fantasma"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
    }
}
