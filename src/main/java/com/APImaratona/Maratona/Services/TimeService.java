package com.APImaratona.Maratona.Services;

import com.APImaratona.Maratona.DTO.Time.TimeRequisicaoDTO;
import com.APImaratona.Maratona.DTO.Time.TimeResponseDTO;
import com.APImaratona.Maratona.DTO.Usuario.UsuarioResponseDTO;
import com.APImaratona.Maratona.Exceptions.EntidadeNaoEcontrada;
import com.APImaratona.Maratona.Exceptions.RegraDeNegocio;
import com.APImaratona.Maratona.Model.Time;
import com.APImaratona.Maratona.Model.Usuario;
import com.APImaratona.Maratona.Repository.Jpa.TimeRepository;
import com.APImaratona.Maratona.Repository.Jpa.UsuarioRepository;
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

        List<String> nomesUsuarios = dto.getNomesUsuarios() != null ? dto.getNomesUsuarios() : new ArrayList<>();

        if(nomesUsuarios.size()>3){
            throw new RuntimeException("Lista de membros do time maior que o permitido, tamanho= " + nomesUsuarios.size());
        }

        if(!nomesUsuarios.isEmpty()) {
            for(String nome : nomesUsuarios){
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
                timeDTO.getUsuarios().add(UsuarioResponseDTO.fromEntity(u));
            }
            timesDTO.add(timeDTO);
        }

        return timesDTO;
    }

    public TimeResponseDTO buscarTime(String nome){
        if(!timeRepo.existsByNome(nome)){
            throw new EntidadeNaoEcontrada("Time: "+ nome +", nao encontrado");
        }

        Time time = timeRepo.findByNome(nome);

        TimeResponseDTO timeDTO = new TimeResponseDTO();
        timeDTO.setNomeTime(time.getNome());
        timeDTO.setUsuarios(new ArrayList<>());

        for(Usuario u : time.getUsuarios()){
            timeDTO.getUsuarios().add(UsuarioResponseDTO.fromEntity(u));
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
            throw new RegraDeNegocio("Parametro nome do time NULL");
        }

        if(!timeRepo.existsByNome(dto.getNomeTime())){
            throw new RegraDeNegocio("Time inexistente");
        }

        Time time = timeRepo.findByNome(dto.getNomeTime());

        if(dto.getNomesUsuarios() == null || dto.getNomesUsuarios().isEmpty()){
            throw new RegraDeNegocio("Nenhum usuario para adicionar");
        }

        if((dto.getNomesUsuarios().size() + time.getUsuarios().size()) > 3){
            throw new RegraDeNegocio("Time: " + time.getNome() + ", tera mais de 3 integrantes");
        }

        for(String nome : dto.getNomesUsuarios()){
            if(!usuarioRepo.existsByNomeUsuario(nome)){
                throw new EntidadeNaoEcontrada("Nome de usuario: " + nome + ", nao econtrado");
            }

            Usuario usuario = usuarioRepo.findByNomeUsuario(nome);

            if(usuario.getTime() != null){
                throw new RegraDeNegocio("Usuario: " + usuario.getNome() + ", ja esta no Time: " +
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
            throw new RegraDeNegocio("Parametro nome do time NULL");
        }

        if(!timeRepo.existsByNome(dto.getNomeTime())){
            throw new EntidadeNaoEcontrada("Time inexistente");
        }

        Time time = timeRepo.findByNome(dto.getNomeTime());

        if(dto.getNomesUsuarios() == null || dto.getNomesUsuarios().isEmpty()){
            throw new RegraDeNegocio("Nenhum usuario para remover");
        }

        for(String nome : dto.getNomesUsuarios()){
            if(!usuarioRepo.existsByNomeUsuario(nome)){
                throw new EntidadeNaoEcontrada("Nome de usuario: " + nome + ", nao econtrado");
            }

            Usuario usuario = usuarioRepo.findByNomeUsuario(nome);

            if(usuario.getTime() == null || !usuario.getTime().getNome().equals(time.getNome())){
                throw new RegraDeNegocio("Usuario: " + usuario.getNome() + ", nao esta no Time: " +
                        time.getNome());
            }

            time.getUsuarios().remove(usuario);
            timeRepo.save(time);
            usuario.setTime(null);
            usuarioRepo.save(usuario);
        }
    }

}
