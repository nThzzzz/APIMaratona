package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesUserInfoResponse;
import com.APImaratona.Maratona.DTO.Usuario.*;
import com.APImaratona.Maratona.Exceptions.AutenticacaoInvalidaException;
import com.APImaratona.Maratona.Exceptions.EntidadeNaoEcontrada;
import com.APImaratona.Maratona.Exceptions.RegraDeNegocio;
import com.APImaratona.Maratona.Model.Time;
import com.APImaratona.Maratona.Model.Usuario;
import com.APImaratona.Maratona.Repository.Jpa.TimeRepository;
import com.APImaratona.Maratona.Repository.Jpa.UsuarioRepository;
import com.APImaratona.Maratona.Repository.Neo4j.UsuarioNodeRepository;
import com.APImaratona.Maratona.Seguranca.JwtService;
import com.APImaratona.Maratona.Seguranca.SegHelperService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final TimeRepository timeRepo;
    private final UsuarioNodeRepository usuarioNodeRepository;
    private final CodeforcesService codeforcesService;
    private final SegHelperService segHelperService;
    private final JwtService jwtService;

    @Caching(evict = {
            @CacheEvict(value = "cacheUsuariosProblema", allEntries = true),
            @CacheEvict(value = "cacheProblemasUsuario", key = "#dto.nomeUsuario")
    })
    public void cadastrarUsuario(UsuarioRequest.CadastrarUsuario dto){
        Usuario usuario = new Usuario();

        usuario.setNome(dto.nome());
        usuario.setNomeUsuario(dto.nomeUsuario());
        usuario.setSenha(segHelperService.encodarSenha(dto.senha())); // nunca grava senha em texto puro
        usuario.setEmail(dto.email());

        if(usuarioRepo.existsByEmail(dto.email())){
            throw new RegraDeNegocio("Usuário já cadastrado");
        }

        if(usuarioRepo.existsByNomeUsuario(dto.nomeUsuario())){
            throw new RegraDeNegocio("Nome de Usuário já cadastrado");
        }

        Time time = new Time();
        if (dto.nomeTime() != null && !dto.nomeTime().isBlank()) {
            if(timeRepo.existsByNome(dto.nomeTime())){
                time =  timeRepo.findByNome(dto.nomeTime());

                if(time.getUsuarios().size()==3){
                    throw new RegraDeNegocio("O time " + time.getNome() + ", já possui 3 integrantes");
                }

                time.getUsuarios().add(usuario);
                usuario.setTime(time);
            }else{
                throw new EntidadeNaoEcontrada("Time não encontrado");
            }
        }

        CodeforcesUserInfoResponse cfUsuario = codeforcesService.infoPerfilUsuario(usuario.getNomeUsuario());

        usuario.setRating(cfUsuario.getRating());
        usuario.setRank(cfUsuario.getRank());

        usuarioRepo.save(usuario);
    }

    public List<UsuarioResponse> listarUsuarios(){
        List<UsuarioResponse> listaUsuarios = new ArrayList<>();

        List<Usuario> usuarios = usuarioRepo.findAll();

        for(Usuario u : usuarios){
            listaUsuarios.add(UsuarioResponse.fromEntity(u));
        }

        return listaUsuarios;
    }

    public UsuarioResponse buscarUsuarioNome(String nome){
        if(!usuarioRepo.existsByNomeUsuario(nome)){
            throw new EntidadeNaoEcontrada("Usuario nao encontrado");
        }

        Usuario usuario = usuarioRepo.findByNomeUsuario(nome);

        return UsuarioResponse.fromEntity(usuario);
    }

    // Para atualizar o cache caso tenha excluido um usuario
    @Caching(evict = {
            @CacheEvict(value = "cacheUsuariosProblema", allEntries = true),
            @CacheEvict(value = "cacheProblemasUsuario", allEntries = true)
    })
    public void excluirUsuarioEmail(String nomeUsuario, UsuarioRequest.ExcluirUsuarioEmail dto, String nomeUsuarioAutenticado){
        Usuario u = buscarContaDoDono(nomeUsuario, nomeUsuarioAutenticado);

        // A rota e /excluirUsuario/{nomeUsuario}/email: quem manda o email de outra conta
        // nao esta confirmando nada, entao o email funciona como segunda confirmacao --
        // mesmo papel da senha logo abaixo.
        if(!u.getEmail().equals(dto.email())){
            throw new RegraDeNegocio("O email informado não pertence ao usuário " + nomeUsuario);
        }

        if(!segHelperService.verificaSenha(u.getSenha(), dto.senhaAtual())){
            throw new RegraDeNegocio("Senha incorreta");
        }

        excluirConta(u);
    }

    // Para atualizar o cache caso tenha excluido um usuario
    @Caching(evict = {
            @CacheEvict(value = "cacheUsuariosProblema", allEntries = true),
            @CacheEvict(value = "cacheProblemasUsuario", allEntries = true)
    })
    public void excluirUsuarioNomeUsuario(String nomeUsuario, UsuarioRequest.ExcluirUsuarioNomeUsuario dto, String nomeUsuarioAutenticado){
        Usuario u = buscarContaDoDono(nomeUsuario, nomeUsuarioAutenticado);

        if(!segHelperService.verificaSenha(u.getSenha(), dto.senhaAtual())){
            throw new RegraDeNegocio("Senha incorreta");
        }

        excluirConta(u);
    }

    // Toda operacao de conta e sobre a PROPRIA conta. Centralizado porque a dupla
    // "existe?" + "o token e dele?" estava copiada em seis metodos: um service novo que
    // esquecesse a segunda metade deixaria qualquer autenticado mexer em conta alheia.
    private Usuario buscarContaDoDono(String nomeUsuario, String nomeUsuarioAutenticado){
        if(!usuarioRepo.existsByNomeUsuario(nomeUsuario)) {
            throw new EntidadeNaoEcontrada("Usuário não encontrado");
        }

        // O token JWT prova quem esta chamando; so deixa agir sobre a propria conta,
        // mesmo que a senha informada por algum motivo bata com a de outra conta.
        if(!segHelperService.saoMesmoUsuario(nomeUsuario, nomeUsuarioAutenticado)) {
            throw new AutenticacaoInvalidaException("Token não corresponde a este usuário");
        }

        return usuarioRepo.findByNomeUsuario(nomeUsuario);
    }

    // Parte final comum das duas rotas de exclusao: solta o usuario do time (se houver) e
    // remove das duas bases (Neo4j + Postgres). Ficar num lugar so evita que as duas rotas
    // divirjam de novo -- foi exatamente assim que a condicao de senha ficou invertida aqui.
    private void excluirConta(Usuario u){
        if(u.getTime() != null) {
            // Compara pelo nomeUsuario (unico) em vez de == : getCapitao() pode devolver
            // outra instancia da mesma linha e a checagem passaria batido. O null e possivel
            // em times gravados antes da coluna capitao existir (ddl-auto: update).
            Usuario capitao = u.getTime().getCapitao();
            if(capitao != null && segHelperService.saoMesmoUsuario(capitao.getNomeUsuario(), u.getNomeUsuario())){
                throw new RegraDeNegocio("O usuário é capitão de um time, Time= " + u.getTime().getNome());
            }
            u.getTime().getUsuarios().remove(u);
        }

        usuarioNodeRepository.deleteById(u.getNomeUsuario());
        usuarioRepo.delete(u);
    }

    @Caching(evict = {
            @CacheEvict(value = "cacheUsuariosProblema", allEntries = true),
            @CacheEvict(value = "cacheProblemasUsuario", key = "#nomeUsuario")
    })
    public LoginResponse editarNomeUsuario(String nomeUsuario, UsuarioRequest.AlterarNomeUsuario dto, String nomeUsuarioAutenticado) {
        Usuario usuario = buscarContaDoDono(nomeUsuario, nomeUsuarioAutenticado);

        if(!segHelperService.verificaSenha(usuario.getSenha(), dto.senhaAtual())) {
            throw new RegraDeNegocio("Senha incorreta");
        }

        String nomeNovo = dto.nomeUsuarioNovo();
        if(nomeUsuario.equals(nomeNovo)) {
            throw new RegraDeNegocio("O novo nome de usuário deve ser diferente do antigo");
        }

        if(usuarioRepo.existsByNomeUsuario(nomeNovo)) {
            throw new RegraDeNegocio("Nome de usuário já em uso por outra pessoa");
        }

        usuario.setNomeUsuario(nomeNovo);
        usuarioRepo.save(usuario);

        usuarioNodeRepository.atualizarNomeUsuarioNode(nomeUsuario, nomeNovo);

        CodeforcesUserInfoResponse cfUsuario = codeforcesService.infoPerfilUsuario(nomeNovo);
        usuario.setRank(cfUsuario.getRank());
        usuario.setRating(cfUsuario.getRating());
        usuarioRepo.save(usuario);

        String tokenNovo = jwtService.gerarToken(nomeNovo);
        return new LoginResponse(tokenNovo, "Bearer");
    }

    public void editarEmailUsuario(String nomeUsuario, UsuarioRequest.AlterarEmail dto, String nomeUsuarioAutenticado) {
        Usuario usuario = buscarContaDoDono(nomeUsuario, nomeUsuarioAutenticado);
        if(!segHelperService.verificaSenha(usuario.getSenha(), dto.senhaAtual())) {
            throw new RegraDeNegocio("Senha incorreta");
        }

        String novoEmail = dto.emailNovo();
        // Checa "igual ao atual" antes de "em uso": senao trocar o email por ele mesmo
        // cai no existsByEmail e devolve a mensagem errada.
        if(usuario.getEmail().equals(novoEmail)) {
            throw new RegraDeNegocio("O novo email deve ser diferente do antigo");
        }

        if(usuarioRepo.existsByEmail(novoEmail)) {
            throw new RegraDeNegocio("Email já em uso por outra pessoa");
        }

        usuario.setEmail(novoEmail);
        usuarioRepo.save(usuario);
    }

    public void editarSenhaUsuario(String nomeUsuario, UsuarioRequest.AlterarSenha dto, String nomeUsuarioAutenticado) {
        Usuario usuario = buscarContaDoDono(nomeUsuario, nomeUsuarioAutenticado);
        if(!segHelperService.verificaSenha(usuario.getSenha(), dto.senhaAtual())) {
            throw new RegraDeNegocio("Senha incorreta");
        }

        String novaSenha = dto.senhaNova();
        // getSenha() e um hash BCrypt: equals() contra a senha em texto puro nunca da true,
        // entao a checagem so funciona passando pelo verificaSenha.
        if(segHelperService.verificaSenha(usuario.getSenha(), novaSenha)) {
            throw new RegraDeNegocio("A nova senha deve ser diferente da antiga");
        }

        usuario.setSenha(segHelperService.encodarSenha(novaSenha));
        usuarioRepo.save(usuario);
    }


    public void editarPerfilNome(String nomeUsuario, UsuarioRequest.AlterarNome dto, String nomeUsuarioAutenticado){
        Usuario usuario = buscarContaDoDono(nomeUsuario, nomeUsuarioAutenticado);

        String novoNome = dto.nomeNovo();
        if(usuario.getNome().equals(novoNome)) {
            throw new RegraDeNegocio("O novo nome deve ser diferente do antigo");
        }

        usuario.setNome(novoNome);
        usuarioRepo.save(usuario);
    }
}
