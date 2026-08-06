package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.Configuracao.SecurityConfig;
import com.APImaratona.Maratona.DTO.Usuario.EditarUsuarioCredenciaisRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Usuario.EditarUsuarioPerfilRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Usuario.ExcluirUsuarioRequisicaoDTO;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Ao contrario de ControllerUsuarioTest (addFilters=false + JwtService mockado, focado no
 * contrato do controller/service), esta classe roda com a cadeia de seguranca REAL
 * (SecurityConfig, JwtAuthenticationFilter, JwtAuthenticationEntryPoint, JwtService de
 * verdade) para cobrir o que so existe no filtro: requisicao sem token, com token invalido,
 * e com token valido de verdade emitido pelo proprio JwtService -- sem precisar de nenhum
 * banco real, so de um jwt.secret de teste via @TestPropertySource.
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
    @DisplayName("PUT /editarUsuario/{nomeUsuario} sem header Authorization retorna 401")
    void editarUsuarioSemToken() throws Exception {
        EditarUsuarioCredenciaisRequisicaoDTO dto = new EditarUsuarioCredenciaisRequisicaoDTO();
        dto.setSenhaAntiga("senha123");

        MvcResult resultado = chamar("Sem header Authorization", put("/editarUsuario/fulano")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(resultado.getResponse().getContentAsString()).contains("Token JWT ausente, inválido ou expirado");
    }

    @Test
    @DisplayName("PUT /editarUsuario/{nomeUsuario} com token malformado retorna 401")
    void editarUsuarioTokenInvalido() throws Exception {
        EditarUsuarioCredenciaisRequisicaoDTO dto = new EditarUsuarioCredenciaisRequisicaoDTO();
        dto.setSenhaAntiga("senha123");

        MvcResult resultado = chamar("Token malformado", put("/editarUsuario/fulano")
                .header("Authorization", "Bearer token-completamente-invalido")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("DELETE /excluirUsuario sem header Authorization retorna 401")
    void excluirUsuarioSemToken() throws Exception {
        ExcluirUsuarioRequisicaoDTO dto = new ExcluirUsuarioRequisicaoDTO();
        dto.setNomeUsuario("fulano");
        dto.setSenha("senha123");

        MvcResult resultado = chamar("Sem header Authorization", delete("/excluirUsuario")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("PUT /editarUsuario/perfil/{nomeUsuario} com token valido de verdade passa pelo filtro e chega no controller")
    void editarUsuarioComTokenValido() throws Exception {
        String token = jwtService.gerarToken("fulano");

        EditarUsuarioPerfilRequisicaoDTO dto = new EditarUsuarioPerfilRequisicaoDTO();
        dto.setNomeNovo("Fulano Editado");

        when(usuarioService.editarPerfil(eq("fulano"), any(), eq("fulano"))).thenReturn("| Nome |");

        MvcResult resultado = chamar("Token valido emitido pelo JwtService real", put("/editarUsuario/perfil/fulano")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("Nome");
    }

    @Test
    @DisplayName("GET /listaUsuarios continua publico mesmo com a cadeia de seguranca real ativa")
    void listaUsuariosContinuaPublico() throws Exception {
        when(usuarioService.listarUsuarios()).thenReturn(List.of());

        MvcResult resultado = chamar("Endpoint publico sem token", get("/listaUsuarios"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }
}
