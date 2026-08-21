package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Usuario.LoginResponse;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioRequest;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioResponse;
import com.APImaratona.Maratona.Exceptions.AutenticacaoInvalidaException;
import com.APImaratona.Maratona.Exceptions.EntidadeNaoEcontrada;
import com.APImaratona.Maratona.Exceptions.RegraDeNegocio;
import com.APImaratona.Maratona.Seguranca.JwtService;
import org.springframework.security.web.context.SecurityContextRepository;
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

/**
 * Slice do ControllerUsuario com o UsuarioService mockado: cobre o contrato HTTP (rota, corpo,
 * status) e a traducao de excecao -> status feita pelo GlobalExceptionHandler.
 *
 * addFilters=false desliga a cadeia de seguranca, entao quem cobre "sem token = 401" e o
 * ControllerUsuarioSecurityTest. Aqui o .principal(...) simula o usuario ja autenticado, o que
 * permite testar o caso "token valido, mas de OUTRA conta" -> 401 vindo do service.
 */
@WebMvcTest(ControllerUsuario.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestCacheConfig.class)
class ControllerUsuarioTest extends ApiControllerTestSupport {

    private static final UsernamePasswordAuthenticationToken FULANO =
            new UsernamePasswordAuthenticationToken("fulano", null);
    private static final UsernamePasswordAuthenticationToken OUTRA_PESSOA =
            new UsernamePasswordAuthenticationToken("outraPessoa", null);

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private CodeforcesService codeforcesService;

    // JwtAuthenticationFilter e um Filter (@Component) e por isso e escaneado pelo @WebMvcTest
    // mesmo com addFilters=false; sem esse mock o contexto nao sobe por falta de JwtService.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private SecurityContextRepository securityContextRepository;

    @Override
    protected String nomeControlador() {
        return "ControllerUsuario";
    }

    private UsuarioRequest.CadastrarUsuario cadastroValido() {
        return new UsuarioRequest.CadastrarUsuario("Fulano", "fulano@teste.com", "senha123", "fulano", null);
    }

    // ------------------------------ Cadastro ------------------------------

    @Test
    @DisplayName("POST /cadastro com dados validos cadastra e dispara sync com Codeforces")
    void cadastroUsuarioSucesso() throws Exception {
        doNothing().when(usuarioService).cadastrarUsuario(any());
        doNothing().when(codeforcesService).sincronizarPerfilCodeforces(anyString());

        MvcResult resultado = chamar("Cadastro com sucesso", post("/cadastro")
                .contentType(APPLICATION_JSON)
                .content(json(cadastroValido())));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).isEqualTo("Usuario cadastrado com sucesso");
        verify(usuarioService).cadastrarUsuario(any());
        verify(codeforcesService).sincronizarPerfilCodeforces("fulano");
    }

    @Test
    @DisplayName("POST /cadastro com email ja cadastrado retorna 400")
    void cadastroUsuarioEmailDuplicado() throws Exception {
        doThrow(new RegraDeNegocio("Usuário já cadastrado")).when(usuarioService).cadastrarUsuario(any());

        MvcResult resultado = chamar("Email ja cadastrado", post("/cadastro")
                .contentType(APPLICATION_JSON)
                .content(json(cadastroValido())));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString()).contains("Usuário já cadastrado");
    }

    @Test
    @DisplayName("POST /cadastro com time inexistente retorna 404")
    void cadastroUsuarioTimeInexistente() throws Exception {
        var dto = new UsuarioRequest.CadastrarUsuario("Fulano", "fulano@teste.com", "senha123", "fulano", "TimeFantasma");
        doThrow(new EntidadeNaoEcontrada("Time não encontrado")).when(usuarioService).cadastrarUsuario(any());

        MvcResult resultado = chamar("Time inexistente", post("/cadastro")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("POST /cadastro com senha em branco e barrado pelo @Valid antes do service")
    void cadastroUsuarioSenhaEmBranco() throws Exception {
        var dto = new UsuarioRequest.CadastrarUsuario("Fulano", "fulano@teste.com", "  ", "fulano", null);

        MvcResult resultado = chamar("Senha em branco", post("/cadastro")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString()).contains("Senha nulo");
    }

    // ------------------------------ Consultas -----------------------------

    @Test
    @DisplayName("GET /listaUsuarios retorna a lista de usuarios cadastrados")
    void listaUsuarios() throws Exception {
        UsuarioResponse usuario = new UsuarioResponse();
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
    @DisplayName("GET /buscarUsuario/{nomeUsuario} retorna o usuario correspondente")
    void buscarUsuarioPorNome() throws Exception {
        UsuarioResponse usuario = new UsuarioResponse();
        usuario.setNomeUsuario("fulano");
        when(usuarioService.buscarUsuarioNome("fulano")).thenReturn(usuario);

        MvcResult resultado = chamar("Busca por nomeUsuario", get("/buscarUsuario/fulano"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("fulano");
    }

    @Test
    @DisplayName("GET /buscarUsuario/{nomeUsuario} nao trunca nome de usuario com ponto")
    void buscarUsuarioComPontoNoNome() throws Exception {
        // Handles do Codeforces costumam ter ponto (ex: arthurb.zanvetor). Com o
        // PathPatternParser do Spring 6 nao ha mais suffix pattern matching, mas o teste
        // fixa isso: o service tem que receber o nome inteiro, nao "arthurb".
        UsuarioResponse usuario = new UsuarioResponse();
        usuario.setNomeUsuario("arthurb.zanvetor");
        when(usuarioService.buscarUsuarioNome("arthurb.zanvetor")).thenReturn(usuario);

        MvcResult resultado = chamar("Nome de usuario com ponto", get("/buscarUsuario/arthurb.zanvetor"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        verify(usuarioService).buscarUsuarioNome("arthurb.zanvetor");
    }

    @Test
    @DisplayName("GET /buscarUsuario/{nomeUsuario} para usuario inexistente retorna 404")
    void buscarUsuarioInexistente() throws Exception {
        when(usuarioService.buscarUsuarioNome("fantasma")).thenThrow(new EntidadeNaoEcontrada("Usuario nao encontrado"));

        MvcResult resultado = chamar("Usuario inexistente", get("/buscarUsuario/fantasma"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
    }

    // ------------------------------- Edicao -------------------------------

    @Test
    @DisplayName("PUT /editarUsuario/perfil/{nomeUsuario}/nome com sucesso retorna 200")
    void editarPerfilNomeSucesso() throws Exception {
        var dto = new UsuarioRequest.AlterarNome("Fulano Editado");
        doNothing().when(usuarioService).editarPerfilNome(eq("fulano"), any(), eq("fulano"));

        MvcResult resultado = chamar("Edicao de nome (autenticado como fulano)", put("/editarUsuario/perfil/fulano/nome")
                .principal(FULANO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("nome alterado com sucesso");
    }

    @Test
    @DisplayName("PUT /editarUsuario/credenciais/{nomeUsuario}/nomeUsuario devolve um token novo")
    void editarNomeUsuarioDevolveTokenNovo() throws Exception {
        var dto = new UsuarioRequest.AlterarNomeUsuario("fulano2", "senha123");
        when(usuarioService.editarNomeUsuario(eq("fulano"), any(), eq("fulano")))
                .thenReturn(new LoginResponse("token-novo-456", "Bearer"));

        MvcResult resultado = chamar("Troca de username emite token novo", put("/editarUsuario/credenciais/fulano/nomeUsuario")
                .principal(FULANO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("token-novo-456", "Bearer");
    }

    @Test
    @DisplayName("PUT /editarUsuario/credenciais/{nomeUsuario}/senha com senha atual incorreta retorna 400")
    void editarSenhaComSenhaAtualErrada() throws Exception {
        var dto = new UsuarioRequest.AlterarSenha("errada", "senhaNova456");
        doThrow(new RegraDeNegocio("Senha incorreta"))
                .when(usuarioService).editarSenhaUsuario(eq("fulano"), any(), eq("fulano"));

        MvcResult resultado = chamar("Senha atual incorreta", put("/editarUsuario/credenciais/fulano/senha")
                .principal(FULANO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentAsString()).contains("Senha incorreta");
    }

    @Test
    @DisplayName("PUT /editarUsuario/credenciais/{nomeUsuario}/email com token de outro usuario retorna 401")
    void editarEmailComTokenDeOutroUsuario() throws Exception {
        var dto = new UsuarioRequest.AlterarEmail("novo@teste.com", "senha123");
        doThrow(new AutenticacaoInvalidaException("Token não corresponde a este usuário"))
                .when(usuarioService).editarEmailUsuario(eq("fulano"), any(), eq("outraPessoa"));

        MvcResult resultado = chamar("Token de outro usuario tentando editar fulano", put("/editarUsuario/credenciais/fulano/email")
                .principal(OUTRA_PESSOA)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(resultado.getResponse().getContentAsString()).contains("Token não corresponde a este usuário");
    }

    // ------------------------------ Exclusao ------------------------------

    @Test
    @DisplayName("DELETE /excluirUsuario/{nomeUsuario}/nomeUsuario com sucesso retorna 200")
    void excluirUsuarioPorNomeUsuarioSucesso() throws Exception {
        var dto = new UsuarioRequest.ExcluirUsuarioNomeUsuario("senha123");
        doNothing().when(usuarioService).excluirUsuarioNomeUsuario(eq("fulano"), any(), eq("fulano"));

        MvcResult resultado = chamar("Exclusao com sucesso", delete("/excluirUsuario/fulano/nomeUsuario")
                .principal(FULANO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("deletado com sucesso");
    }

    @Test
    @DisplayName("DELETE /excluirUsuario/{nomeUsuario}/email com senha incorreta retorna 400")
    void excluirUsuarioPorEmailSenhaErrada() throws Exception {
        var dto = new UsuarioRequest.ExcluirUsuarioEmail("fulano@teste.com", "errada");
        doThrow(new RegraDeNegocio("Senha incorreta"))
                .when(usuarioService).excluirUsuarioEmail(eq("fulano"), any(), eq("fulano"));

        MvcResult resultado = chamar("Senha incorreta", delete("/excluirUsuario/fulano/email")
                .principal(FULANO)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("DELETE /excluirUsuario/{nomeUsuario}/nomeUsuario com token de outro usuario retorna 401")
    void excluirUsuarioComTokenDeOutroUsuario() throws Exception {
        var dto = new UsuarioRequest.ExcluirUsuarioNomeUsuario("senha123");
        doThrow(new AutenticacaoInvalidaException("Token não corresponde a este usuário"))
                .when(usuarioService).excluirUsuarioNomeUsuario(eq("fulano"), any(), eq("outraPessoa"));

        MvcResult resultado = chamar("Token de outro usuario tentando excluir fulano", delete("/excluirUsuario/fulano/nomeUsuario")
                .principal(OUTRA_PESSOA)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }
}
