package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.Codeforces.CodeforcesUsuarioDTO;
import com.APImaratona.Maratona.DTO.Usuario.EditarUsuarioRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Usuario.ExcluirUsuarioRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioResponseDTO;
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
        usuario.setSenha(dto.getSenha());
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
            UsuarioResponseDTO usuarioDTO = new UsuarioResponseDTO();

            usuarioDTO.setNome(u.getNome());
            usuarioDTO.setNomeUsuario(u.getNomeUsuario());
            usuarioDTO.setEmail(u.getEmail());
            usuarioDTO.setNomeTime(u.getTime() != null ? u.getTime().getNome() : "Sem time");
            usuarioDTO.setRank(u.getRank());
            usuarioDTO.setRating(u.getRating());

            listaUsuarios.add(usuarioDTO);
        }

        return listaUsuarios;
    }

    public UsuarioResponseDTO buscarUsuarioNome(String nome){
        UsuarioResponseDTO usuarioDTO = new UsuarioResponseDTO();

        if(!usuarioRepo.existsByNomeUsuario(nome)){
            throw new EntidadeNaoEcontrada("Usuario nao encontrado");
        }

        Usuario usuario = usuarioRepo.findByNomeUsuario(nome);

        usuarioDTO.setNome(usuario.getNome());
        usuarioDTO.setEmail(usuario.getEmail());
        usuarioDTO.setNomeUsuario(usuario.getNomeUsuario());
        usuarioDTO.setNomeTime(usuario.getTime() != null ? usuario.getTime().getNome() : "Sem time");
        usuarioDTO.setRank(usuario.getRank());
        usuarioDTO.setRating(usuario.getRating());

        return usuarioDTO;
    }

    public UsuarioResponseDTO buscarUsuarioEmail(String email){
        UsuarioResponseDTO usuarioDTO = new UsuarioResponseDTO();

        if(!usuarioRepo.existsByEmail(email)){
            throw new EntidadeNaoEcontrada("Usuario nao encontrado");
        }

        Usuario usuario = usuarioRepo.findByEmail(email);

        usuarioDTO.setNome(usuario.getNome());
        usuarioDTO.setEmail(usuario.getEmail());
        usuarioDTO.setNomeUsuario(usuario.getNomeUsuario());
        usuarioDTO.setNomeTime(usuario.getTime() != null ? usuario.getTime().getNome() : "Sem time");
        usuarioDTO.setRank(usuario.getRank());
        usuarioDTO.setRating(usuario.getRating());

        return usuarioDTO;
    }

    // Para atualizar o cache caso tenha excluido um usuario
    @Caching(evict = {
            @CacheEvict(value = "cacheUsuariosProblema", allEntries = true), // Nome exato!
            @CacheEvict(value = "cacheProblemasUsuario", allEntries = true)
    })
    public void excluirUsuario(ExcluirUsuarioRequisicaoDTO dto){
        if(dto.getSenha() == null){
            throw new RegraDeNegocio("Senha = NULL");
        }

        Usuario u = new Usuario();

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
            @CacheEvict(value = "cacheUsuariosProblema", allEntries = true), // Nome exato!
            @CacheEvict(value = "cacheProblemasUsuario", key = "#nomeUsuario")
    })
    public String editarUsuario(String nomeUsuario, EditarUsuarioRequisicaoDTO dto){
        String resultado = "";

        if(!usuarioRepo.existsByNomeUsuario(nomeUsuario)){
            throw new EntidadeNaoEcontrada("Usuario: " + nomeUsuario + ", nao encontrado");
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
            usuario.setNomeUsuario(dto.getNomeUsuario());
            usuarioNodeRepository.deleteById(dto.getNomeUsuario());
            codeforcesService.sincronizarPerfilCodeforces(usuario.getNomeUsuario());

            CodeforcesUsuarioDTO cfUsuario = codeforcesService.infoPerfilUsuario(usuario.getNomeUsuario());
            usuario.setRank(cfUsuario.getRank());
            usuario.setRating(cfUsuario.getRating());

            resultado += "| Nome de Usuario |";
        }

        // Validação do Nome
        if (dto.getNome() != null && !dto.getNome().isBlank() && !usuario.getNome().equals(dto.getNome())) {
            usuario.setNome(dto.getNome());
            resultado += "| Nome |";
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
        if (dto.getSenhaNova() != null && !dto.getSenhaNova().isBlank() && !usuario.getSenha().equals(dto.getSenhaNova())) {
            usuario.setSenha(dto.getSenhaNova());
            resultado += "| Senha |";
        }

        // Validação do Time
        if (dto.getNomeTime() != null && !dto.getNomeTime().isBlank()) {

            if(!timeRepo.existsByNome(dto.getNomeTime())){
                throw new EntidadeNaoEcontrada("Time: " + dto.getNomeTime() + ", inexistente");
            }

            Time novoTime = timeRepo.findByNome(dto.getNomeTime());

            boolean jaEstaNoTime = (usuario.getTime() != null && usuario.getTime().getNome().equals(novoTime.getNome()));

            if(!jaEstaNoTime){
                if(novoTime.getUsuarios().size() >= 3){
                    throw new RegraDeNegocio("Time: " + novoTime.getNome() + ", ja possui 3 integrantes");
                }

                if (usuario.getTime() != null) {
                    usuario.getTime().getUsuarios().remove(usuario);
                }

                novoTime.getUsuarios().add(usuario);
                usuario.setTime(novoTime);

                timeRepo.save(novoTime);

                resultado += "| Time |";
            }
        }

        usuarioRepo.save(usuario);

        if (resultado.isEmpty()) {
            return "Nenhuma alteração realizada";
        }

        return resultado;
    }

    private boolean verificaSenha(String senhaCerto, String senha){
        return senhaCerto.equals(senha);
    }
}
