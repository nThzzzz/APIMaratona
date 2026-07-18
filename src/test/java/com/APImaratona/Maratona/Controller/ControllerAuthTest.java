package com.APImaratona.Maratona.Controller;

import com.APImaratona.Maratona.DTO.Usuario.LoginRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Usuario.LoginResponseDTO;
import com.APImaratona.Maratona.Exceptions.AutenticacaoInvalidaException;
import com.APImaratona.Maratona.Seguranca.JwtService;
import org.springframework.security.web.context.SecurityContextRepository;
import com.APImaratona.Maratona.Services.AuthService;
import com.APImaratona.Maratona.support.ApiControllerTestSupport;
import com.APImaratona.Maratona.support.TestCacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(ControllerAuth.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestCacheConfig.class)
class ControllerAuthTest extends ApiControllerTestSupport {

    @MockitoBean
    private AuthService authService;

    // JwtAuthenticationFilter e um Filter (@Component) e por isso e escaneado pelo @WebMvcTest
    // mesmo com addFilters=false; sem esse mock o contexto no sobe por falta de JwtService.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private SecurityContextRepository securityContextRepository;

    @Override
    protected String nomeControlador() {
        return "ControllerAuth";
    }

    @Test
    @DisplayName("POST /auth/login com credenciais validas retorna 200 com token")
    void loginSucesso() throws Exception {
        LoginRequisicaoDTO dto = new LoginRequisicaoDTO("fulano", "senha123");
        when(authService.login(any())).thenReturn(new LoginResponseDTO("token-fake-123", "Bearer"));

        MvcResult resultado = chamar("Login com sucesso", post("/auth/login")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).contains("token-fake-123", "Bearer");
    }

    @Test
    @DisplayName("POST /auth/login com credenciais invalidas retorna 401")
    void loginCredenciaisInvalidas() throws Exception {
        LoginRequisicaoDTO dto = new LoginRequisicaoDTO("fulano", "senhaErrada");
        doThrow(new AutenticacaoInvalidaException("Usuário ou senha inválidos")).when(authService).login(any());

        MvcResult resultado = chamar("Credenciais invalidas", post("/auth/login")
                .contentType(APPLICATION_JSON)
                .content(json(dto)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(resultado.getResponse().getContentAsString()).contains("Usuário ou senha inválidos");
    }
}
