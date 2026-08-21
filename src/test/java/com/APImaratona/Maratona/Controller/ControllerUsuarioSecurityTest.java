package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.Configuracao.SecurityConfig;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioRequest;
import com.APImaratona.Maratona.Seguranca.JwtAuthenticationEntryPoint;
import com.APImaratona.Maratona.Seguranca.JwtService;
import com.APImaratona.Maratona.Services.CodeforcesService;
import com.APImaratona.Maratona.Services.UsuarioService;
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

import org.springframework.data.domain.Page;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Ao contrario dos demais testes de controller (addFilters=false + JwtService mockado, focados
 * no contrato controller/service), esta classe roda com a cadeia de seguranca REAL
 * (SecurityConfig, JwtAuthenticationFilter, JwtAuthenticationEntryPoint, JwtService de verdade)
 * para cobrir o que so existe no filtro: requisicao sem token, com token invalido e com token
 * valido emitido pelo proprio JwtService -- sem banco nenhum, so um jwt.secret de teste.
 *
 * O que os testes de /excluirUsuario protegem: o matcher do SecurityConfig precisa terminar em
 * /** para casar com as rotas reais (/excluirUsuario/{nomeUsuario}/email). Sem isso a rota cai
 * no anyRequest().permitAll() e nunca chega no JwtAuthenticationEntryPoint.
 */
@WebMvcTest(ControllerUsuario.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationEntryPoint.class, TestCacheConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=zWvbngQl1NZ/rCawtYmhf+a+toMdWLAGEOGrgZ9QWYE=",
        "jwt.expiracao-ms=3600000"
})
class ControllerUsuarioSecurityTest extends ApiControllerTestSupport {

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private CodeforcesService codeforcesService;

    @Autowired
    private JwtService jwtService;

    @Override
    protected String nomeControlador() {
        return "ControllerUsuario (seguranca real)";
    }

    @Test
    @DisplayName("PUT /editarUsuario/perfil/{nomeUsuario}/nome sem header Authorization retorna 401")
    void editarUsuarioSemToken() throws Exception {
        var dto = new UsuarioRequest.AlterarNome("Fulano Editado");

        MvcResult resultado = chamar("Sem header Authorization", put("/editarUsuario/perfil/fulano/nome")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(resultado.getResponse().getContentAsString()).contains("Token JWT ausente, inválido ou expirado");
    }

    @Test
    @DisplayName("PUT /editarUsuario/perfil/{nomeUsuario}/nome com token malformado retorna 401")
    void editarUsuarioTokenInvalido() throws Exception {
        var dto = new UsuarioRequest.AlterarNome("Fulano Editado");

        MvcResult resultado = chamar("Token malformado", put("/editarUsuario/perfil/fulano/nome")
                .header("Authorization", "Bearer token-completamente-invalido")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("DELETE /excluirUsuario/{nomeUsuario}/email sem token retorna 401 no filtro")
    void excluirUsuarioPorEmailSemToken() throws Exception {
        var dto = new UsuarioRequest.ExcluirUsuarioEmail("fulano@teste.com", "senha123");

        MvcResult resultado = chamar("Sem header Authorization", delete("/excluirUsuario/fulano/email")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(resultado.getResponse().getContentAsString()).contains("Token JWT ausente, inválido ou expirado");
    }

    @Test
    @DisplayName("DELETE /excluirUsuario/{nomeUsuario}/nomeUsuario sem token retorna 401 no filtro")
    void excluirUsuarioPorNomeUsuarioSemToken() throws Exception {
        var dto = new UsuarioRequest.ExcluirUsuarioNomeUsuario("senha123");

        MvcResult resultado = chamar("Sem header Authorization", delete("/excluirUsuario/fulano/nomeUsuario")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(resultado.getResponse().getContentAsString()).contains("Token JWT ausente, inválido ou expirado");
    }

    @Test
    @DisplayName("PUT /editarUsuario/perfil/{nomeUsuario}/nome com token valido chega no controller")
    void editarUsuarioComTokenValido() throws Exception {
        String token = jwtService.gerarToken("fulano");
        var dto = new UsuarioRequest.AlterarNome("Fulano Editado");

        MvcResult resultado = chamar("Token valido emitido pelo JwtService real", put("/editarUsuario/perfil/fulano/nome")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("nome alterado com sucesso");
    }

    @Test
    @DisplayName("DELETE /excluirUsuario/{nomeUsuario}/email com token valido chega no controller")
    void excluirUsuarioComTokenValido() throws Exception {
        String token = jwtService.gerarToken("fulano");
        var dto = new UsuarioRequest.ExcluirUsuarioEmail("fulano@teste.com", "senha123");

        MvcResult resultado = chamar("Token valido emitido pelo JwtService real", delete("/excluirUsuario/fulano/email")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("deletado com sucesso");
    }

    @Test
    @DisplayName("GET /listaUsuarios continua publico mesmo com a cadeia de seguranca real ativa")
    void listaUsuariosContinuaPublico() throws Exception {
        when(usuarioService.listarUsuarios(any())).thenReturn(Page.empty());

        MvcResult resultado = chamar("Endpoint publico sem token", get("/listaUsuarios"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Rota nao listada no SecurityConfig exige token (postura deny by default)")
    void rotaDesconhecidaExigeToken() throws Exception {
        // Trava a inversao da postura: com o antigo anyRequest().permitAll() um caminho
        // desconhecido passava pela seguranca e respondia 404. Agora para no filtro.
        MvcResult resultado = chamar("Caminho sem matcher explicito", get("/rotaQueNinguemClassificou"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }
}
