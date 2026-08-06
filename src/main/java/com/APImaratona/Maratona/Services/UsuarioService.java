package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesUsuarioDTO;
import com.APImaratona.Maratona.DTO.Usuario.*;
import com.APImaratona.Maratona.Exceptions.AutenticacaoInvalidaException;
import com.APImaratona.Maratona.Exceptions.EntidadeNaoEcontrada;
import com.APImaratona.Maratona.Exceptions.RegraDeNegocio;
import com.APImaratona.Maratona.Model.Time;
import com.APImaratona.Maratona.Model.Usuario;
import com.APImaratona.Maratona.Repository.Jpa.TimeRepository;
import com.APImaratona.Maratona.Repository.Jpa.UsuarioRepository;
import com.APImaratona.Maratona.Repository.Neo4j.UsuarioNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor // pra nao usar o @Autwired, lombok faz
public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final TimeRepository timeRepo;
    private final UsuarioNodeRepository usuarioNodeRepository;
    private final CodeforcesService codeforcesService;
    private final PasswordEncoder passwordEncoder;

    @Caching(evict = {
            @CacheEvict(value = "cacheUsuariosProblema", allEntries = true),
            @CacheEvict(value = "cacheProblemasUsuario", key = "#dto.nomeUsuario")
    })
    public void cadastrarUsuario(UsuarioRequisicaoDTO dto){
        // validacao e cadastros do usuario

        Usuario usuario = new Usuario();

        // Fazer verificacao e tratamento
        usuario.setNome(dto.getNome());
        usuario.setNomeUsuario(dto.getNomeUsuario());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha())); // nunca grava senha em texto puro
        usuario.setEmail(dto.getEmail());

        if(usuarioRepo.existsByEmail(dto.getEmail())){
            throw new RegraDeNegocio("Usuário já cadastrado");
        }

        if(usuarioRepo.existsByNomeUsuario(dto.getNomeUsuario())){
            throw new RegraDeNegocio("Nome de Usuário já cadastrado");
        }

        // validadcao do time

        Time time = new Time();

        if(dto.getNomeTime()!=null){
            if(timeRepo.existsByNome(dto.getNomeTime())){
                time =  timeRepo.findByNome(dto.getNomeTime());

                if(time.getUsuarios().size()==3){
                    throw new RegraDeNegocio("O time " + time.getNome() + ", já possui 3 integrantes");
                }

                time.getUsuarios().add(usuario);
                usuario.setTime(time);
            }else{
                throw new EntidadeNaoEcontrada("Time não encontrado");
            }
        }

        CodeforcesUsuarioDTO cfUsuario = codeforcesService.infoPerfilUsuario(usuario.getNomeUsuario());

        usuario.setRating(cfUsuario.getRating());
        usuario.setRank(cfUsuario.getRank());

        usuarioRepo.save(usuario);
    }

    public List<UsuarioResponseDTO> listarUsuarios(){
        List<UsuarioResponseDTO> listaUsuarios = new ArrayList<>();

        List<Usuario> usuarios = usuarioRepo.findAll();

        for(Usuario u : usuarios){
            listaUsuarios.add(UsuarioResponseDTO.fromEntity(u));
        }

        return listaUsuarios;
    }

    public UsuarioResponseDTO buscarUsuarioNome(String nome){
        if(!usuarioRepo.existsByNomeUsuario(nome)){
            throw new EntidadeNaoEcontrada("Usuario nao encontrado");
        }

        Usuario usuario = usuarioRepo.findByNomeUsuario(nome);

        return UsuarioResponseDTO.fromEntity(usuario);
    }

    public UsuarioResponseDTO buscarUsuarioEmail(String email){
        if(!usuarioRepo.existsByEmail(email)){
            throw new EntidadeNaoEcontrada("Usuario nao encontrado");
        }

        Usuario usuario = usuarioRepo.findByEmail(email);

        return UsuarioResponseDTO.fromEntity(usuario);
    }

    // Para atualizar o cache caso tenha excluido um usuario
    @Caching(evict = {
            @CacheEvict(value = "cacheUsuariosProblema", allEntries = true),
            @CacheEvict(value = "cacheProblemasUsuario", allEntries = true)
    })
    public void excluirUsuario(ExcluirUsuarioRequisicaoDTO dto, String nomeUsuarioAutenticado){
        if(dto.getSenha() == null){
            throw new RegraDeNegocio("Senha = NULL");
        }

        Usuario u;

        //verifica se é por nome
        if(dto.getNomeUsuario() != null && !dto.getNomeUsuario().isBlank()){
            if(!usuarioRepo.existsByNomeUsuario(dto.getNomeUsuario())){
                throw new EntidadeNaoEcontrada("Nome de usuario nao encontrado");
            }

            u = usuarioRepo.findByNomeUsuario(dto.getNomeUsuario());
        }else if(dto.getEmail() != null && !dto.getEmail().isBlank()){ // verifica email
            if(!usuarioRepo.existsByEmail(dto.getEmail())){
                throw new RegraDeNegocio("Email de usuario nao encontrado");
            }

            u = usuarioRepo.findByEmail(dto.getEmail());
        }else{
            throw new RegraDeNegocio("Nenhum identificador de usuario (nomeUsuario ou email) fornecido");
        }

        // O token JWT prova quem esta chamando; so deixa excluir a propria conta,
        // mesmo que a senha informada por algum motivo bata com a de outra conta.
        if(!u.getNomeUsuario().equals(nomeUsuarioAutenticado)){
            throw new AutenticacaoInvalidaException("Token não corresponde a este usuário");
        }

        if(verificaSenha(u.getSenha(), dto.getSenha())){
            if(u.getTime() != null) {
                u.getTime().getUsuarios().remove(u);
            }

            usuarioNodeRepository.deleteById(u.getNomeUsuario());
            usuarioRepo.delete(u);
            return;
        }else{
            throw new RegraDeNegocio("Senha incorreta");
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "cacheUsuariosProblema", allEntries = true),
            @CacheEvict(value = "cacheProblemasUsuario", key = "#nomeUsuario")
    })
    public String editarUsuario(String nomeUsuario, EditarUsuarioCredenciaisRequisicaoDTO dto, String nomeUsuarioAutenticado){
        String resultado = "";

        if(!usuarioRepo.existsByNomeUsuario(nomeUsuario)){
            throw new EntidadeNaoEcontrada("Usuario: " + nomeUsuario + ", nao encontrado");
        }

        // O token JWT prova quem esta chamando; so deixa editar a propria conta.
        if(!nomeUsuario.equals(nomeUsuarioAutenticado)){
            throw new AutenticacaoInvalidaException("Token não corresponde a este usuário");
        }

        Usuario usuario = usuarioRepo.findByNomeUsuario(nomeUsuario);

        if(!verificaSenha(usuario.getSenha(), dto.getSenhaAntiga())){
            throw new RegraDeNegocio("Senha incorreta");
        }

        // Validação do Nome de Usuário
        if (dto.getNomeUsuario() != null && !dto.getNomeUsuario().isBlank() && !usuario.getNomeUsuario().equals(dto.getNomeUsuario())) {
            if(usuarioRepo.existsByNomeUsuario(dto.getNomeUsuario())){
                throw new RegraDeNegocio("Nome de usuário ja em uso");
            }
            String nomeUsuarioAntigo = usuario.getNomeUsuario();
            usuario.setNomeUsuario(dto.getNomeUsuario());
            usuarioNodeRepository.deleteById(nomeUsuarioAntigo);
            codeforcesService.sincronizarPerfilCodeforces(usuario.getNomeUsuario());

            CodeforcesUsuarioDTO cfUsuario = codeforcesService.infoPerfilUsuario(usuario.getNomeUsuario());
            usuario.setRank(cfUsuario.getRank());
            usuario.setRating(cfUsuario.getRating());

            resultado += "| Nome de Usuario |";
        }

        // Validação do Email
        if (dto.getEmail() != null && !dto.getEmail().isBlank() && !usuario.getEmail().equals(dto.getEmail())) {
            if(usuarioRepo.existsByEmail(dto.getEmail())){
                throw new RegraDeNegocio("Email ja em uso");
            }
            usuario.setEmail(dto.getEmail());
            resultado += "| email |";
        }

        // Validação da Senha Nova
        if (dto.getSenhaNova() != null && !dto.getSenhaNova().isBlank() && !passwordEncoder.matches(dto.getSenhaNova(), usuario.getSenha())) {
            usuario.setSenha(passwordEncoder.encode(dto.getSenhaNova()));
            resultado += "| Senha |";
        }

        usuarioRepo.save(usuario);

        if (resultado.isEmpty()) {
            return "Nenhuma alteração realizada";
        }

        return resultado;
    }

    public String editarPerfil(String nomeUsuario, EditarUsuarioPerfilRequisicaoDTO dto, String nomeUsuarioAutenticado){
        String resultado = "";

        if(!usuarioRepo.existsByNomeUsuario(nomeUsuario)){
            throw new EntidadeNaoEcontrada("Usuario: " + nomeUsuario + ", nao encontrado");
        }

        // O token JWT prova quem esta chamando; so deixa editar a propria conta.
        if(!nomeUsuario.equals(nomeUsuarioAutenticado)){
            throw new AutenticacaoInvalidaException("Token não corresponde a este usuário");
        }

        Usuario usuario = usuarioRepo.findByNomeUsuario(nomeUsuario);

        // Validação do Nome
        if (dto.getNomeNovo() != null && !dto.getNomeNovo().isBlank() && !usuario.getNome().equals(dto.getNomeNovo())) {
            usuario.setNome(dto.getNomeNovo());
            resultado += "| Nome |";
        }

        // Validação do resto

        usuarioRepo.save(usuario);

        if (resultado.isEmpty()) {
            return "Nenhuma alteração realizada";
        }

        return resultado;
    }

    private boolean verificaSenha(String senhaCerto, String senha){
        try {
            return passwordEncoder.matches(senha, senhaCerto);
        } catch (IllegalArgumentException e) {
            return false; // hash salvo nao e um BCrypt valido (conta antiga em texto puro)
        }
    }
}
