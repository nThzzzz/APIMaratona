package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.TimeRequisicaoDTO;
import com.APImaratona.Maratona.DTO.TimeResponseDTO;
import com.APImaratona.Maratona.DTO.UsuarioResponseDTO;
import com.APImaratona.Maratona.Model.Time;
import com.APImaratona.Maratona.Model.Usuario;
import com.APImaratona.Maratona.Repository.TimeRepository;
import com.APImaratona.Maratona.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeService {
    private final TimeRepository timeRepo;
    private final UsuarioRepository usuarioRepo;

    public void cadastrarTime(TimeRequisicaoDTO dto){
        // Fazer verificacao e tratamento

        Time time = new Time();

        if(dto.getNomeTime()==null){
            throw new RuntimeException("Nome do time NULL");
        }

        time.setNome(dto.getNomeTime());

        if(timeRepo.existsByNome(time.getNome())){
            throw new RuntimeException("Nome de time ja utilizado");
        }

        if(dto.getNomesUsuarios().size()>3){
            throw new RuntimeException("Lista de membros do time maior que o permitido, tamanho= " + dto.getNomesUsuarios().size());
        }

        if(!dto.getNomesUsuarios().isEmpty()) {
            for(String nome : dto.getNomesUsuarios()){
                if(!usuarioRepo.existsByNomeUsuario(nome)){
                    throw new RuntimeException("Usuario: " + nome + ", nao econtrado");
                }

                Usuario usuario = usuarioRepo.findByNomeUsuario(nome);

                if(usuario.getTime() != null){
                    throw new RuntimeException("Usuario: " + nome + ", ja esta cadastrado em um time");
                }

                time.getUsuarios().add(usuario);
                usuario.setTime(time);
            }
        }

        timeRepo.save(time);
    }

    public List<TimeResponseDTO> listarTimes(){
        List<Time> times = new ArrayList<>();
        List<TimeResponseDTO> timesDTO = new ArrayList<>();

        times = timeRepo.findAll();

        for(Time t : times){
            TimeResponseDTO timeDTO = new TimeResponseDTO();
            timeDTO.setNomeTime(t.getNome());
            timeDTO.setUsuarios(new ArrayList<>());

            for(Usuario u : t.getUsuarios()){
                UsuarioResponseDTO usarioDTO = new UsuarioResponseDTO();

                usarioDTO.setNome(u.getNome());
                usarioDTO.setEmail(u.getEmail());
                usarioDTO.setNomeUsuario(u.getNomeUsuario());
                usarioDTO.setNomeTime(u.getTime().getNome());

                timeDTO.getUsuarios().add(usarioDTO);
            }
            timesDTO.add(timeDTO);
        }

        return timesDTO;
    }

    public TimeResponseDTO buscarTime(String nome){
        if(!timeRepo.existsByNome(nome)){
            throw new RuntimeException("Time nao encontrado");
        }

        Time time = timeRepo.findByNome(nome);

        TimeResponseDTO timeDTO = new TimeResponseDTO();
        timeDTO.setNomeTime(time.getNome());
        timeDTO.setUsuarios(new ArrayList<>());

        for(Usuario u : time.getUsuarios()){
            UsuarioResponseDTO usuarioResponseDTO = new UsuarioResponseDTO();

            usuarioResponseDTO.setNome(u.getNome());
            usuarioResponseDTO.setEmail(u.getEmail());
            usuarioResponseDTO.setNomeUsuario(u.getNomeUsuario());
            usuarioResponseDTO.setNomeTime(u.getTime() != null ? u.getTime().getNome() : "Sem time");

            timeDTO.getUsuarios().add(usuarioResponseDTO);
        }

        return timeDTO;
    }

    public void excluirTime(String nome){
        if(!timeRepo.existsByNome(nome)){
            throw new RuntimeException("Time nao econtrado");
        }

        Time time = timeRepo.findByNome(nome);

        for(Usuario u : time.getUsuarios()){
            u.setTime(null);
            usuarioRepo.save(u);
        }

        timeRepo.delete(time);
    }

    public void adicionarUsuarioNoTime(TimeRequisicaoDTO dto){
        if(dto.getNomeTime() == null){
            throw new RuntimeException("Parametro nome do time NULL");
        }

        if(!timeRepo.existsByNome(dto.getNomeTime())){
            throw new RuntimeException("Time inexistente");
        }

        Time time = timeRepo.findByNome(dto.getNomeTime());

        if(dto.getNomesUsuarios().isEmpty()){
            throw new RuntimeException("Nenhum usuario para adicionar");
        }

        if((dto.getNomesUsuarios().size() + time.getUsuarios().size()) > 3){
            throw new RuntimeException("Time: " + time.getNome() + ", tera mais de 3 integrantes");
        }

        for(String nome : dto.getNomesUsuarios()){
            if(!usuarioRepo.existsByNomeUsuario(nome)){
                throw new RuntimeException("Nome de usuario: " + nome + ", nao econtrado");
            }

            Usuario usuario = usuarioRepo.findByNomeUsuario(nome);

            if(usuario.getTime() != null){
                throw new RuntimeException("Usuario: " + usuario.getNome() + ", ja esta no Time: " +
                        usuario.getTime().getNome());
            }

            usuario.setTime(time);
            time.getUsuarios().add(usuario);

            usuarioRepo.save(usuario);
            timeRepo.save(time);
        }
    }

    public void removerUsuarioNoTime(TimeRequisicaoDTO dto){
        if(dto.getNomeTime() == null){
            throw new RuntimeException("Parametro nome do time NULL");
        }

        if(!timeRepo.existsByNome(dto.getNomeTime())){
            throw new RuntimeException("Time inexistente");
        }

        Time time = timeRepo.findByNome(dto.getNomeTime());

        if(dto.getNomesUsuarios().isEmpty()){
            throw new RuntimeException("Nenhum usuario para remover");
        }

        for(String nome : dto.getNomesUsuarios()){
            if(!usuarioRepo.existsByNomeUsuario(nome)){
                throw new RuntimeException("Nome de usuario: " + nome + ", nao econtrado");
            }

            Usuario usuario = usuarioRepo.findByNomeUsuario(nome);

            if(!usuario.getTime().getNome().equals(time.getNome())){
                throw new RuntimeException("Usuario: " + usuario.getNome() + ", nao esta no Time: " +
                        time.getNome());
            }

            time.getUsuarios().remove(usuario);
            timeRepo.save(time);
            usuario.setTime(null);
            usuarioRepo.save(usuario);
        }
    }

}
