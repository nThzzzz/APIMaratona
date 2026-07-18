package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Usuario.EditarUsuarioRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Usuario.ExcluirUsuarioRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioResponseDTO;
import com.APImaratona.Maratona.Exceptions.AutenticacaoInvalidaException;
import com.APImaratona.Maratona.Exceptions.EntidadeNaoEcontrada;
import com.APImaratona.Maratona.Exceptions.RegraDeNegocio;
import com.APImaratona.Maratona.Seguranca.JwtService;
import com.APImaratona.Maratona.Services.CodeforcesService;
import com.APImaratona.Maratona.Services.UsuarioService;
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
import static org.mockito.ArgumentMatchers.anyString;
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

@WebMvcTest(ControllerUsuario.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestCacheConfig.class)
class ControllerUsuarioTest extends ApiControllerTestSupport {

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private CodeforcesService codeforcesService;

    // JwtAuthenticationFilter e um Filter (@Component) e por isso e escaneado pelo @WebMvcTest
    // mesmo com addFilters=false; sem esse mock o contexto no sobe por falta de JwtService.
    @MockitoBean
    private JwtService jwtService;

    @Override
    protected String nomeControlador() {
        return "ControllerUsuario";
    }

    @Test
    @DisplayName("GET /teste responde 200 com texto fixo")
    void teste() throws Exception {
        MvcResult resultado = chamar("Endpoint de teste basico", get("/teste"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).isEqualTo("teste");
    }

    @Test
    @DisplayName("POST /cadastro com dados validos cadastra e dispara sync com Codeforces")
    void cadastroUsuarioSucesso() throws Exception {
        UsuarioRequisicaoDTO dto = new UsuarioRequisicaoDTO("Fulano", "fulano@teste.com", "senha123", "fulano");

        doNothing().when(usuarioService).cadastrarUsuario(any());
        doNothing().when(codeforcesService).sincronizarPerfilCodeforces(anyString());

        MvcResult resultado = chamar("Cadastro com sucesso", post("/cadastro")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).isEqualTo("Usuario cadastrado com sucesso");
        verify(usuarioService).cadastrarUsuario(any());
        verify(codeforcesService).sincronizarPerfilCodeforces("fulano");
    }

    @Test
    @DisplayName("POST /cadastro com email ja cadastrado retorna 400")
    void cadastroUsuarioEmailDuplicado() throws Exception {
        UsuarioRequisicaoDTO dto = new UsuarioRequisicaoDTO("Fulano", "fulano@teste.com", "senha123", "fulano");
        doThrow(new RegraDeNegocio("Usuário já cadastrado")).when(usuarioService).cadastrarUsuario(any());

        MvcResult resultado = chamar("Email ja cadastrado", post("/cadastro")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString()).contains("Usuário já cadastrado");
    }

    @Test
    @DisplayName("POST /cadastro com time inexistente retorna 404")
    void cadastroUsuarioTimeInexistente() throws Exception {
        UsuarioRequisicaoDTO dto = new UsuarioRequisicaoDTO("Fulano", "fulano@teste.com", "senha123", "fulano");
        dto.setNomeTime("TimeFantasma");
        doThrow(new EntidadeNaoEcontrada("Time não encontrado")).when(usuarioService).cadastrarUsuario(any());

        MvcResult resultado = chamar("Time inexistente", post("/cadastro")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("GET /listaUsuarios retorna a lista de usuarios cadastrados")
    void listaUsuarios() throws Exception {
        UsuarioResponseDTO usuario = new UsuarioResponseDTO();
        usuario.setNome("Fulano");
        usuario.setNomeUsuario("fulano");
        usuario.setEmail("fulano@teste.com");
        usuario.setNomeTime("Sem time");
        usuario.setRank("newbie");
        usuario.setRating(0);

        when(usuarioService.listarUsuarios()).thenReturn(List.of(usuario));

        MvcResult resultado = chamar("Lista com um usuario", get("/listaUsuarios"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("fulano");
    }

    @Test
    @DisplayName("GET /buscarUsuario?nomeUsuario= retorna o usuario correspondente")
    void buscarUsuarioPorNome() throws Exception {
        UsuarioResponseDTO usuario = new UsuarioResponseDTO();
        usuario.setNomeUsuario("fulano");
        when(usuarioService.buscarUsuarioNome("fulano")).thenReturn(usuario);

        MvcResult resultado = chamar("Busca por nomeUsuario", get("/buscarUsuario").param("nomeUsuario", "fulano"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("GET /buscarUsuario?email= retorna o usuario correspondente")
    void buscarUsuarioPorEmail() throws Exception {
        UsuarioResponseDTO usuario = new UsuarioResponseDTO();
        usuario.setEmail("fulano@teste.com");
        when(usuarioService.buscarUsuarioEmail("fulano@teste.com")).thenReturn(usuario);

        MvcResult resultado = chamar("Busca por email", get("/buscarUsuario").param("email", "fulano@teste.com"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("GET /buscarUsuario sem parametros retorna 500 (RuntimeException nao mapeada)")
    void buscarUsuarioSemParametros() throws Exception {
        MvcResult resultado = chamar("Nenhum parametro informado", get("/buscarUsuario"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(500);
    }

    @Test
    @DisplayName("GET /buscarUsuario para usuario inexistente retorna 404")
    void buscarUsuarioInexistente() throws Exception {
        when(usuarioService.buscarUsuarioNome("fantasma")).thenThrow(new EntidadeNaoEcontrada("Usuario nao encontrado"));

        MvcResult resultado = chamar("Usuario inexistente", get("/buscarUsuario").param("nomeUsuario", "fantasma"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("PUT /editarUsuario/{nomeUsuario} com sucesso retorna 200")
    void editarUsuarioSucesso() throws Exception {
        EditarUsuarioRequisicaoDTO dto = new EditarUsuarioRequisicaoDTO();
        dto.setSenhaAntiga("senha123");
        dto.setNome("Fulano Editado");

        when(usuarioService.editarUsuario(eq("fulano"), any(), eq("fulano"))).thenReturn("| Nome |");

        MvcResult resultado = chamar("Edicao de nome com sucesso (autenticado como fulano)", put("/editarUsuario/fulano")
                .principal(new UsernamePasswordAuthenticationToken("fulano", null))
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("Nome");
    }

    @Test
    @DisplayName("PUT /editarUsuario/{nomeUsuario} com senha antiga incorreta retorna 400")
    void editarUsuarioSenhaErrada() throws Exception {
        EditarUsuarioRequisicaoDTO dto = new EditarUsuarioRequisicaoDTO();
        dto.setSenhaAntiga("errada");

        when(usuarioService.editarUsuario(eq("fulano"), any(), eq("fulano"))).thenThrow(new RegraDeNegocio("Senha incorreta"));

        MvcResult resultado = chamar("Senha antiga incorreta (autenticado como fulano)", put("/editarUsuario/fulano")
                .principal(new UsernamePasswordAuthenticationToken("fulano", null))
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("PUT /editarUsuario/{nomeUsuario} com token de outro usuario retorna 401")
    void editarUsuarioTokenNaoCorresponde() throws Exception {
        EditarUsuarioRequisicaoDTO dto = new EditarUsuarioRequisicaoDTO();
        dto.setSenhaAntiga("senha123");

        when(usuarioService.editarUsuario(eq("fulano"), any(), eq("outraPessoa")))
                .thenThrow(new AutenticacaoInvalidaException("Token não corresponde a este usuário"));

        MvcResult resultado = chamar("Token de outro usuario tentando editar fulano", put("/editarUsuario/fulano")
                .principal(new UsernamePasswordAuthenticationToken("outraPessoa", null))
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("DELETE /excluirUsuario com sucesso retorna 200")
    void excluirUsuarioSucesso() throws Exception {
        ExcluirUsuarioRequisicaoDTO dto = new ExcluirUsuarioRequisicaoDTO();
        dto.setNomeUsuario("fulano");
        dto.setSenha("senha123");

        doNothing().when(usuarioService).excluirUsuario(any(), eq("fulano"));

        MvcResult resultado = chamar("Exclusao com sucesso (autenticado como fulano)", delete("/excluirUsuario")
                .principal(new UsernamePasswordAuthenticationToken("fulano", null))
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("DELETE /excluirUsuario com senha incorreta retorna 400")
    void excluirUsuarioSenhaErrada() throws Exception {
        ExcluirUsuarioRequisicaoDTO dto = new ExcluirUsuarioRequisicaoDTO();
        dto.setNomeUsuario("fulano");
        dto.setSenha("errada");

        doThrow(new RegraDeNegocio("Senha incorreta")).when(usuarioService).excluirUsuario(any(), eq("fulano"));

        MvcResult resultado = chamar("Senha incorreta (autenticado como fulano)", delete("/excluirUsuario")
                .principal(new UsernamePasswordAuthenticationToken("fulano", null))
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("DELETE /excluirUsuario com token de outro usuario retorna 401")
    void excluirUsuarioTokenNaoCorresponde() throws Exception {
        ExcluirUsuarioRequisicaoDTO dto = new ExcluirUsuarioRequisicaoDTO();
        dto.setNomeUsuario("fulano");
        dto.setSenha("senha123");

        doThrow(new AutenticacaoInvalidaException("Token não corresponde a este usuário"))
                .when(usuarioService).excluirUsuario(any(), eq("outraPessoa"));

        MvcResult resultado = chamar("Token de outro usuario tentando excluir fulano", delete("/excluirUsuario")
                .principal(new UsernamePasswordAuthenticationToken("outraPessoa", null))
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }
}
