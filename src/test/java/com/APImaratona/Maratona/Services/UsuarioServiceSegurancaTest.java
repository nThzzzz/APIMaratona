package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.Usuario.UsuarioRequest;
import com.APImaratona.Maratona.Exceptions.RegraDeNegocio;
import com.APImaratona.Maratona.Model.Usuario;
import com.APImaratona.Maratona.Repository.Jpa.TimeRepository;
import com.APImaratona.Maratona.Repository.Jpa.UsuarioRepository;
import com.APImaratona.Maratona.Repository.Neo4j.UsuarioNodeRepository;
import com.APImaratona.Maratona.Seguranca.JwtService;
import com.APImaratona.Maratona.Seguranca.SegHelperService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste unitario puro (sem contexto Spring) das checagens de senha do UsuarioService.
 * Usa o SegHelperService/BCrypt de verdade -- o ponto aqui e justamente comparar hash com
 * texto puro corretamente, entao mockar o encoder esconderia os bugs que estes testes travam:
 *
 *  - excluirUsuarioNomeUsuario tinha a condicao invertida e apagava a conta quando a senha
 *    estava ERRADA;
 *  - editarSenhaUsuario comparava o hash BCrypt com equals() contra a senha nova em texto
 *    puro, o que nunca da true e deixava a regra "senha nova != senha antiga" morta.
 */
class UsuarioServiceSegurancaTest {

    private static final String SENHA_ATUAL = "senha123";

    private final UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
    private final TimeRepository timeRepo = mock(TimeRepository.class);
    private final UsuarioNodeRepository usuarioNodeRepository = mock(UsuarioNodeRepository.class);
    private final CodeforcesService codeforcesService = mock(CodeforcesService.class);
    private final SegHelperService segHelperService = new SegHelperService(new BCryptPasswordEncoder());
    private final JwtService jwtService = mock(JwtService.class);

    private final UsuarioService usuarioService = new UsuarioService(
            usuarioRepo, timeRepo, usuarioNodeRepository, codeforcesService, segHelperService, jwtService);

    private Usuario usuarioSalvo() {
        Usuario u = new Usuario();
        u.setNome("Fulano");
        u.setNomeUsuario("fulano");
        u.setEmail("fulano@teste.com");
        u.setSenha(segHelperService.encodarSenha(SENHA_ATUAL));

        when(usuarioRepo.existsByNomeUsuario("fulano")).thenReturn(true);
        when(usuarioRepo.findByNomeUsuario("fulano")).thenReturn(u);
        when(usuarioRepo.existsByEmail("fulano@teste.com")).thenReturn(true);
        when(usuarioRepo.findByEmail("fulano@teste.com")).thenReturn(u);
        return u;
    }

    @Test
    @DisplayName("excluirUsuarioNomeUsuario com a senha CORRETA exclui a conta")
    void excluirPorNomeUsuarioComSenhaCorreta() {
        Usuario u = usuarioSalvo();

        usuarioService.excluirUsuarioNomeUsuario(
                "fulano", new UsuarioRequest.ExcluirUsuarioNomeUsuario(SENHA_ATUAL), "fulano");

        verify(usuarioRepo).delete(u);
        verify(usuarioNodeRepository).deleteById("fulano");
    }

    @Test
    @DisplayName("excluirUsuarioNomeUsuario com a senha ERRADA nao exclui nada e devolve erro")
    void excluirPorNomeUsuarioComSenhaErrada() {
        Usuario u = usuarioSalvo();

        assertThatThrownBy(() -> usuarioService.excluirUsuarioNomeUsuario(
                "fulano", new UsuarioRequest.ExcluirUsuarioNomeUsuario("senha-errada"), "fulano"))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessage("Senha incorreta");

        verify(usuarioRepo, never()).delete(u);
        verify(usuarioNodeRepository, never()).deleteById("fulano");
    }

    @Test
    @DisplayName("excluirUsuarioEmail com a senha CORRETA exclui a conta")
    void excluirPorEmailComSenhaCorreta() {
        Usuario u = usuarioSalvo();

        usuarioService.excluirUsuarioEmail(
                "fulano", new UsuarioRequest.ExcluirUsuarioEmail("fulano@teste.com", SENHA_ATUAL), "fulano");

        verify(usuarioRepo).delete(u);
    }

    @Test
    @DisplayName("excluirUsuarioEmail com a senha ERRADA nao exclui nada")
    void excluirPorEmailComSenhaErrada() {
        Usuario u = usuarioSalvo();

        assertThatThrownBy(() -> usuarioService.excluirUsuarioEmail(
                "fulano", new UsuarioRequest.ExcluirUsuarioEmail("fulano@teste.com", "senha-errada"), "fulano"))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessage("Senha incorreta");

        verify(usuarioRepo, never()).delete(u);
    }

    @Test
    @DisplayName("excluirUsuarioEmail recusa email que pertence a outra conta que nao a do path")
    void excluirPorEmailDeOutraConta() {
        usuarioSalvo();

        assertThatThrownBy(() -> usuarioService.excluirUsuarioEmail(
                "sicrano", new UsuarioRequest.ExcluirUsuarioEmail("fulano@teste.com", SENHA_ATUAL), "sicrano"))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessageContaining("não pertence ao usuário sicrano");

        verify(usuarioRepo, never()).delete(any(Usuario.class));
    }

    @Test
    @DisplayName("editarSenhaUsuario recusa uma senha nova igual a atual (mesmo com hash BCrypt)")
    void trocarSenhaPelaMesmaSenha() {
        usuarioSalvo();

        assertThatThrownBy(() -> usuarioService.editarSenhaUsuario(
                "fulano", new UsuarioRequest.AlterarSenha(SENHA_ATUAL, SENHA_ATUAL), "fulano"))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessage("A nova senha deve ser diferente da antiga");

        verify(usuarioRepo, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("editarSenhaUsuario grava a senha nova como hash BCrypt, nunca em texto puro")
    void trocarSenhaGravaHash() {
        usuarioSalvo();

        usuarioService.editarSenhaUsuario(
                "fulano", new UsuarioRequest.AlterarSenha(SENHA_ATUAL, "senhaNova456"), "fulano");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(captor.capture());

        String senhaGravada = captor.getValue().getSenha();
        assertThat(senhaGravada).isNotEqualTo("senhaNova456");
        assertThat(segHelperService.verificaSenha(senhaGravada, "senhaNova456")).isTrue();
    }
}
