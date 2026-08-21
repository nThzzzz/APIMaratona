package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.Time.TimeRequest;
import com.APImaratona.Maratona.Exceptions.EntidadeNaoEcontrada;
import com.APImaratona.Maratona.Exceptions.RegraDeNegocio;
import com.APImaratona.Maratona.Model.Time;
import com.APImaratona.Maratona.Model.Usuario;
import com.APImaratona.Maratona.Repository.Jpa.TimeRepository;
import com.APImaratona.Maratona.Repository.Jpa.UsuarioRepository;
import com.APImaratona.Maratona.Seguranca.SegHelperService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste unitario puro das regras de edicao de time. O foco e o que so o capitao pode fazer
 * e o que acontece quando o time nao tem capitao -- caso real de linhas gravadas antes da
 * coluna existir, que antes estourava NPE (500) em vez de recusar a operacao.
 */
class TimeServiceTest {

    private final TimeRepository timeRepo = mock(TimeRepository.class);
    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
    private final SegHelperService segHelperService = new SegHelperService(new BCryptPasswordEncoder());

    private final TimeService timeService = new TimeService(timeRepo, usuarioService, usuarioRepo, segHelperService);

    private Usuario usuario(String nomeUsuario) {
        Usuario u = new Usuario();
        u.setNomeUsuario(nomeUsuario);
        u.setNome(nomeUsuario);
        return u;
    }

    /** Time "Timaco" com fulano (capitao) e sicrano, registrado no repositorio mockado. */
    private Time timeComCapitao() {
        Time time = new Time();
        time.setNome("Timaco");

        Usuario capitao = usuario("fulano");
        time.getUsuarios().add(capitao);
        time.getUsuarios().add(usuario("sicrano"));
        time.setCapitao(capitao);

        when(timeRepo.existsByNome("Timaco")).thenReturn(true);
        when(timeRepo.findByNome("Timaco")).thenReturn(time);
        return time;
    }

    // --------------------------- Renomear ---------------------------

    @Test
    @DisplayName("editarNomeTime pelo capitao renomeia o time")
    void renomearPeloCapitao() {
        Time time = timeComCapitao();

        timeService.editarNomeTime("Timaco", new TimeRequest.AlterarNomeTime("Timacao"), "fulano");

        assertThat(time.getNome()).isEqualTo("Timacao");
        verify(timeRepo).save(time);
    }

    @Test
    @DisplayName("editarNomeTime por quem nao e o capitao e recusado")
    void renomearSemSerCapitao() {
        Time time = timeComCapitao();

        assertThatThrownBy(() -> timeService.editarNomeTime(
                "Timaco", new TimeRequest.AlterarNomeTime("Timacao"), "sicrano"))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessageContaining("não é o capitão");

        assertThat(time.getNome()).isEqualTo("Timaco");
        verify(timeRepo, never()).save(any(Time.class));
    }

    @Test
    @DisplayName("editarNomeTime recusa nome ja usado por outro time")
    void renomearParaNomeExistente() {
        timeComCapitao();
        when(timeRepo.existsByNome("Timacao")).thenReturn(true);

        assertThatThrownBy(() -> timeService.editarNomeTime(
                "Timaco", new TimeRequest.AlterarNomeTime("Timacao"), "fulano"))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessage("Nome de time ja utilizado");
    }

    @Test
    @DisplayName("editarNomeTime recusa o mesmo nome que o time ja tem")
    void renomearParaOMesmoNome() {
        timeComCapitao();

        assertThatThrownBy(() -> timeService.editarNomeTime(
                "Timaco", new TimeRequest.AlterarNomeTime("Timaco"), "fulano"))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessage("O novo nome do time deve ser diferente do atual");
    }

    @Test
    @DisplayName("editarNomeTime em time inexistente devolve nao encontrado")
    void renomearTimeInexistente() {
        when(timeRepo.existsByNome("Fantasma")).thenReturn(false);

        assertThatThrownBy(() -> timeService.editarNomeTime(
                "Fantasma", new TimeRequest.AlterarNomeTime("Timacao"), "fulano"))
                .isInstanceOf(EntidadeNaoEcontrada.class);
    }

    @Test
    @DisplayName("time sem capitao recusa a operacao em vez de estourar NPE")
    void timeSemCapitao() {
        Time time = new Time();
        time.setNome("Orfao");
        time.getUsuarios().add(usuario("fulano"));
        // capitao continua null: linha gravada antes da coluna existir

        when(timeRepo.existsByNome("Orfao")).thenReturn(true);
        when(timeRepo.findByNome("Orfao")).thenReturn(time);

        assertThatThrownBy(() -> timeService.editarNomeTime(
                "Orfao", new TimeRequest.AlterarNomeTime("Timacao"), "fulano"))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessageContaining("não é o capitão");
    }

    // ------------------------ Transferir capitania ------------------------

    @Test
    @DisplayName("transferirCapitania passa a capitania para outro integrante do time")
    void transferirParaIntegrante() {
        Time time = timeComCapitao();

        timeService.transferirCapitania("Timaco", new TimeRequest.TransferirCapitania("sicrano"), "fulano");

        assertThat(time.getCapitao().getNomeUsuario()).isEqualTo("sicrano");
        verify(timeRepo).save(time);
    }

    @Test
    @DisplayName("transferirCapitania recusa quem nao e integrante do time")
    void transferirParaForaDoTime() {
        Time time = timeComCapitao();

        assertThatThrownBy(() -> timeService.transferirCapitania(
                "Timaco", new TimeRequest.TransferirCapitania("estranho"), "fulano"))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessageContaining("nao e integrante do Time");

        assertThat(time.getCapitao().getNomeUsuario()).isEqualTo("fulano");
        verify(timeRepo, never()).save(any(Time.class));
    }

    @Test
    @DisplayName("transferirCapitania recusa transferir para o proprio capitao")
    void transferirParaSiMesmo() {
        timeComCapitao();

        assertThatThrownBy(() -> timeService.transferirCapitania(
                "Timaco", new TimeRequest.TransferirCapitania("fulano"), "fulano"))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessage("O novo capitão deve ser diferente do atual");
    }

    @Test
    @DisplayName("transferirCapitania por quem nao e o capitao e recusada")
    void transferirSemSerCapitao() {
        Time time = timeComCapitao();

        assertThatThrownBy(() -> timeService.transferirCapitania(
                "Timaco", new TimeRequest.TransferirCapitania("sicrano"), "sicrano"))
                .isInstanceOf(RegraDeNegocio.class)
                .hasMessageContaining("não é o capitão");

        assertThat(time.getCapitao().getNomeUsuario()).isEqualTo("fulano");
    }
}
