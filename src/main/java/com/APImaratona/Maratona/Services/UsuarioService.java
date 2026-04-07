package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.CadastroRequisicaoDTO;
import com.APImaratona.Maratona.DTO.UsuarioResponseDTO;
import com.APImaratona.Maratona.Model.Time;
import com.APImaratona.Maratona.Model.Usuario;
import com.APImaratona.Maratona.Repository.TimeRepository;
import com.APImaratona.Maratona.Repository.UsuarioRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor // pra nao usar o @Autwired, lombok faz
public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final TimeRepository timeRepo;

    public void cadastrarUsuario(CadastroRequisicaoDTO dto){


        // validacao e cadastros do usuario

        Usuario usuario = new Usuario();

        // Fazer verificacao e tratamento
        usuario.setNome(dto.getNome());
        usuario.setNomeUsuario(dto.getNomeUsuario());
        usuario.setSenha(dto.getSenha());
        usuario.setEmail(dto.getEmail());

        if(usuarioRepo.existsByEmail(dto.getEmail())){
            throw new RuntimeException("Usuário já cadastrado");
        }

        if(usuarioRepo.existsByNome(dto.getNome())){
            throw new RuntimeException("Nome de Usuário já cadastrado");
        }

        // validadcao do time

        Time time = new Time();

        if(dto.getNomeTime()!=null){
            if(timeRepo.existsByNome(dto.getNomeTime())){
                time =  timeRepo.findByNome(dto.getNomeTime());

                if(time.getUsuarios().size()==3){
                    throw new RuntimeException("O time " + time.getNome() + ", já possui 3 integrantes");
                }

                time.getUsuarios().add(usuario);
                usuario.setTime(time);
            }else{
                throw new RuntimeException("Time não encontrado");
            }
        }

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

            listaUsuarios.add(usuarioDTO);
        }

        return listaUsuarios;
    }

    public UsuarioResponseDTO buscarUsuarioNome(String nome){
        UsuarioResponseDTO usuarioDTO = new UsuarioResponseDTO();

        if(!usuarioRepo.existsByNomeUsuario(nome)){
            throw new RuntimeException("Usuario nao encontrado");
        }

        Usuario usuario = usuarioRepo.findByNomeUsuario(nome);

        usuarioDTO.setNome(usuario.getNome());
        usuarioDTO.setEmail(usuario.getEmail());
        usuarioDTO.setNomeUsuario(usuario.getNomeUsuario());
        usuarioDTO.setNomeTime(usuario.getTime() != null ? usuario.getTime().getNome() : "Sem time");

        return usuarioDTO;
    }

    public UsuarioResponseDTO buscarUsuarioEmail(String email){
        UsuarioResponseDTO usuarioDTO = new UsuarioResponseDTO();

        if(!usuarioRepo.existsByEmail(email)){
            throw new RuntimeException("Usuario nao encontrado");
        }

        Usuario usuario = usuarioRepo.findByEmail(email);

        usuarioDTO.setNome(usuario.getNome());
        usuarioDTO.setEmail(usuario.getEmail());
        usuarioDTO.setNomeUsuario(usuario.getNomeUsuario());
        usuarioDTO.setNomeTime(usuario.getTime() != null ? usuario.getTime().getNome() : "Sem time");

        return usuarioDTO;
    }
}
