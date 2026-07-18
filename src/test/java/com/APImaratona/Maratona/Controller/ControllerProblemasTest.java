package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Usuario.UsuarioResponseDTO;
import com.APImaratona.Maratona.Exceptions.EntidadeNaoEcontrada;
import com.APImaratona.Maratona.Model.Problema;
import com.APImaratona.Maratona.Services.ProblemasService;
import com.APImaratona.Maratona.support.ApiControllerTestSupport;
import com.APImaratona.Maratona.support.TestCacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(ControllerProblemas.class)
@Import(TestCacheConfig.class)
class ControllerProblemasTest extends ApiControllerTestSupport {

    @MockitoBean
    private ProblemasService problemasService;

    @Override
    protected String nomeControlador() {
        return "ControllerProblemas";
    }

    @Test
    @DisplayName("GET /{idProblema} retorna o problema encontrado")
    void buscarProblemaExistente() throws Exception {
        Problema problema = new Problema("1500A", "Problema Exemplo", "<p>Desc</p>", List.of("math"), 1200);
        when(problemasService.buscarProblema("1500A")).thenReturn(problema);

        MvcResult resultado = chamar("Problema existente", get("/1500A"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("Problema Exemplo");
    }

    @Test
    @DisplayName("GET /{idProblema} inexistente retorna 200 com corpo vazio (sem tratamento de nao encontrado)")
    void buscarProblemaInexistente() throws Exception {
        when(problemasService.buscarProblema("9999Z")).thenReturn(null);

        MvcResult resultado = chamar("Problema inexistente", get("/9999Z"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).isBlank();
    }

    @Test
    @DisplayName("GET /listarProblemas retorna a lista de problemas cadastrados")
    void listarProblemas() throws Exception {
        Problema problema = new Problema("1500A", "Problema Exemplo", "<p>Desc</p>", List.of("math"), 1200);
        when(problemasService.listarProblemas()).thenReturn(List.of(problema));

        MvcResult resultado = chamar("Lista com um problema", get("/listarProblemas"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("1500A");
    }

    @Test
    @DisplayName("GET /usuariosFizeramProblema/{idProblema} retorna a lista de usuarios")
    void usuariosFizeramProblema() throws Exception {
        UsuarioResponseDTO usuario = new UsuarioResponseDTO();
        usuario.setNomeUsuario("fulano");
        when(problemasService.usuariosFizeramProblema("1500A")).thenReturn(List.of(usuario));

        MvcResult resultado = chamar("Lista de usuarios que resolveram", get("/usuariosFizeramProblema/1500A"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("fulano");
    }

    @Test
    @DisplayName("GET /usuariosFizeramProblema/{idProblema} para problema nao cadastrado retorna 404")
    void usuariosFizeramProblemaInexistente() throws Exception {
        when(problemasService.usuariosFizeramProblema("9999Z")).thenThrow(new EntidadeNaoEcontrada("Problema: 9999Z, não cadastrado"));

        MvcResult resultado = chamar("Problema nao cadastrado", get("/usuariosFizeramProblema/9999Z"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("GET /problemasFeitorPor/{nomeUsuario} retorna a lista de problemas resolvidos")
    void problemasFeitosPor() throws Exception {
        Problema problema = new Problema("1500A", "Problema Exemplo", "<p>Desc</p>", List.of("math"), 1200);
        when(problemasService.problemasFeitosPor("fulano")).thenReturn(List.of(problema));

        MvcResult resultado = chamar("Lista de problemas resolvidos", get("/problemasFeitorPor/fulano"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("GET /problemasFeitorPor/{nomeUsuario} para usuario inexistente retorna 404")
    void problemasFeitosPorUsuarioInexistente() throws Exception {
        when(problemasService.problemasFeitosPor("fantasma")).thenThrow(new EntidadeNaoEcontrada("Usuário: fantasma, não encontrado"));

        MvcResult resultado = chamar("Usuario inexistente", get("/problemasFeitorPor/fantasma"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("GET /recomendarProblemaSimilaridade/{nomeUsuario} retorna recomendacoes")
    void recomendarPorSimilaridade() throws Exception {
        Problema problema = new Problema("1500A", "Problema Exemplo", "<p>Desc</p>", List.of("math"), 1200);
        when(problemasService.recomendarPorSimilaridade("fulano")).thenReturn(List.of(problema));

        MvcResult resultado = chamar("Recomendacao por similaridade", get("/recomendarProblemaSimilaridade/fulano"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("GET /recomendarProblemaSimilaridade/{nomeUsuario} para usuario inexistente retorna 404")
    void recomendarPorSimilaridadeUsuarioInexistente() throws Exception {
        when(problemasService.recomendarPorSimilaridade("fantasma")).thenThrow(new EntidadeNaoEcontrada("Usuário: fantasma, não encontrado"));

        MvcResult resultado = chamar("Usuario inexistente", get("/recomendarProblemaSimilaridade/fantasma"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("GET /recomendarProblemaRating/{nomeUsuario} retorna recomendacoes")
    void recomendarPorRating() throws Exception {
        Problema problema = new Problema("1500A", "Problema Exemplo", "<p>Desc</p>", List.of("math"), 1200);
        when(problemasService.recomendarProblemasComBaseRating("fulano")).thenReturn(List.of(problema));

        MvcResult resultado = chamar("Recomendacao por rating", get("/recomendarProblemaRating/fulano"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("GET /recomendarProblemaRating/{nomeUsuario} para usuario inexistente retorna 404")
    void recomendarPorRatingUsuarioInexistente() throws Exception {
        when(problemasService.recomendarProblemasComBaseRating("fantasma")).thenThrow(new EntidadeNaoEcontrada("Usuário: fantasma, não encontrado"));

        MvcResult resultado = chamar("Usuario inexistente", get("/recomendarProblemaRating/fantasma"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
    }
}
